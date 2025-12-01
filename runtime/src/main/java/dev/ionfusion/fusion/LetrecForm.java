// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionValue.UNDEF;
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

        final int numBindings = bindingForms.size();
        SyntaxSymbol[] boundNames = new SyntaxSymbol[numBindings];
        for (int i = 0; i < numBindings; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("binding pair", i);
            checkPair.arityExact(2);
            boundNames[i] = checkPair.requiredIdentifier("bound name", 0);
        }

        Environment bodyEnv = new LocalEnvironment(env, boundNames, stx);
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

        final int numBindings = bindingForms.size();

        CompiledForm  [] valueForms = new CompiledForm  [numBindings];
        SourceLocation[] valueLocns = new SourceLocation[numBindings];

        for (int i = 0; i < numBindings; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);
            SyntaxValue boundExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, boundExpr);
            valueLocns[i] = boundExpr.getLocation();
        }

        CompiledForm body = comp.compileBegin(env, stx, 2);

        switch (valueForms.length)
        {
            case 0:
                return body;
            case 1:
                return new CompiledLetrec1(valueForms, valueLocns, body);
            case 2:
                return new CompiledLetrec2(valueForms, valueLocns, body);
            default:
                return new CompiledLetrec(valueForms, valueLocns, body);
        }
    }


    //========================================================================


    private static final class CompiledLetrec
        implements CompiledForm
    {
        private final CompiledForm[]   myValueForms;
        private final SourceLocation[] myValueLocns;
        private final CompiledForm     myBody;

        CompiledLetrec(CompiledForm[]   valueForms,
                       SourceLocation[] valueLocns,
                       CompiledForm     body)
        {
            myValueForms = valueForms;
            myValueLocns = valueLocns;
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
                CompiledForm   form = myValueForms[i];
                SourceLocation locn = myValueLocns[i];
                boundValues[i] = eval.eval(localStore, form, locn);
            }

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledLetrec1
        implements CompiledForm
    {
        private final CompiledForm   myValueForm0;
        private final SourceLocation myValueLocn0;
        private final CompiledForm   myBody;

        CompiledLetrec1(CompiledForm[]   valueForms,
                        SourceLocation[] valueLocns,
                        CompiledForm     body)
        {
            myValueForm0 = valueForms[0];
            myValueLocn0 = valueLocns[0];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Store localStore = new LocalStore1(store, UNDEF);

            Object value = eval.eval(localStore, myValueForm0, myValueLocn0);
            localStore.set(0, value);

            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledLetrec2
        implements CompiledForm
    {
        private final CompiledForm   myValueForm0;
        private final CompiledForm   myValueForm1;
        private final SourceLocation myValueLocn0;
        private final SourceLocation myValueLocn1;
        private final CompiledForm   myBody;

        CompiledLetrec2(CompiledForm[]   valueForms,
                        SourceLocation[] valueLocns,
                        CompiledForm     body)
        {
            myValueForm0 = valueForms[0];
            myValueForm1 = valueForms[1];
            myValueLocn0 = valueLocns[0];
            myValueLocn1 = valueLocns[1];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Store localStore = new LocalStore2(store, UNDEF, UNDEF);

            Object value = eval.eval(localStore, myValueForm0, myValueLocn0);
            localStore.set(0, value);

            value = eval.eval(localStore, myValueForm1, myValueLocn1);
            localStore.set(1, value);

            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
