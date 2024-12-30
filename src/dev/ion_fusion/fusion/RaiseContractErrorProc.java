// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionString.checkRequiredStringArg;
import static dev.ion_fusion.fusion.FusionText.checkRequiredTextArg;


final class RaiseContractErrorProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(2, args);

        String name    = checkRequiredTextArg(eval, this, 0, args);
        String message = checkRequiredStringArg(eval, this, 1, args);

        if (name.isEmpty()) name = "unknown procedure";

        throw new ContractException(name + ": " + message);
    }
}
