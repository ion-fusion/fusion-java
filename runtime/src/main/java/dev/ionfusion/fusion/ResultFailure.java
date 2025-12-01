// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;
import static dev.ionfusion.fusion.FusionUtils.writeFriendlyIndex;
import java.io.IOException;

/**
 * Indicates a contractual failure of a result from some computation.
 */
@SuppressWarnings("serial")
final class ResultFailure
    extends ContractException
{
    private final String     myName;
    private final String     myExpectation;
    private final int        myBadPos;
    private final Object[]   myActuals;



    /**
     *
     * @param name must not be null.
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actual must not be null.
     */
    ResultFailure(String name, String expectation,
                  int badPos, Object actual)
    {
        super("result failure");
        assert name != null && actual != null;

        myName = name;
        myExpectation = expectation;
        myBadPos = badPos;
        myActuals = new Object[]{ actual };
    }

    /**
     *
     * @param name must not be null.
     * @param actual must not be null.
     */
    ResultFailure(String name, String expectation, Object actual)
    {
        this(name, expectation, -1, actual);
    }

    /**
     *
     * @param name
     * @param expectation
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actuals must not be null.
     */
    ResultFailure(String name, String expectation,
                  int badPos, Object[] actuals)
    {
        super("result failure");
        assert name != null;
        assert badPos < actuals.length;

        myName = name;
        myExpectation = expectation;
        myBadPos = badPos;
        myActuals = actuals;
    }


    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        int actualsLen = myActuals.length;

        out.append(myName);
        out.append(" expects ");
        out.append(myExpectation);

        if (0 <= myBadPos)
        {
            out.append(" as ");
            writeFriendlyIndex(out, myBadPos);
            out.append(" result, given ");
            safeWrite(eval, out, myActuals[actualsLen == 1 ? 0 : myBadPos]);
        }

        if (actualsLen > 1 || (myBadPos < 0 && actualsLen != 0))
        {
            out.append(myBadPos < 0
                     ? "\nResults were:"
                     : "\nOther results were:");

            for (int i = 0; i < actualsLen; i++)
            {
                if (i != myBadPos)
                {
                    out.append("\n  ");
                    safeWrite(eval, out, myActuals[i]);
                }
            }
        }
    }
}
