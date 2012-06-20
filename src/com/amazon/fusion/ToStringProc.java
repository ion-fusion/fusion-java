// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.math.BigInteger;

/**
 *
 */
final class ToStringProc
    extends Procedure
{
    ToStringProc()
    {
        //    "                                                                               |
        super("Converts an integer to string format. Input of string types will be "+
              "returned in the same way");
    }

    @Override
    FusionValue invoke(Evaluator eval, FusionValue[] args)
        throws FusionException
    {
        checkArityExact(1,args);
        String result;

        try
        {
            BigInteger val = checkBigIntArg(0,args);
            result = val.toString();
        } catch (ArgTypeFailure e)
        {
            try
            {
                result = checkTextArg(0, args);
            } catch(ArgTypeFailure e1)
            {
                throw e1;
            }
        }

        if (result != null)
        {
            return eval.newString(result);
        }

        throw new FusionException("Input argument type is invalid");
    }
}
