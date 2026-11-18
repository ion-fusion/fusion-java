// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionCollection.unsafeCollectionSize;
import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionSequence.unsafeSequenceElt;
import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpElement;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.Syntax.rebuild;

import dev.ionfusion.runtime.base.FusionException;


final class ParameterizeForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        final int exprSize = check.arityAtLeast(3);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of parameterizations", 1);

        var expandedDecls = checkBindings.subformSexpMap("parameter/value pair",
                                                          checkDecl -> {
            checkDecl.arityExact(2);

            var decl = checkDecl.sequence();
            var paramExpr = unsafeSexpElement(eval, decl, 0);
            var valueExpr = unsafeSexpElement(eval, decl, 1);

            paramExpr = expander.expandExpression(env, (SyntaxValue<?>) paramExpr);
            valueExpr = expander.expandExpression(env, (SyntaxValue<?>) valueExpr);

            var expanded = immutableSexp(eval, paramExpr, valueExpr);
            return rebuild(eval, expanded, checkDecl.form());
        });

        // Expand the body expressions
        var expandedForms = new SyntaxValue[exprSize];
        expandedForms[0] = stx.get(eval, 0);
        expandedForms[1] = rebuild(eval, expandedDecls, checkBindings.form());

        // TODO #67 Should allow internal definitions
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

        var formSexp = stx.unwrap(eval);
        var decls = unsafeSyntaxUnwrap(eval, unsafeSexpElement(eval, formSexp, 1));

        int numBindings = unsafeCollectionSize(eval, decls);

        CompiledForm[] paramForms = new CompiledForm[numBindings];
        CompiledForm[] valueForms = new CompiledForm[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            var decl = unsafeSyntaxUnwrap(eval, unsafeSequenceElt(eval, decls, i));

            var paramExpr = unsafeSexpElement(eval, decl, 0);
            var valueExpr = unsafeSexpElement(eval, decl, 1);

            paramForms[i] = comp.compileExpression(env, paramExpr);
            valueForms[i] = comp.compileExpression(env, valueExpr);
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        return new CompiledParameterize(paramForms, valueForms, body);
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
