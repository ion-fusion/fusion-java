// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWriteToString;

/**
 * Runtime representation of Fusion macros, performing syntax expansion.
 */
final class MacroForm
    extends SyntacticForm
{
    private final Procedure myTransformer;

    MacroForm(Procedure transformer)
    {
        myTransformer = transformer;
    }


    final SyntaxValue expandOnce(Expander expander, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();

        // Must grab the origin identifier before a mark is applied.
        SyntaxSymbol origin = (SyntaxSymbol) stx.get(eval, 0);

        MarkWrap markWrap = new MarkWrap();

        stx = (SyntaxSexp) stx.addOrRemoveMark(markWrap);

        // TODO http://docs.racket-lang.org/reference/syntax-model.html
        // In addition, if the use of a transformer is in the same definition
        // context as its binding, the use-site syntax object is extended with
        // an additional fresh use-site scope that is not flipped in the
        // transformer’s result, so that only use-site syntax objects have the
        // use-site scope.
        //
        // TVJ: That may not be needed until implementing sets-of-scopes.

        SyntaxValue expanded = doExpandOnce(expander, stx);

        expanded = expanded.addOrRemoveMark(markWrap);

        // http://docs.racket-lang.org/reference/stxprops.html
        expanded = expanded.trackOrigin(eval, stx, origin);

        return expanded;
    }

    @Override
    final SyntaxValue expand(Expander expander, Environment env,
                             SyntaxSexp stx)
        throws FusionException
    {
        SyntaxValue expanded = expandOnce(expander, stx);

        // TODO FUSION-207 tail expand
        return expander.expand(env, expanded);
    }

    /**
     * Performs a single "level" of macro expansion.
     *
     * @param stx the input syntax, including the macro identifier.
     * @throws FusionException
     */
    private SyntaxValue doExpandOnce(Expander expander, SyntaxSexp stx)
        throws FusionException
    {
        Object expanded;
        try
        {
            // TODO FUSION-32 This should set current-namespace
            // See Racket Reference 1.2.3.2
            // http://docs.racket-lang.org/reference/syntax-model.html#(part._expand-steps)
            // But BEWARE: this is called during partial expansion!
            expanded = expander.getEvaluator().callNonTail(myTransformer, stx);
        }
        catch (FusionException e)
        {
            e.addContext(stx);
            throw e;
        }

        try
        {
            return (SyntaxValue) expanded;
        }
        catch (ClassCastException e)
        {
            String message =
                "Transformer returned non-syntax result: " +
                safeWriteToString(expander.getEvaluator(), expanded);
            throw new SyntaxException(myTransformer.identify(), message,
                                      stx);
        }
    }


    @Override
    final CompiledForm compile(Compiler comp, Environment env,
                               SyntaxSexp stx)
        throws FusionException
    {
        throw new IllegalStateException("Macros should be expanded already!");
    }
}
