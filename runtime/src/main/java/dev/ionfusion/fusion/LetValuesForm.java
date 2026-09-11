// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.ResultFailure.makeResultError;
import static dev.ionfusion.fusion.SyntaxSymbol.ensureUniqueIdentifiers;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.util.ArrayList;

final class LetValuesForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        final int letExprSize = check.arityAtLeast(3);

        SyntaxChecker checkBindings =
            check.subformSeq("sequence of bindings", 1);
        SyntaxSequence bindingForms = checkBindings.form();

        final int numBindingForms = bindingForms.size(eval);

        // Gather the bound names
        ArrayList<SyntaxSymbol> boundNameList =
            new ArrayList<>(numBindingForms);
        for (int i = 0; i < numBindingForms; i++)
        {
            SyntaxChecker checkPair =
                checkBindings.subformSexp("binding pair", i);
            checkPair.arityExact(2);

            SyntaxChecker checkBoundNames =
                checkPair.subformSexp("binding name sequence", 0);

            int size = checkBoundNames.form().size(eval);
            for (int j = 0; j < size; j++)
            {
                SyntaxSymbol name =
                    checkBoundNames.requiredIdentifier("binding name", j);
                boundNameList.add(name);
            }
        }


        SyntaxSymbol[] boundNames;
        Environment bodyEnv;
        SyntaxWrap localWrap;

        final int bindingCount = boundNameList.size();
        if (bindingCount == 0)
        {
            boundNames = null;
            bodyEnv = env;
            localWrap = null;
        }
        else
        {
            boundNames = boundNameList.toArray(SyntaxSymbol.EMPTY_ARRAY);
            ensureUniqueIdentifiers(eval, boundNames, stx);
            bodyEnv = new LocalEnvironment(env, boundNames);
            localWrap = new EnvironmentWrap(bodyEnv);
        }

        // Expand the bound-value expressions
        SyntaxValue[] expandedForms = new SyntaxValue[numBindingForms];
        int bindingPos = 0;
        for (int i = 0; i < numBindingForms; i++)
        {
            // Already type- and arity-checked this above
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);

            SyntaxSexp names = (SyntaxSexp) binding.get(eval, 0);
            SyntaxValue[] wrappedNames = names.extract(eval);
            int size = names.size(eval);
            for (int j = 0; j < size; j++)
            {
                // Wrap the bound names so they resolve to their own binding.
                SyntaxSymbol name = boundNames[bindingPos];
                assert name == wrappedNames[j];

                name = name.addWrap(localWrap);
                name.resolve();
                wrappedNames[j] = name;
                bindingPos++;
            }
            names = names.copyReplacingChildren(eval, wrappedNames);

            SyntaxValue boundExpr = binding.get(eval, 1);
            boundExpr = expander.expandExpression(env, boundExpr);
            expandedForms[i] =
                binding.copyReplacingChildren(eval, names, boundExpr);
        }
        assert bindingPos == bindingCount;

        bindingForms = bindingForms.copyReplacingChildren(eval, expandedForms);

        expandedForms = new SyntaxValue[letExprSize];
        expandedForms[0] = stx.get(eval, 0);
        expandedForms[1] = bindingForms;

        // TODO Should allow internal definitions
        //  https://github.com/ion-fusion/fusion-java/issues/67
        for (int i = 2; i < letExprSize; i++)
        {
            SyntaxValue subform = stx.get(eval, i);
            if (localWrap != null)
            {
                subform = subform.addWrap(localWrap);
            }
            expandedForms[i] = expander.expandExpression(bodyEnv, subform);
        }

        return stx.copyReplacingChildren(eval, expandedForms);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp expr)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        SyntaxSequence bindingForms = (SyntaxSequence) expr.get(eval, 1);

        // The number of bindings is >= the number of binding forms.
        final int numBindingForms = bindingForms.size(eval);

        int[] valueCounts = new int[numBindingForms];
        CompiledForm[]     valueForms = new CompiledForm    [numBindingForms];
        ResourcePosition[] valuePosns = new ResourcePosition[numBindingForms];

        int bindingCount = 0;
        boolean allSingles = true;
        for (int i = 0; i < numBindingForms; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);

            SyntaxSexp names = (SyntaxSexp) binding.get(eval, 0);
            int size = names.size(eval);
            bindingCount += size;
            valueCounts[i] = size;

            allSingles &= (size == 1);

            SyntaxValue boundExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, boundExpr);
            valuePosns[i] = boundExpr.getPosition();
        }

        if (bindingCount != 0)
        {
            // Dummy environment to keep track of depth
            env = new LocalEnvironment(env);
        }

        CompiledForm body = comp.compileBegin(env, expr, 2);

        if (allSingles)
        {
            return compilePlainLet(valueForms, valuePosns, body);
        }

        return new CompiledLetValues(bindingCount, valueCounts, valueForms,
                                     valuePosns, body);
    }


    static CompiledForm compilePlainLet(CompiledForm[]     valueForms,
                                        ResourcePosition[] valuePosns,
                                        CompiledForm body)
    {
        switch (valueForms.length)
        {
            case 0:
                // Note that this doesn't allocate an environment rib!
                // This only works because no-arg lambdas and no-binding
                // let_values are compiled without a local environment.
                return body;
            case 1:
                return new CompiledPlainLet1(valueForms, valuePosns, body);
            case 2:
                return new CompiledPlainLet2(valueForms, valuePosns, body);
            default:
                return new CompiledPlainLet (valueForms, valuePosns, body);
        }
    }


    //========================================================================


    /**
     * "Plain let" is when each expression produces one value.
     */
    private static final class CompiledPlainLet
        implements CompiledForm
    {
        private final CompiledForm[]     myValueForms;
        private final ResourcePosition[] myValuePosns;
        private final CompiledForm       myBody;

        CompiledPlainLet(CompiledForm[]     valueForms,
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

            for (int i = 0; i < numBindings; i++)
            {
                CompiledForm     form = myValueForms[i];
                ResourcePosition pos  = myValuePosns[i];
                Object values = eval.eval(store, form, pos);
                eval.checkSingleResult(values, "local-binding form");
                boundValues[i] = values;
            }

            Store localStore = new LocalStore(store, boundValues);
            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledPlainLet1
        implements CompiledForm
    {
        private final CompiledForm     myValueForm0;
        private final ResourcePosition myValuePos0;
        private final CompiledForm     myBody;

        CompiledPlainLet1(CompiledForm    [] valueForms,
                          ResourcePosition[] valuePosns,
                          CompiledForm body)
        {
            myValueForm0 = valueForms[0];
            myValuePos0  = valuePosns[0];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm0, myValuePos0);
            eval.checkSingleResult(value, "local-binding form");

            Store localStore = new LocalStore1(store, value);
            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledPlainLet2
        implements CompiledForm
    {
        private final CompiledForm     myValueForm0;
        private final CompiledForm     myValueForm1;
        private final ResourcePosition myValuePos0;
        private final ResourcePosition myValuePos1;
        private final CompiledForm     myBody;

        CompiledPlainLet2(CompiledForm    [] valueForms,
                          ResourcePosition[] valuePosns,
                          CompiledForm     body)
        {
            myValueForm0 = valueForms[0];
            myValueForm1 = valueForms[1];
            myValuePos0  = valuePosns[0];
            myValuePos1  = valuePosns[1];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value0 = eval.eval(store, myValueForm0, myValuePos0);
            eval.checkSingleResult(value0, "local-binding form");

            Object value1 = eval.eval(store, myValueForm1, myValuePos1);
            eval.checkSingleResult(value1, "local-binding form");

            Store localStore = new LocalStore2(store, value0, value1);
            return eval.bounceTailForm(localStore, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLetValues
        implements CompiledForm
    {
        private final int                myBindingCount;
        private final int[]              myValueCounts;
        private final CompiledForm[]     myValueForms;
        private final ResourcePosition[] myValuePosns;
        private final CompiledForm       myBody;

        CompiledLetValues(int                bindingCount,
                          int[]              valueCounts,
                          CompiledForm[]     valueForms,
                          ResourcePosition[] valuePosns,
                          CompiledForm       body)
        {
            assert valueCounts.length == valueForms.length;

            myBindingCount = bindingCount;
            myValueCounts  = valueCounts;
            myValueForms   = valueForms;
            myValuePosns   = valuePosns;
            myBody         = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            final int numBindingForms = myValueForms.length;

            Object[] boundValues = new Object[myBindingCount];

            int bindingPos = 0;
            for (int i = 0; i < numBindingForms; i++)
            {
                CompiledForm     form = myValueForms[i];
                ResourcePosition posn = myValuePosns[i];
                Object values = eval.eval(store, form, posn);

                int expectedCount = myValueCounts[i];
                if (expectedCount == 1)
                {
                    eval.checkSingleResult(values, "let_values");
                    boundValues[bindingPos++] = values;
                }
                else if (values instanceof Object[])
                {
                    Object[] vals = (Object[]) values;
                    int actualCount = vals.length;
                    if (expectedCount != actualCount)
                    {
                        String expectation =
                            expectedCount + " results but received " +
                            actualCount;
                        throw makeResultError(eval, "local-binding form", expectation, vals);
                    }

                    System.arraycopy(vals, 0,
                                     boundValues, bindingPos,
                                     actualCount);
                    bindingPos += actualCount;
                }
                else
                {
                    String expectation =
                        expectedCount + " results but received 1";
                    throw makeResultError(eval, "local-binding form", expectation, values);
                }
            }

            Store localStore = new LocalStore(store, boundValues);
            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
