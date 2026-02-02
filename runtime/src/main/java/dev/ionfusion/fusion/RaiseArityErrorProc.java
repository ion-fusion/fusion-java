// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.ArityFailure.makeArityError;
import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;

import java.util.Arrays;


final class RaiseArityErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(eval, 2, args);

        String name      = checkRequiredTextArg(eval, this, 0, args);
        int arity        = checkIntArgToJavaInt(eval, this, 1, args);
        Object[] actuals = Arrays.copyOfRange(args, 2, args.length);

        if (name.isEmpty()) name = "unknown procedure";

        throw makeArityError(eval, name, arity, arity, actuals);
    }
}
