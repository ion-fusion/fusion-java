// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;
import static dev.ionfusion.fusion._private.FusionUtils.writeFriendlyIndex;

/**
 * Indicates a contractual failure of a result from some computation.
 */
@SuppressWarnings("serial")
final class ResultFailure
    extends ContractException
{
    private ResultFailure(String message)
    {
        super(message);
    }


    /**
     *
     * @param name must not be null.
     * @param expectation must not be null.
     * @param badPos the zero-based index of the problematic value.
     * If negative, a specific position isn't implicated.
     * @param actuals must not be null.
     */
    static FusionException makeResultError(Evaluator eval,
                                           String    name,
                                           String    expectation,
                                           int       badPos,
                                           Object... actuals)
    {
        assert name != null;
        assert badPos < actuals.length;

        StringBuilder out = new StringBuilder();

        int actualsLen = actuals.length;

        out.append(name);
        out.append(" expects ");
        out.append(expectation);

        if (0 <= badPos)
        {
            out.append(" as ");
            writeFriendlyIndex(out, badPos);
            out.append(" result, given ");
            safeWrite(eval, out, actuals[actualsLen == 1 ? 0 : badPos]);
        }

        if (actualsLen > 1 || (badPos < 0 && actualsLen != 0))
        {
            out.append(badPos < 0 ? "\nResults were:" : "\nOther results were:");

            for (int i = 0; i < actualsLen; i++)
            {
                if (i != badPos)
                {
                    out.append("\n  ");
                    safeWrite(eval, out, actuals[i]);
                }
            }
        }

        return new ResultFailure(out.toString());
    }

    static FusionException makeResultError(Evaluator eval,
                                           String    name,
                                           String    expectation,
                                           Object... actuals)
    {
        return makeResultError(eval, name, expectation, -1, actuals);
    }
}
