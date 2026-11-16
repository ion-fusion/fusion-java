// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.FusionException;

/**
 * Base class for syntactic forms.
 */
abstract class SyntacticForm
    implements NamedValue<BaseSymbol>
{
    private BaseSymbol myName;

    public final void inferObjectName(BaseSymbol name)
    {
        if (myName == null)
        {
            myName = name;
        }
    }

    public final BaseSymbol objectName()
    {
        return myName;
    }


    abstract SyntaxValue expand(Expander expander, Environment env,
                                SyntaxSexp stx)
        throws FusionException;


    abstract CompiledForm compile(Compiler comp, Environment env,
                                  SyntaxSexp stx)
        throws FusionException;


    /**
     * Evaluates the expansion-time code of a top-level form.
     *
     * @param comp active compiler
     * @param topNs they namespace in which we're evaluating
     * @param topStx a fully expanded top-level form.
     * @throws FusionException
     *
     * @see FusionEval#evalCompileTimePartOfTopLevel
     */
    void evalCompileTimePart(Compiler comp,
                             TopLevelNamespace topNs,
                             SyntaxSexp topStx)
        throws FusionException
    {
        // Most forms have nothing to do.
    }


    //========================================================================
    // Type-checking helpers

    final SyntaxChecker check(Evaluator eval, SyntaxSexp form)
    {
        String name = objectName().stringValue();
        return new SyntaxChecker(eval, name, form);
    }

    final SyntaxChecker check(Expander expander, SyntaxSexp form)
    {
        return check(expander.getEvaluator(), form);
    }
}
