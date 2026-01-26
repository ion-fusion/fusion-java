// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWriteToString;

/**
 * Represents an arbitrary, non-exception Fusion value thrown by {@code raise}.
 */
@SuppressWarnings("serial")
final class FusionUserException
    extends FusionException
{
    private final Object myRaisedValue;

    private FusionUserException(String message, Object raisedValue)
    {
        super(message);
        myRaisedValue = raisedValue;
    }

    /**
     * @param raisedValue must not extend {@link Throwable}.
     *
     * @return a new exception.
     */
    static FusionException make(Evaluator eval, Object raisedValue)
    {
        if (raisedValue instanceof Throwable)
        {
            String msg = "java.lang.Throwable cannot be raised from Fusion code";
            throw new IllegalArgumentException(msg, (Throwable) raisedValue);
        }

        String msg = safeWriteToString(eval, raisedValue);
        return new FusionUserException(msg, raisedValue);
    }


    @Override
    public final Object getRaisedValue()
    {
        return myRaisedValue;
    }
}
