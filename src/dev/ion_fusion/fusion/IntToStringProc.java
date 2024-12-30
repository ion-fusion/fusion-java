// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionNumber.checkNullableIntArg;
import static dev.ion_fusion.fusion.FusionString.makeString;
import java.math.BigInteger;

final class IntToStringProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(1, args);

        BigInteger val = checkNullableIntArg(eval, this, 0, args);
        String text = (val == null ? null : val.toString());

        return makeString(eval, text);
    }
}
