// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWrite;
import static com.amazon.fusion.FusionUtils.writeFriendlyIndex;
import java.io.IOException;

/**
 * Indicates a failure applying a procedure with the wrong type of argument.
 */
@SuppressWarnings("serial")
final class ArgumentException
    extends ContractException
{
    private final String   myName;
    private final String   myExpectation;
    private final int      myBadPos;
    private final Object[] myActuals;


    /**
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actuals must not be null or zero-length.
     */
    ArgumentException(String name, String expectation,
                      int badPos, Object... actuals)
    {
        super("arg type failure");
        assert name != null && actuals.length != 0;

        // We allow badPos to be anything if there's only one actual provided.
//      assert badPos < actuals.length;

        myName = name;
        myExpectation = expectation;
        myBadPos = badPos;
        myActuals = actuals;
    }

    /**
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actual must not be null.
     */
    ArgumentException(String name, String expectation,
                      int badPos, Object actual)
    {
        this(name, expectation, badPos, new Object[] { actual });
    }

    /**
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actuals must not be null or zero-length.
     */
    ArgumentException(NamedValue name, String expectation,
                      int badPos, Object... actuals)
    {
        this(name.identify(), expectation, badPos, actuals);
    }

    /**
     * @param badPos the index of the problematic argument.
     *   -1 means a specific arg isn't implicated.
     * @param actual must not be null.
     */
    ArgumentException(NamedValue name, String expectation,
                      int badPos, Object actual)
    {
        this(name.identify(), expectation, badPos, new Object[]{ actual });
        assert actual != null;
    }


    public int getBadPos()
    {
        return myBadPos;
    }

    @Override
    void displayMessage(Evaluator eval, Appendable b)
        throws IOException, FusionException
    {
        int actualsLen = myActuals.length;

        b.append(myName);
        b.append(" expects ");
        b.append(myExpectation);

        if (0 <= myBadPos)
        {
            b.append(" as ");
            writeFriendlyIndex(b, myBadPos);
            b.append(" argument, given ");
            safeWrite(eval, b, myActuals[actualsLen == 1 ? 0 : myBadPos]);
        }

        if (actualsLen != 1 || myBadPos < 0)
        {
            b.append(myBadPos < 0
                     ? "\nArguments were:"
                     : "\nOther arguments were:");

            for (int i = 0; i < actualsLen; i++)
            {
                if (i != myBadPos)
                {
                    b.append("\n  ");
                    safeWrite(eval, b, myActuals[i]);
                }
            }
        }
    }
}
