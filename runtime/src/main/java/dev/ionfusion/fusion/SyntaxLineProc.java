// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.makeInt;

import dev.ionfusion.runtime.base.SourceLocation;


class SyntaxLineProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        SyntaxValue stx = checkSyntaxArg(eval, 0, arg);
        SourceLocation location = stx.getLocation();
        if (location != null)
        {
            return makeInt(eval, location.getLine());
        }
        return FusionNumber.ZERO_INT;
    }
}
