// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.isString;

final class DefineSyntaxForm
    extends SyntacticForm
{
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        if (! (expander.isTopLevelContext() || expander.isModuleContext()))
        {
            throw check.failure("Definition must be at top-level or module level");
        }

        int arity = check.arityAtLeast(3);

        SyntaxValue[] children = stx.extract(eval);

        SyntaxSymbol identifier = check.requiredIdentifier(1);

        // WARNING!  This isn't conditional as with 'define' since
        // 'define_syntax' doesn't get predefined by the 'module' expander.
        {
            Namespace ns = env.namespace();
            assert ns == env;
            children[1] = ns.predefine(identifier, stx);
        }

        int bodyPos;
        SyntaxValue maybeDoc = children[2];
        if (isString(eval, maybeDoc.unwrap(eval)) && arity > 3)
        {
            bodyPos = 3;
        }
        else
        {
            bodyPos = 2;
        }

        if (bodyPos != arity-1)
        {
            throw check.failure("Too many subforms");
        }

        SyntaxValue valueStx = stx.get(eval, bodyPos);
        children[bodyPos] = expander.expandExpression(env, valueStx);

        return stx.copyReplacingChildren(eval, children);
    }


    //========================================================================


    @Override
    void evalCompileTimePart(Compiler comp,
                             TopLevelNamespace topNs,
                             SyntaxSexp topStx)
        throws FusionException
    {
        CompiledForm compiledForm = compile(comp, topNs, topStx);
        comp.getEvaluator().eval(topNs, compiledForm);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        return comp.compileDefineSyntax(env, stx);
    }
}
