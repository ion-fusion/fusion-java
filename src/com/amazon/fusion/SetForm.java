// Copyright (c) 2012-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

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
        if (binding instanceof FreeBinding)
        {
            throw check.failure("variable has no binding", id);
        }

        SyntaxValue[] children = stx.extract(eval);
        SyntaxValue valueExpr = stx.get(eval, 2);
        children[2] = expander.expandExpression(env, valueExpr);

        return stx.copyReplacingChildren(eval, children);
    }


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        CompiledForm valueForm = eval.compile(env, stx.get(eval, 2));

        SyntaxSymbol id = (SyntaxSymbol) stx.get(eval, 1);
        Binding binding = id.getBinding();

        return binding.compileSet(eval, env, valueForm);
    }
}
