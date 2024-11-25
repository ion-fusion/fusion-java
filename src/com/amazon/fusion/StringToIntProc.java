// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionNumber.makeInt;
import static com.amazon.fusion.FusionString.checkNullableStringArg;
import java.math.BigInteger;

final class StringToIntProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(1, args);

        String val = checkNullableStringArg(eval, this, 0, args);

        BigInteger bigInt = (val != null ? parse(val, args) : null);

        return makeInt(eval, bigInt);
    }


    BigInteger parse(String text, Object[] args)
        throws ArgumentException
    {
        try
        {
            // Work-around behavior of BigInteger constructor, which (in
            // defiance of its documentation) accepts leading '+'.
            if (! text.startsWith("+"))
            {
                return new BigInteger(text);
            }
        }
        catch (NumberFormatException e) { }

        throw argFailure("valid int content", 0, args);
    }
}
