// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * The {@code quote} syntactic form.
 */
final class QuoteForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        SyntaxChecker check = check(expander, stx);
        check.arityExact(2);
        return stx;
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        SyntaxValue quotedSource = stx.get(eval, 1);
        Object result = quotedSource.syntaxToDatum(eval);
        return new CompiledConstant(result);
    }
}
