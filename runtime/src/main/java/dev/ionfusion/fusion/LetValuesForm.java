// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

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

        final int numBindingForms = bindingForms.size();

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

            for (int j = 0; j < checkBoundNames.form().size(); j++)
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
            bodyEnv = new LocalEnvironment(env, boundNames, stx);
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
            for (int j = 0; j < names.size(); j++)
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
        final int numBindingForms = bindingForms.size();

        int[] valueCounts = new int[numBindingForms];
        CompiledForm[]   valueForms = new CompiledForm  [numBindingForms];
        SourceLocation[] valueLocs  = new SourceLocation[numBindingForms];

        int bindingCount = 0;
        boolean allSingles = true;
        for (int i = 0; i < numBindingForms; i++)
        {
            SyntaxSexp binding = (SyntaxSexp) bindingForms.get(eval, i);

            SyntaxSexp names = (SyntaxSexp) binding.get(eval, 0);
            int size = names.size();
            bindingCount += size;
            valueCounts[i] = size;

            allSingles &= (size == 1);

            SyntaxValue boundExpr = binding.get(eval, 1);
            valueForms[i] = comp.compileExpression(env, boundExpr);
            valueLocs [i] = boundExpr.getLocation();
        }

        if (bindingCount != 0)
        {
            // Dummy environment to keep track of depth
            env = new LocalEnvironment(env);
        }

        CompiledForm body = comp.compileBegin(env, expr, 2);

        if (allSingles)
        {
            return compilePlainLet(valueForms, valueLocs, body);
        }

        return new CompiledLetValues(bindingCount, valueCounts, valueForms,
                                     valueLocs, body);
    }


    static CompiledForm compilePlainLet(CompiledForm[]   valueForms,
                                        SourceLocation[] valueLocs,
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
                return new CompiledPlainLet1(valueForms, valueLocs, body);
            case 2:
                return new CompiledPlainLet2(valueForms, valueLocs, body);
            default:
                return new CompiledPlainLet (valueForms, valueLocs, body);
        }
    }


    //========================================================================


    /**
     * "Plain let" is when each expression produces one value.
     */
    private static final class CompiledPlainLet
        implements CompiledForm
    {
        private final CompiledForm[]   myValueForms;
        private final SourceLocation[] myValueLocs;
        private final CompiledForm     myBody;

        CompiledPlainLet(CompiledForm  [] valueForms,
                         SourceLocation[] valueLocs,
                         CompiledForm     body)
        {
            myValueForms = valueForms;
            myValueLocs  = valueLocs;
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
                CompiledForm   form = myValueForms[i];
                SourceLocation loc  = myValueLocs [i];
                Object values = eval.eval(store, form, loc);
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
        private final CompiledForm   myValueForm0;
        private final SourceLocation myValueLoc0;
        private final CompiledForm   myBody;

        CompiledPlainLet1(CompiledForm  [] valueForms,
                          SourceLocation[] valueLocs,
                          CompiledForm body)
        {
            myValueForm0 = valueForms[0];
            myValueLoc0  = valueLocs [0];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm0, myValueLoc0);
            eval.checkSingleResult(value, "local-binding form");

            Store localStore = new LocalStore1(store, value);
            return eval.bounceTailForm(localStore, myBody);
        }
    }


    private static final class CompiledPlainLet2
        implements CompiledForm
    {
        private final CompiledForm myValueForm0;
        private final CompiledForm myValueForm1;
        private final SourceLocation myValueLoc0;
        private final SourceLocation myValueLoc1;
        private final CompiledForm myBody;

        CompiledPlainLet2(CompiledForm  [] valueForms,
                          SourceLocation[] valueLocs,
                          CompiledForm     body)
        {
            myValueForm0 = valueForms[0];
            myValueForm1 = valueForms[1];
            myValueLoc0  = valueLocs [0];
            myValueLoc1  = valueLocs [1];
            myBody       = body;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value0 = eval.eval(store, myValueForm0, myValueLoc0);
            eval.checkSingleResult(value0, "local-binding form");

            Object value1 = eval.eval(store, myValueForm1, myValueLoc1);
            eval.checkSingleResult(value1, "local-binding form");

            Store localStore = new LocalStore2(store, value0, value1);
            return eval.bounceTailForm(localStore, myBody);
        }
    }


    //========================================================================


    private static final class CompiledLetValues
        implements CompiledForm
    {
        private final int              myBindingCount;
        private final int[]            myValueCounts;
        private final CompiledForm[]   myValueForms;
        private final SourceLocation[] myValueLocns;
        private final CompiledForm     myBody;

        CompiledLetValues(int              bindingCount,
                          int[]            valueCounts,
                          CompiledForm[]   valueForms,
                          SourceLocation[] valueLocns,
                          CompiledForm     body)
        {
            assert valueCounts.length == valueForms.length;

            myBindingCount = bindingCount;
            myValueCounts  = valueCounts;
            myValueForms   = valueForms;
            myValueLocns   = valueLocns;
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
                CompiledForm   form = myValueForms[i];
                SourceLocation locn = myValueLocns[i];
                Object values = eval.eval(store, form, locn);

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
                        throw new ResultFailure("local-binding form",
                                                expectation, -1, vals);
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
                    throw new ResultFailure("local-binding form",
                                            expectation, -1, values);
                }
            }

            Store localStore = new LocalStore(store, boundValues);
            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
