// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionCollection.unsafeCollectionSize;
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionSequence.isSequence;
import static dev.ionfusion.fusion.FusionSyntax.isSyntax;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;

import dev.ionfusion.runtime.base.FusionException;

final class SyntaxSizeProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        if (isSyntax(eval, arg))
        {
            var datum = unsafeSyntaxUnwrap(eval, arg);
            if (isSequence(eval, datum))
            {
                var size = unsafeCollectionSize(eval, datum);
                return makeInt(eval, size);
            }
        }

        throw argError(eval, "syntax list or sexp", 0, arg);
    }
}
