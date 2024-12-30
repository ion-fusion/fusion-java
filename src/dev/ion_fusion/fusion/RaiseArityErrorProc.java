// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ion_fusion.fusion.FusionText.checkRequiredTextArg;
import java.util.Arrays;


final class RaiseArityErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(2, args);

        String name      = checkRequiredTextArg(eval, this, 0, args);
        int arity        = checkIntArgToJavaInt(eval, this, 1, args);
        Object[] actuals = Arrays.copyOfRange(args, 2, args.length);

        if (name.isEmpty()) name = "unknown procedure";

        throw new ArityFailure(name, arity, arity, actuals);
    }
}
