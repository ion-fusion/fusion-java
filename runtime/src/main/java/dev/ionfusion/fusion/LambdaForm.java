// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.Syntax.isBindableIdentifier;
import static dev.ionfusion.fusion.SyntaxSymbol.ensureUniqueIdentifiers;

import dev.ionfusion.runtime.base.FusionException;

/**
 * The {@code lambda} syntactic form, which evaluates to a {@link Closure}.
 */
final class LambdaForm
    extends SyntacticForm
{
    @Override
    SyntaxValue<?> expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        int arity = check.arityAtLeast(3);

        var children = stx.extract(eval);

        int bodyStart;
        var maybeDoc = children[2];
        if (isString(eval, maybeDoc.unwrap(eval)) && arity > 3)
        {
            bodyStart = 3;
        }
        else
        {
            bodyStart = 2;
        }

        var formals = children[1];
        var args = flattenFormals(eval, check, formals);

        // When there's no args, we can avoid an empty binding rib at runtime.
        // TODO #67 This will need changing for internal definitions
        //      since we won't know yet whether the rib will be empty or not.
        LocalEnvironment localEnv = null;
        if (args.length != 0)
        {
            ensureUniqueIdentifiers(eval, args, stx);
            env = localEnv = new LocalEnvironment(env, args);
        }

        // Prepare the bound names so they resolve to their own binding.
        for (int i = 0; i < args.length; i++)
        {
            SyntaxSymbol arg = args[i];
            arg = (SyntaxSymbol) arg.addWrap(localEnv.getWrap());
            arg.resolve();           // Caches the binding in the identifier
            args[i] = arg;
        }

        if (formals instanceof SyntaxSymbol)
        {
            children[1] = args[0];
        }
        else
        {
            children[1] = ((SyntaxSexp) formals).copyReplacingChildren(eval, args);
        }

        // TODO #67 Should allow internal definitions
        for (int i = bodyStart; i < children.length; i++)
        {
            SyntaxValue<?> bodyForm = children[i];
            if (localEnv != null)
            {
                bodyForm = bodyForm.addWrap(localEnv.getWrap());
            }
            bodyForm = expander.expandExpression(env, bodyForm);
            children[i] = bodyForm;
        }

        return stx.copyReplacingChildren(eval, children);
    }


    private static SyntaxSymbol[] flattenFormals(Evaluator eval,
                                                 SyntaxChecker lambdaCheck,
                                                 SyntaxValue<?> formals)
        throws FusionException
    {
        if (isBindableIdentifier(eval, formals))
        {
            return new SyntaxSymbol[] { (SyntaxSymbol) formals };
        }

        if (!(formals instanceof SyntaxSexp))
        {
            throw lambdaCheck.failure("formals must be a sexp or non-empty symbol", formals);
        }

        SyntaxSexp argSexp = (SyntaxSexp) formals;
        int size = argSexp.size(eval);
        if (size == 0) return SyntaxSymbol.EMPTY_ARRAY;

        SyntaxSymbol[] args = new SyntaxSymbol[size];
        for (int i = 0; i < size; i++)
        {
            var arg = argSexp.get(eval, i);
            if (! isBindableIdentifier(eval, arg))
            {
                throw lambdaCheck.failure("formal argument name must be a non-empty symbol", arg);
            }
            args[i] = (SyntaxSymbol) arg;
        }
        return args;
    }

    //========================================================================

    /**
     * Collects the formal argument identifiers, regardless of rest-ness.
     */
    private static SyntaxSymbol[] flattenFormals(Evaluator eval,
                                                 SyntaxValue<?> formalsDecl)
        throws FusionException
    {
        // (lambda rest ___)
        if (formalsDecl instanceof SyntaxSymbol)
        {
            return new SyntaxSymbol[] { (SyntaxSymbol) formalsDecl };
        }

        // (lambda (formal ...) ___)
        var formalsSexp = (SyntaxSexp) formalsDecl;
        int size = formalsSexp.size(eval);
        return size == 0
               ? SyntaxSymbol.EMPTY_ARRAY
               : formalsSexp.extract(eval, SyntaxSymbol.class);
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        var formalsDecl = stx.get(eval, 1);
        var formals = flattenFormals(eval, formalsDecl);
        if (formals.length != 0)
        {
            // We micro-optimize by avoiding an environment frame for no-arg procedures.
            env = LocalEnvironment.forBinders(env, formals);
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        boolean isRest = (formalsDecl instanceof SyntaxSymbol);
        if (isRest)
        {
            return new CompiledLambdaRest(body);
        }
        else
        {
            int arity = formals.length;
            switch (arity)
            {
                case 0:
                    return new CompiledLambda0(body);
                case 1:
                    return new CompiledLambda1(body);
                case 2:
                    return new CompiledLambda2(body);
                default:
                    return new CompiledLambdaN(arity, body);
            }
        }
    }


    //========================================================================


    abstract static class CompiledLambda
        implements CompiledForm
    {
        final CompiledForm myBody;

        CompiledLambda(CompiledForm body)
        {
            myBody = body;
        }
    }


    /**
     * Implements lambdas that accept a fixed number of arguments.
     */
    abstract static class CompiledLambdaExact
        extends CompiledLambda
    {
        CompiledLambdaExact(CompiledForm body)
        {
            super(body);
        }

        abstract int getExactArity();
    }


    private abstract static class Closure
        extends Procedure
    {
        final Store        myEnclosure;
        final CompiledForm myBody;

        /**
         * Constructs a new closure from its source and enclosing lexical
         * environment.
         *
         * @param enclosure the store lexically surrounding the source of this
         *  closure.  Any free variables in the procedure are expected to be
         *  bound here.
         */
        Closure(Store enclosure, CompiledForm body)
        {
            myEnclosure = enclosure;
            myBody      = body;
        }
    }


    //========================================================================


    private static final class CompiledLambda0
        extends CompiledLambdaExact
    {
        CompiledLambda0(CompiledForm body)
        {
            super(body);
        }

        @Override
        public int getExactArity()
        {
            return 0;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new Closure0(store, myBody);
        }
    }


    private static final class Closure0
        extends Closure
    {
        Closure0(Store enclosure, CompiledForm body)
        {
            super(enclosure, body);
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(eval, 0, args);

            // No local store is created to wrap myEnclosure!
            return eval.bounceTailForm(myEnclosure, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLambda1
        extends CompiledLambdaExact
    {
        CompiledLambda1(CompiledForm body)
        {
            super(body);
        }

        @Override
        public int getExactArity()
        {
            return 1;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new Closure1(store, myBody);
        }
    }


    private static final class Closure1
        extends Closure
    {
        Closure1(Store enclosure, CompiledForm body)
        {
            super(enclosure, body);
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(eval, 1, args);

            Store localStore = new LocalStore1(myEnclosure, args[0]);

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLambda2
        extends CompiledLambdaExact
    {
        CompiledLambda2(CompiledForm body)
        {
            super(body);
        }

        @Override
        public int getExactArity()
        {
            return 2;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new Closure2(store, myBody);
        }
    }


    private static final class Closure2
        extends Closure
    {
        Closure2(Store enclosure, CompiledForm body)
        {
            super(enclosure, body);
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(eval, 2, args);

            Store localStore = new LocalStore2(myEnclosure, args[0], args[1]);

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLambdaN
        extends CompiledLambdaExact
    {
        private final int myArity;

        CompiledLambdaN(int arity, CompiledForm body)
        {
            super(body);
            myArity = arity;
        }

        @Override
        int getExactArity()
        {
            return myArity;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new ClosureN(store, myArity, myBody);
        }
    }


    private static class ClosureN
        extends Closure
    {
        private final int myArity;

        ClosureN(Store enclosure, int arity, CompiledForm body)
        {
            super(enclosure, body);
            myArity = arity;
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(eval, myArity, args);

            Store localStore = new LocalStore(myEnclosure, args);

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLambdaRest
        extends CompiledLambda
    {
        CompiledLambdaRest(CompiledForm body)
        {
            super(body);
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new ClosureRest(store, myBody);
        }
    }


    private static final class ClosureRest
        extends Closure
    {
        ClosureRest(Store enclosure, CompiledForm body)
        {
            super(enclosure, body);
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
        {
            Object rest = immutableSexp(eval, args);

            Store localStore = new LocalStore1(myEnclosure, rest);

            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
