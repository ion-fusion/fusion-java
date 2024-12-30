// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;
import java.util.Arrays;


final class RaiseArgumentErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(4, args);

        String name     = checkRequiredTextArg(eval, this, 0, args);
        String expected = checkRequiredStringArg(eval, this, 1, args);
        int    badPos   = checkIntArgToJavaInt(eval, this, 2, args);

        Object[] actuals = Arrays.copyOfRange(args, 3, args.length);

        if (name.isEmpty()) name = "unknown procedure";

        if (actuals.length <= badPos)
        {
            // The position is bad, but we don't want to blow up because these
            // code paths are generally untested. Better to act like we don't
            // know which argument is bad; at least an error message is given.
            badPos = -1;
        }

        throw new ArgumentException(name, expected, badPos, actuals);
    }
}
