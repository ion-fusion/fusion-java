// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionValue.UNDEF;
import static dev.ionfusion.fusion.SyntaxSymbol.ensureUniqueIdentifiers;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.util.Arrays;

final class LetrecForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        final int letrecExprSize = check.arityAtLeast(3);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of bindings", 1);
        SyntaxSequence bindingForms = checkBindings.form();

        final int numBindings = bindingForms.size(eval);
        SyntaxSymbol[] boundNames = new SyntaxSymbol[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("binding pair", i);
            checkPair.arityExact(2);
            boundNames[i] = checkPair.requiredIdentifier("bound name", 0);
        }
        ensureUniqueIdentifiers(eval, boundNames, stx);

        Environment bodyEnv = new LocalEnvironment(env, boundNames);
        SyntaxWrap localWrap = new EnvironmentWrap(bodyEnv);

        // Expand the bound-value expressions
        SyntaxValue[] expandedForms = new SyntaxValue[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            // Wrap the bound names so they resolve to their own binding.
            SyntaxSymbol name = boundNames[i].addWrap(localWrap);
            name.resolve();

            // Already type- and arity-checked this above
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);
            SyntaxValue boundExpr = binding.get(eval, 1);
            boundExpr = boundExpr.addWrap(localWrap);
            boundExpr = expander.expandExpression(bodyEnv, boundExpr);
            expandedForms[i] =
                binding.copyReplacingChildren(eval, name, boundExpr);
        }

        bindingForms = bindingForms.copyReplacingChildren(eval, expandedForms);

        expandedForms = new SyntaxValue[letrecExprSize];
        expandedForms[0] = stx.get(eval, 0);
        expandedForms[1] = bindingForms;

        // TODO Should allow internal definitions
        //  https://github.com/ion-fusion/fusion-java/issues/67
        for (int i = 2; i < letrecExprSize; i++)
        {
            SyntaxValue subform = stx.get(eval, i);
            subform = subform.addWrap(localWrap);
            expandedForms[i] = expander.expandExpression(bodyEnv, subform);
        }

        return stx.copyReplacingChildren(eval, expandedForms);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        // Dummy environment to keep track of depth
        env = new LocalEnvironment(env);

        SyntaxSequence bindingForms = (SyntaxSequence) stx.get(eval, 1);

        final int numBindings = bindingForms.size(eval);

        CompiledForm    [] valueForms = new CompiledForm  [numBindings];
        ResourcePosition[] valuePosns = new ResourcePosition[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);
            SyntaxValue boundExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, boundExpr);
            valuePosns[i] = boundExpr.getPosition();
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        switch (valueForms.length)
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

            Store localStore = new LocalStore(store, boundValues);

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
