// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonValue;
import java.util.Iterator;

/**
 *
 */
final class ReadProc
    extends Procedure
{
    private Iterator<IonValue> myInputValues;

    ReadProc()
    {
        //    "                                                                               |
        super("Reads an Ion value from the standard input stream. Returns undef when there's\n" +
              "no more data; use (is_undef) to check for it.");
    }

    @Override
    FusionValue invoke(Evaluator eval, FusionValue[] args)
        throws FusionException
    {
        checkArityExact(args);

        if (myInputValues == null)
        {
            myInputValues = eval.getSystem().iterate(System.in);
        }

        FusionValue result;
        if (myInputValues.hasNext())
        {
            IonValue v = myInputValues.next();
            result = new DomValue(v);
        }
        else
        {
            result = FusionValue.UNDEF;
        }

        return result;
    }
}
