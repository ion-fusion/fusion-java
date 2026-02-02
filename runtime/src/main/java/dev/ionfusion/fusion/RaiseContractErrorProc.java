// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;


final class RaiseContractErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(eval, 2, args);

        String name    = checkRequiredTextArg(eval, this, 0, args);
        String message = checkRequiredStringArg(eval, this, 1, args);

        if (name.isEmpty()) name = "unknown procedure";

        throw new ContractException(name + ": " + message);
    }
}
