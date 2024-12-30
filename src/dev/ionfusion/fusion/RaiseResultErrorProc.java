// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;



final class RaiseResultErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(3, args);

        String name     = checkRequiredTextArg(eval, this, 0, args);
        String expected = checkRequiredStringArg(eval, this, 1, args);
        Object actual = args[2];

        if (name.isEmpty()) name = "unknown procedure";

        throw new ResultFailure(name, expected, actual);
    }
}
