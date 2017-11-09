// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeDisplay;
import static com.amazon.fusion.FusionIo.safeWriteToString;
import static com.amazon.fusion.FusionVoid.voidValue;

final class AssertForm
    extends SyntacticForm
{
    AssertForm()
    {
        //    "                                                                               |
        super("expr message ...",
              "Evaluates the `expr`, throwing an exception if the result isn't truthy.\n" +
              "The exception `display`s the `message`s, which are only evaluated on failure.");
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        check(expander, stx).arityAtLeast(2);
        return expandArgs(expander, env, stx);
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        SyntaxValue testFormSyntax = stx.get(eval, 1);
        CompiledForm testForm = comp.compileExpression(env, testFormSyntax);

        CompiledForm[] messageForms = comp.compileExpressions(env, stx, 2);

        SourceLocation location = testFormSyntax.getLocation();
        String expression = safeWriteToString(eval, testFormSyntax);

        return new CompiledAssert(testForm, messageForms,
                                  location, expression);
    }


    //========================================================================


    private static final class CompiledAssert
        implements CompiledForm
    {
        private final CompiledForm   myTestForm;
        private final CompiledForm[] myMessageForms;
        private final SourceLocation myLocation;
        private final String         myExpression;

        CompiledAssert(CompiledForm testForm,
                       CompiledForm[] messageForms,
                       SourceLocation location,
                       String expression)
        {
            myTestForm       = testForm;
            myMessageForms   = messageForms;
            myLocation       = location;
            myExpression     = expression;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object result = eval.eval(store, myTestForm);
            if (isTruthy(eval, result).isTrue())
            {
                return voidValue(eval);
            }

            String message;
            int size = myMessageForms.length;
            if (size != 0)
            {
                StringBuilder buf = new StringBuilder(256);
                for (CompiledForm messageForm : myMessageForms)
                {
                    Object messageValue = eval.eval(store, messageForm);

                    // Use safe API so we don't throw a different exception
                    safeDisplay(eval, buf, messageValue);
                }
                message = buf.toString();
            }
            else
            {
                message = null;
            }

            throw new FusionAssertionException(message, myLocation, myExpression, result);
        }
    }
}
