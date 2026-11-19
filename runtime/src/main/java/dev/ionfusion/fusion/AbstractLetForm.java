// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSequence.unsafeSequenceMap;
import static dev.ionfusion.fusion.SyntaxSymbol.ensureUniqueIdentifiers;

import dev.ionfusion.runtime.base.FusionException;
import java.util.ArrayList;

abstract class AbstractLetForm
    extends SyntacticForm
{
    private final boolean myBindingsAreMultiValue;
    private final boolean myBindingsAreRecursive;

    protected AbstractLetForm(boolean bindingsAreMultiValue,
                              boolean bindingsAreRecursive)
    {
        myBindingsAreMultiValue = bindingsAreMultiValue;
        myBindingsAreRecursive  = bindingsAreRecursive;
    }


    @Override
    SyntaxValue<?> expand(Expander expander, Environment outerEnv, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        check.arityAtLeast(3);

        SyntaxChecker decls = check.subformSeq("sequence of bindings", 1);

        var binders = collectBinders(eval, decls);
        ensureUniqueIdentifiers(eval, binders, stx);

        var localEnv = LocalEnvironment.forExpansion(outerEnv, binders);

        var subforms = stx.extract(eval);
        subforms[1] = expandBindingPairs(expander, outerEnv, localEnv, decls);

        // TODO #67 Should allow internal definitions
        for (int i = 2; i < subforms.length; i++)
        {
            var bodyForm = localEnv.applyContext(subforms[i]);
            subforms[i] = expander.expandExpression(localEnv, bodyForm);
        }

        return stx.copyReplacingChildren(eval, subforms);
    }


    SyntaxSymbol[] collectBinders(Evaluator eval, SyntaxChecker checkPairs)
        throws FusionException
    {
        int pairCount = checkPairs.size();

        var binders = new ArrayList<SyntaxSymbol>(pairCount);
        for (int i = 0; i < pairCount; i++)
        {
            var checkPair = checkPairs.subformSexp("binding pair", i);
            checkPair.arityExact(2);

            if (myBindingsAreMultiValue)
            {
                var checkIds = checkPair.subformSexp("binding names", 0);
                int size     = checkIds.size();
                for (int j = 0; j < size; j++)
                {
                    var binder = checkIds.requiredIdentifier("bound name", j);
                    binders.add(binder);
                }
            }
            else
            {
                var binder = checkPair.requiredIdentifier("bound name", 0);
                binders.add(binder);
            }
        }

        return binders.toArray(SyntaxSymbol.EMPTY_ARRAY);
    }


    SyntaxValue<?> expandBindingPairs(Expander expander,
                                      Environment outerEnv,
                                      LocalEnvironment localEnv,
                                      SyntaxChecker bindingPairs)
        throws FusionException
    {
        var eval = expander.getEvaluator();

        var seq = bindingPairs.sequence();
        var expanded = unsafeSequenceMap(eval, seq, bp -> {
            var bindingPair = (SyntaxSexp) bp;
            var binders = bindingPair.get(eval, 0);
            var expr    = bindingPair.get(eval, 1);

            // Binders always get the local context so they resolve properly.
            binders = localEnv.applyContext(binders);

            // Value expressions only get the local context for recursive forms.
            if (myBindingsAreRecursive)
            {
                expr = localEnv.applyContext(expr);
            }

            expr = expander.expandExpression(outerEnv, expr);

            return bindingPair.copyReplacingChildren(eval, binders, expr);
        });

        return Syntax.rebuild(eval, expanded, bindingPairs.form());
    }
}
