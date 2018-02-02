// Copyright (c) 2014-2018 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionString.checkRequiredStringArg;
import static com.amazon.fusion.FusionText.checkRequiredTextArg;


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
