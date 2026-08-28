// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionSyntax.checkSyntaxArg;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ResourcePosition;


class SyntaxLineProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        SyntaxValue      stx = checkSyntaxArg(eval, this, 0, arg);
        ResourcePosition pos = stx.getPosition();
        if (pos != null)
        {
            return makeInt(eval, pos.getLine());
        }
        return FusionNumber.ZERO_INT;
    }
}
