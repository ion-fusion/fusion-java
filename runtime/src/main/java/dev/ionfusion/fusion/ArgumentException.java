// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;
import static dev.ionfusion.fusion._private.FusionUtils.writeFriendlyIndex;

import java.io.IOException;
import java.util.Arrays;

/**
 * Indicates a failure applying a procedure with the wrong type of argument.
 */
@SuppressWarnings("serial")
final class ArgumentException
    extends ContractException
{
    private static final BaseValue REDACTED_VALUE =
        new BaseValue()
        {
            @Override
            void write(Evaluator eval, Appendable out) throws IOException
            {
                out.append("{{{REDACTED}}}");
            }
        };

    static ArgumentException makeSanitizedException(Evaluator eval, ArgumentException e)
    {
        Object[] values = new Object[e.myActuals.length];
        Arrays.fill(values, REDACTED_VALUE);
        return makeArgumentError(eval, e.getName(), e.getExpectation(),
                                 e.getBadPos(), values);
    }

    private final String   myName;
    private final String   myExpectation;
    private final int      myBadPos;
    private final Object[] myActuals;



    /**
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actuals must not be null or zero-length.
     */
    public static ArgumentException makeArgumentError(Evaluator eval, String name, String expectation, int badPos, Object... actuals)
    {
        StringBuilder b = new StringBuilder();
        b.append(name);
        b.append(" expects ");
        b.append(expectation);

        int actualsLen = actuals.length;
        if (0 <= badPos)
        {
            b.append(" as ");
            writeFriendlyIndex(b, badPos);
            b.append(" argument, given ");
            safeWrite(eval, b, actuals[actualsLen == 1 ? 0 : badPos]);
        }

        if (actualsLen != 1 || badPos < 0)
        {
            b.append(badPos < 0
                         ? "\nArguments were:"
                         : "\nOther arguments were:");

            for (int i = 0; i < actualsLen; i++)
            {
                if (i != badPos)
                {
                    b.append("\n  ");
                    safeWrite(eval, b, actuals[i]);
                }
            }
        }

        return new ArgumentException(b.toString(), name, expectation, badPos, actuals);
    }

    /**
     * @param badPos the zero-based index of the problematic value.
     *   -1 means a specific position isn't implicated.
     * @param actuals must not be null or zero-length.
     */
    private ArgumentException(String message,
                              String name, String expectation,
                              int badPos, Object... actuals)
    {
        super(message);
        assert name != null && actuals.length != 0;

        // We allow badPos to be anything if there's only one actual provided.
//      assert badPos < actuals.length;

        myName = name;
        myExpectation = expectation;
        myBadPos = badPos;
        myActuals = actuals;
    }


    String getName()
    {
        return myName;
    }

    String getExpectation()
    {
        return myExpectation;
    }

    int getBadPos()
    {
        return myBadPos;
    }

    int getActualsLength()
    {
        return myActuals.length;
    }
}
