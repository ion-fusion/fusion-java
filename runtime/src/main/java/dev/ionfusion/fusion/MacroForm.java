// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWriteToString;

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

        // TODO Eliminate this tail-call.
        //  https://github.com/ion-fusion/fusion-java/issues/71
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
            // TODO This should set current-namespace
            //  https://github.com/ion-fusion/fusion-java/issues/77
            // See Racket Reference 1.2.3.2 says:
            // "The call to the syntax transformer is parameterized to set
            // `current-namespace` to a namespace that shares bindings and
            // variables with the namespace being used to expand, except that
            // its base phase is one greater."
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
