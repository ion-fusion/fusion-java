// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

final class SetForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        check.arityExact(3);

        SyntaxSymbol id = check.requiredIdentifier("variable identifier", 1);
        Binding binding = id.resolve();

        String message = binding.mutationSyntaxErrorMessage();
        if (message != null)
        {
            throw check.failure(message + ": " + id);
        }

        SyntaxValue[] children = stx.extract(eval);
        children[2] = expander.expandExpression(env, children[2]);

        return stx.copyReplacingChildren(eval, children);
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        return comp.compileSet(env, stx);
    }
}
