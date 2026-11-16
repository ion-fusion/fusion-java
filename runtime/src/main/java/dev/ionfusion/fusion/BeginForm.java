// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.Syntax.mapOverSubforms;

import dev.ionfusion.runtime.base.FusionException;

/**
 * The {@code begin} syntactic form.
 */
final class BeginForm
    extends SyntacticForm
{
    @Override
    SyntaxValue<?> expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        // At module context, we should've spliced this into the module body.
        assert ! (expander.isModuleContext() || expander.isTopLevelContext());
        // TODO #67 handle splicing in internal-defn context

        return mapOverSubforms(eval, stx,
                               c -> expander.expandExpression(env, (SyntaxValue<?>) c));
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        return comp.compileBegin(env, stx, 1);
    }
}
