// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_STRING_ARRAY;
import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.Syntax.identifiersToNames;
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
            SyntaxSymbol identifier = (SyntaxSymbol) formalsDecl;
            String name = identifier.stringValue();
            return new CompiledLambdaRest(name, body);
        }
        else
        {
            String[] argNames = identifiersToNames(formals);
            switch (argNames.length)
            {
                case 0:
                    return new CompiledLambda0(body);
                case 1:
                    return new CompiledLambda1(argNames, body);
                case 2:
                    return new CompiledLambda2(argNames, body);
                default:
                    return new CompiledLambdaN(argNames, body);
            }
        }
    }


    //========================================================================


    abstract static class CompiledLambdaBase
        implements CompiledForm
    {
        final String[]     myArgNames;
        final CompiledForm myBody;

        CompiledLambdaBase(String[] argNames, CompiledForm body)
        {
            myArgNames = argNames;
            myBody     = body;
        }
    }

    /** Marker for lambdas that accept a fixed number of arguments. */
    interface CompiledLambdaExact
    {
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
        Closure(Store enclosure, String[] argNames, CompiledForm body)
        {
            super(argNames);

            myEnclosure = enclosure;
            myBody      = body;
        }
    }


    //========================================================================


    private static final class CompiledLambda0
        extends CompiledLambdaBase
        implements CompiledLambdaExact
    {
        CompiledLambda0(CompiledForm body)
        {
            super(EMPTY_STRING_ARRAY, body);
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            return new Closure0(store, myBody);
        }
    }


    private static final class Closure0
        extends Closure
    {
        Closure0(Store enclosure, CompiledForm body)
        {
            super(enclosure, EMPTY_STRING_ARRAY, body);
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
        extends CompiledLambdaBase
        implements CompiledLambdaExact
    {
        CompiledLambda1(String[] argNames, CompiledForm body)
        {
            super(argNames, body);
            assert argNames.length == 1;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            return new Closure1(store, myArgNames, myBody);
        }
    }


    private static final class Closure1
        extends Closure
    {
        Closure1(Store enclosure, String[] argNames, CompiledForm body)
        {
            super(enclosure, argNames, body);
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
        extends CompiledLambdaBase
        implements CompiledLambdaExact
    {
        CompiledLambda2(String[] argNames, CompiledForm body)
        {
            super(argNames, body);
            assert argNames.length == 2;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            return new Closure2(store, myArgNames, myBody);
        }
    }


    private static final class Closure2
        extends Closure
    {
        Closure2(Store enclosure, String[] argNames, CompiledForm body)
        {
            super(enclosure, argNames, body);
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
        extends CompiledLambdaBase
        implements CompiledLambdaExact
    {
        CompiledLambdaN(String[] argNames, CompiledForm body)
        {
            super(argNames, body);
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
        {
            return new ClosureN(store, myArgNames, myBody);
        }
    }


    private static class ClosureN
        extends Closure
    {
        private final int myArity;

        ClosureN(Store enclosure, String[] argNames, CompiledForm body)
        {
            super(enclosure, argNames, body);
            myArity = argNames.length;
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
        extends CompiledLambdaBase
    {
        CompiledLambdaRest(String restArgName, CompiledForm body)
        {
            super(new String[] { restArgName, Procedure.DOTDOTDOT }, body);
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            return new ClosureRest(store, myArgNames, myBody);
        }
    }


    private static final class ClosureRest
        extends Closure
    {
        ClosureRest(Store enclosure, String[] argNames, CompiledForm body)
        {
            super(enclosure, argNames, body);
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            Object rest = immutableSexp(eval, args);

            Store localStore = new LocalStore1(myEnclosure, rest);

            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
