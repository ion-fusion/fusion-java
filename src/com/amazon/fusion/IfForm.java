// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;


/**
 * The {@code if} syntactic form.
 */
final class IfForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        check(expander, stx).arityExact(4);
        return expandArgs(expander, env, stx);
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        CompiledForm testForm = comp.compileExpression(env, stx.get(eval, 1));
        CompiledForm thenForm = comp.compileExpression(env, stx.get(eval, 2));
        CompiledForm elseForm = comp.compileExpression(env, stx.get(eval, 3));

        return new CompiledIf(testForm, thenForm, elseForm);
    }


    //========================================================================


    private static final class CompiledIf
        implements CompiledForm
    {
        private final CompiledForm myTestForm;
        private final CompiledForm myThenForm;
        private final CompiledForm myElseForm;

        CompiledIf(CompiledForm testForm, CompiledForm thenForm,
                   CompiledForm elseForm)
        {
            myTestForm = testForm;
            myThenForm = thenForm;
            myElseForm = elseForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object result = eval.eval(store, myTestForm);

            boolean truthy = isTruthy(eval, result).isTrue();

            CompiledForm branch = truthy ? myThenForm : myElseForm;

            return eval.bounceTailForm(store, branch);
        }
    }
}
