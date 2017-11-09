// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWriteToString;


final class ParameterizeForm
    extends SyntacticForm
{
    ParameterizeForm()
    {
        //    "                                                                               |
        super("((PARAM EXPR) ...) BODY ...+",
              "Dynamically binds the PARAMs to the EXPR values while evaluating the BODY.\n" +
              "The PARAMs are evaluated first, in order; each must result in a dynamic\n" +
              "parameter procedure. The EXPRs are then evaluated in order, and then the params\n" +
              "are changed to their results for the dynamic extent of the BODY.\n" +
              "BODY may be one or more forms; the result of the last form is the result of the\n" +
              "entire expression.");
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        final int exprSize = check.arityAtLeast(3);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of parameterizations", 1);
        SyntaxSequence bindingForms = checkBindings.form();

        final int numBindings = bindingForms.size();
        SyntaxValue[] expandedForms = new SyntaxValue[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("parameter/value pair", i);
            checkPair.arityExact(2);

            SyntaxSexp binding = (SyntaxSexp) checkPair.form();

            SyntaxValue paramExpr = binding.get(eval, 0);
            paramExpr = expander.expandExpression(env, paramExpr);

            SyntaxValue boundExpr = binding.get(eval, 1);
            boundExpr = expander.expandExpression(env, boundExpr);

            binding = binding.copyReplacingChildren(eval, paramExpr, boundExpr);
            expandedForms[i] = binding;
        }

        bindingForms = bindingForms.copyReplacingChildren(eval, expandedForms);

        // Expand the body expressions
        expandedForms = new SyntaxValue[exprSize];
        expandedForms[0] = stx.get(eval, 0);
        expandedForms[1] = bindingForms;

        // TODO FUSION-36 Should allow internal definitions
        for (int i = 2; i < exprSize; i++)
        {
            SyntaxValue bodyExpr = stx.get(eval, i);
            expandedForms[i] = expander.expandExpression(env, bodyExpr);
        }

        return stx.copyReplacingChildren(eval, expandedForms);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        SyntaxSequence bindingForms = (SyntaxSequence) stx.get(eval, 1);

        final int numBindings = bindingForms.size();

        CompiledForm[] parameterForms = new CompiledForm[numBindings];
        CompiledForm[] valueForms     = new CompiledForm[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);

            SyntaxValue paramExpr = binding.get(eval, 0);
            parameterForms[i] = comp.compileExpression(env, paramExpr);

            SyntaxValue valueExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, valueExpr);
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        return new CompiledParameterize(parameterForms, valueForms, body);
    }


    //========================================================================


    private static final class CompiledParameterize
        implements CompiledForm
    {
        private final CompiledForm[] myParameterForms;
        private final CompiledForm[] myValueForms;
        private final CompiledForm   myBody;

        CompiledParameterize(CompiledForm[] parameterForms,
                             CompiledForm[] valueForms,
                             CompiledForm   body)
        {
            myParameterForms = parameterForms;
            myValueForms     = valueForms;
            myBody           = body;
        }


        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int numBindings = myParameterForms.length;

            DynamicParameter[] parameters = new DynamicParameter[numBindings];
            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm paramForm = myParameterForms[i];
                Object paramValue = eval.eval(store, paramForm);
                try
                {
                    parameters[i] = (DynamicParameter) paramValue;
                }
                catch (ClassCastException e)
                {
                    String message =
                        "Parameter expression evaluated to non-parameter: " +
                        safeWriteToString(eval, paramValue);
                    throw new ContractException(message);
                }
            }

            Object[] boundValues = new Object[numBindings];
            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm valueForm = myValueForms[i];
                Object value = eval.eval(store, valueForm);
                boundValues[i] = value;
            }

            Evaluator bodyEval = eval.markedContinuation(parameters, boundValues);

            // TODO TAIL
            return bodyEval.eval(store, myBody);
        }
    }
}
