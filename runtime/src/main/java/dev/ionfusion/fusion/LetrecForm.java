// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionValue.UNDEF;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.util.Arrays;

final class LetrecForm
    extends AbstractLetForm
{
    LetrecForm()
    {
        super(/* multivalue? */ false, /* recursive? */ true);
    }


    //========================================================================


    /**
     * Extract the {@code id}s from a form like {@code ((id expr) ...)} or
     * {@code [(id expr), ...]}.
     */
    private static SyntaxSymbol[] collectBinders(Evaluator eval,
                                                 SyntaxSequence<?> bindings)
        throws FusionException
    {
        final int numBindings = bindings.size(eval);

        SyntaxSymbol[] binders = new SyntaxSymbol[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindings.get(eval, i);
            binders[i] = (SyntaxSymbol) binding.get(eval, 0);
        }

        return binders;
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        var bindingForms = (SyntaxSequence) stx.get(eval, 1);
        var binders = collectBinders(eval, bindingForms);
        env = LocalEnvironment.forCompilation(env, binders);

        final int numBindings = binders.length;
        CompiledForm    [] valueForms = new CompiledForm    [numBindings];
        ResourcePosition[] valuePosns = new ResourcePosition[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);
            SyntaxValue boundExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, boundExpr);
            valuePosns[i] = boundExpr.getPosition();
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        switch (numBindings)
        {
            case 0:
                return body;
            case 1:
                return new CompiledLetrec1(valueForms, valuePosns, body);
            case 2:
                return new CompiledLetrec2(valueForms, valuePosns, body);
            default:
                return new CompiledLetrec(valueForms, valuePosns, body);
        }
    }


    //========================================================================


    private static final class CompiledLetrec
        implements CompiledForm
    {
        private final CompiledForm[]     myValueForms;
        private final ResourcePosition[] myValuePosns;
        private final CompiledForm       myBody;

        CompiledLetrec(CompiledForm[]     valueForms,
                       ResourcePosition[] valuePosns,
                       CompiledForm       body)
        {
            myValueForms = valueForms;
            myValuePosns = valuePosns;
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int numBindings = myValueForms.length;

            Object[] boundValues = new Object[numBindings];
            Arrays.fill(boundValues, UNDEF);

            Store localStore = LocalStore.forExecution(store, boundValues);

            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm     form = myValueForms[i];
                ResourcePosition posn = myValuePosns[i];
                boundValues[i] = eval.eval(localStore, form, posn);
            }

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledLetrec1
        implements CompiledForm
    {
        private final CompiledForm     myValueForm0;
        private final ResourcePosition myValuePosn0;
        private final CompiledForm     myBody;

        CompiledLetrec1(CompiledForm[]     valueForms,
                        ResourcePosition[] valuePosns,
                        CompiledForm       body)
        {
            myValueForm0 = valueForms[0];
            myValuePosn0 = valuePosns[0];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Store localStore = new LocalStore1(store, UNDEF);

            Object value = eval.eval(localStore, myValueForm0, myValuePosn0);
            localStore.set(0, value);

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledLetrec2
        implements CompiledForm
    {
        private final CompiledForm     myValueForm0;
        private final CompiledForm     myValueForm1;
        private final ResourcePosition myValuePosn0;
        private final ResourcePosition myValuePosn1;
        private final CompiledForm     myBody;

        CompiledLetrec2(CompiledForm[]     valueForms,
                        ResourcePosition[] valuePosns,
                        CompiledForm       body)
        {
            myValueForm0 = valueForms[0];
            myValueForm1 = valueForms[1];
            myValuePosn0 = valuePosns[0];
            myValuePosn1 = valuePosns[1];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Store localStore = new LocalStore2(store, UNDEF, UNDEF);

            Object value = eval.eval(localStore, myValueForm0, myValuePosn0);
            localStore.set(0, value);

            value = eval.eval(localStore, myValueForm1, myValuePosn1);
            localStore.set(1, value);

            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
