// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonBool;
import com.amazon.ion.IonDecimal;
import com.amazon.ion.IonInt;
import com.amazon.ion.IonString;
import com.amazon.ion.IonTimestamp;
import com.amazon.ion.IonValue;

final class EqualProc
    extends Procedure
{
    EqualProc()
    {
        //    "                                                                               |
        super("Returns true if A and B are equal, ignoring precision. The arguments must be\n" +
              "the same type: non-null integer, decimal, boolean, string, or timestamp.",
              "a", "b");
    }

    private static final String EXPECTATION =
        "non-null bool, int, decimal, string, or timestamp";

    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(args);

        IonValue leftVal  = FusionValue.castToIonValueMaybe(args[0]);
        IonValue rightVal = FusionValue.castToIonValueMaybe(args[1]);
        boolean result = false;
        int compareVal = 0;

        if (leftVal  == null || leftVal.isNullValue() ||
            rightVal == null || rightVal.isNullValue())
        {
            throw new ArgTypeFailure(this, EXPECTATION, -1, args);
        }

        if (leftVal instanceof IonInt && rightVal instanceof IonInt)
        {
            IonInt left  = (IonInt) leftVal;
            IonInt right = (IonInt) rightVal;
            compareVal = left.bigIntegerValue().compareTo(right.bigIntegerValue());
            result = (compareVal == 0);
        }
        else if (leftVal instanceof IonDecimal && rightVal instanceof IonDecimal)
        {
            IonDecimal left = (IonDecimal) leftVal;
            IonDecimal right = (IonDecimal) rightVal;
            compareVal = left.bigDecimalValue().compareTo(right.bigDecimalValue());
            result = (compareVal == 0);
        }
        else if (leftVal instanceof IonString && rightVal instanceof IonString)
        {
            IonString left  = (IonString) leftVal;
            IonString right = (IonString) rightVal;
            result = left.stringValue().equals(right.stringValue());
        }
        else if (leftVal instanceof IonTimestamp && rightVal instanceof IonTimestamp)
        {
            IonTimestamp left = (IonTimestamp) leftVal;
            IonTimestamp right = (IonTimestamp) rightVal;
            compareVal = left.timestampValue().compareTo(right.timestampValue());
            result = (compareVal == 0);
        }
        else if (leftVal instanceof IonBool && rightVal instanceof IonBool)
        {
            // Bool is checked last since it's least likely to be used, I assume.
            IonBool left  = (IonBool) leftVal;
            IonBool right = (IonBool) rightVal;
            result = left.booleanValue() == right.booleanValue();
        }
        else
        {
            throw new ArgTypeFailure(this, EXPECTATION, -1, args);
        }

        return eval.newBool(result);
    }
}
