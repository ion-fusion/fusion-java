// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionCollection.unsafeCollectionSize;
import static dev.ionfusion.fusion.FusionSequence.unsafeSequenceElt;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpElement;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.ResultFailure.makeResultError;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.util.ArrayList;

final class LetValuesForm
    extends AbstractLetForm
{
    LetValuesForm()
    {
        super(/* multivalue? */ true, /* recursive? */ false);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp expr)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        var formSexp = expr.unwrap(eval);
        var decls = unsafeSyntaxUnwrap(eval, unsafeSexpElement(eval, formSexp, 1));

        // The number of bindings is >= the number of binding forms.
        int numBindingForms = unsafeCollectionSize(eval, decls);

        var binders = new ArrayList<SyntaxSymbol>();
        int[] valueCounts = new int[numBindingForms];
        CompiledForm[]     valueForms = new CompiledForm    [numBindingForms];
        ResourcePosition[] valuePosns = new ResourcePosition[numBindingForms];

        int bindingCount = 0;
        boolean allSingles = true;
        for (int i = 0; i < numBindingForms; i++)
        {
            var decl = unsafeSyntaxUnwrap(eval, unsafeSequenceElt(eval, decls, i));

            Object names = unsafeSyntaxUnwrap(eval, unsafePairHead(eval, decl));
            int size = 0;
            while (isPair(eval, names))
            {
                binders.add((SyntaxSymbol) unsafePairHead(eval, names));
                names = unsafePairTail(eval, names);
                size++;
            }

            bindingCount += size;
            valueCounts[i] = size;

            allSingles &= (size == 1);

            var valueExpr = (SyntaxValue<?>) unsafeSexpElement(eval, decl, 1);
            valueForms[i] = comp.compileExpression(env, valueExpr);
            valuePosns[i] = valueExpr.getPosition();
        }

        env = LocalEnvironment.forCompilation(env, binders.toArray(new SyntaxSymbol[0]));

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

            Store localStore = LocalStore.forExecution(store, boundValues);
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

            Store localStore = LocalStore.forExecution(store, boundValues);
            return eval.bounceTailForm(localStore, myBody);
        }
    }
}
