// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionNumber.checkIntArgToJavaInt;
import static com.amazon.fusion.FusionText.checkRequiredTextArg;
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
