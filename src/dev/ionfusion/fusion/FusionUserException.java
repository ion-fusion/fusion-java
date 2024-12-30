// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.io.IOException;

/**
 * Represents an arbitrary, non-exception Fusion value thrown by {@code raise}.
 */
@SuppressWarnings("serial")
final class FusionUserException
    extends FusionException
{
    private final Object myRaisedValue;

    FusionUserException(Object raisedValue)
    {
        super((String) null);
        myRaisedValue = raisedValue;
    }

    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        FusionIo.write(eval, out, myRaisedValue);
    }

    @Override
    public final Object getRaisedValue()
    {
        return myRaisedValue;
    }
}
