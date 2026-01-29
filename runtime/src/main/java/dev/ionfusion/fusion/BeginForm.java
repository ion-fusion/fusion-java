// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * The {@code begin} syntactic form.
 */
final class BeginForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        // At module context, we should've spliced this into the module body.
        assert ! (expander.isModuleContext() || expander.isTopLevelContext());
        // TODO handle splicing in internal-defn context
        //  https://github.com/ion-fusion/fusion-java/issues/67

        int size = stx.size(eval);

        SyntaxValue[] expandedChildren = new SyntaxValue[size];
        expandedChildren[0] = stx.get(eval, 0);

        for (int i = 1; i < size; i++)
        {
            SyntaxValue subform = stx.get(eval, i);
            expandedChildren[i] = expander.expandExpression(env, subform);
        }
        return stx.copyReplacingChildren(eval, expandedChildren);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        return comp.compileBegin(env, stx, 1);
    }
}
