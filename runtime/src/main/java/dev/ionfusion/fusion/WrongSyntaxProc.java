// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeDisplayManyToString;
import static dev.ionfusion.fusion.SyntaxException.makeSyntaxError;

import dev.ionfusion.runtime.base.FusionException;

/**
 * Fusion procedure to raise a syntax error.
 */
final class WrongSyntaxProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(eval, 1, args);
        SyntaxValue stx = checkSyntaxArg(eval, 0, args);

        String name = null; // TODO infer name
        String message = safeDisplayManyToString(eval, args, 1);

        throw makeSyntaxError(eval, name, message, stx);
    }
}
