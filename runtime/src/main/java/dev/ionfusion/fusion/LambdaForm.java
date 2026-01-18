// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_STRING_ARRAY;

/**
 * The {@code lambda} syntactic form, which evaluates to a {@link Closure}.
 */
final class LambdaForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        int arity = check.arityAtLeast(3);

        SyntaxValue[] children = stx.extract(eval);

        int bodyStart;
        SyntaxValue maybeDoc = children[2];
        if (isString(eval, maybeDoc.unwrap(eval)) && arity > 3)
        {
            bodyStart = 3;
        }
        else
        {
            bodyStart = 2;
        }

        boolean isRest = (children[1] instanceof SyntaxSymbol);

        SyntaxSymbol[] args;
        if (isRest)
        {
            // Check for null/empty symbol
            SyntaxSymbol rest = check.requiredIdentifier("rest parameter", 1);
            args = new SyntaxSymbol[]{ rest };
        }
        else
        {
            SyntaxChecker checkFormals =
                check.subformSexp("formal arguments", 1);
            args = determineArgs(checkFormals);
        }

        // When there's no args, we can avoid an empty binding rib at runtime.
        // TODO This will need changing for internal definitions
        //      since we won't know yet whether the rib will be empty or not.
        //      https://github.com/ion-fusion/fusion-java/issues/67
        SyntaxWrap localWrap = null;
        if (args.length != 0)
        {
            env = new LocalEnvironment(env, args, stx);
            localWrap = new EnvironmentWrap(env);
        }

        // Prepare the bound names so they resolve to their own binding.
        for (int i = 0; i < args.length; i++)
        {
            SyntaxSymbol arg = args[i];
            arg = arg.addWrap(localWrap);
            arg.resolve();           // Caches the binding in the identifier
            args[i] = arg;
        }

        if (isRest)
        {
            children[1] = args[0];
        }
        else
        {
            SyntaxSexp formals = (SyntaxSexp) children[1];
            children[1] = formals.copyReplacingChildren(eval, args);
        }

        // TODO Should allow internal definitions
        //  https://github.com/ion-fusion/fusion-java/issues/67
        for (int i = bodyStart; i < children.length; i++)
        {
            SyntaxValue bodyForm = children[i];
            if (localWrap != null)
            {
                bodyForm = bodyForm.addWrap(localWrap);
            }
            bodyForm = expander.expandExpression(env, bodyForm);
            children[i] = bodyForm;
        }

        return stx.copyReplacingChildren(eval, children);
    }


    private static SyntaxSymbol[] determineArgs(SyntaxChecker checkArgs)
        throws FusionException
    {
        SyntaxSexp argSexp = (SyntaxSexp) checkArgs.form();
        int size = argSexp.size();
        if (size == 0) return SyntaxSymbol.EMPTY_ARRAY;

        SyntaxSymbol[] args = new SyntaxSymbol[size];
        for (int i = 0; i < size; i++)
        {
            args[i] = checkArgs.requiredIdentifier("formal argument name", i);
        }
        return args;
    }

    //========================================================================


    private static int countFormals(SyntaxValue formalsDecl)
        throws FusionException
    {
        // (lambda rest ___)
        if (formalsDecl instanceof SyntaxSymbol) return 1;

        // (lambda (formal ...) ___)
        return ((SyntaxSexp) formalsDecl).size();
    }


    private static String[] determineArgNames(Evaluator eval,
                                              SyntaxSexp formalsDecl)
        throws FusionException
    {
        int size = formalsDecl.size();
        if (size == 0) return EMPTY_STRING_ARRAY;

        String[] args = new String[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxSymbol identifier = (SyntaxSymbol) formalsDecl.get(eval, i);
            args[i] = identifier.stringValue();
        }
        return args;
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        SyntaxValue formalsDecl = stx.get(eval, 1);
        if (countFormals(formalsDecl) != 0)
        {
            // Dummy environment to keep track of depth
            env = new LocalEnvironment(env);
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
            String[] argNames =
                determineArgNames(eval, (SyntaxSexp) formalsDecl);
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
            throws FusionException
        {
            return new Closure(store, myArgNames, myBody);
        }
    }


    private static class Closure
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

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(args);

            Store localStore = new LocalStore(myEnclosure, args);

            return eval.bounceTailForm(localStore, myBody);
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
            checkArityExact(0, args);

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
            checkArityExact(1, args);

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
            checkArityExact(2, args);

            Store localStore = new LocalStore2(myEnclosure, args[0], args[1]);

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
