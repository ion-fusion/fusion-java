// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.math.BigInteger;

/**
 *
 */
final class ToIntProc
    extends Procedure
{
    ToIntProc()
    {
        //    "                                                                               |
        super("Converts a string to int format - can also accept an int on input and return" +
            " the same int back upon exit.");
    }

    @Override
    FusionValue invoke(Evaluator eval, FusionValue[] args)
        throws FusionException
    {
        checkArityExact(1,args);
        BigInteger result = null;

        try
        {
            String val = checkTextArg(0,args);
            result = new BigInteger(val);
        } catch (NumberFormatException e)
        {
            try
            {
                result = checkBigIntArg(0,args);
            } catch (NumberFormatException e2) { }
        }

        if (result != null)
        {
            return eval.newInt(result);
        }

        throw new FusionException("Invalid string cannot be converted to an int");
    }
}
