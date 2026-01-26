// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;

import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;

@SuppressWarnings("serial")
public final class FusionAssertionException
    extends FusionErrorException
{
    private final String myExpression;
    private final Object myResult;

    /**
     * @param message may be null.
     * @param location may be null.
     * @param result must not be null.
     */
    FusionAssertionException(String message,
                             SourceLocation location,
                             String expression,
                             Object result)
    {
        super(message);
        myExpression = expression;
        myResult = result;

        addContext(location);
    }

    /**
     * Returns the formatted message as provided by the application.
     *
     * @return may be null if no message values were provided.
     */
    public String getUserMessage()
    {
        return getBaseMessage();
    }

    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        out.append("Assertion failure: ");

        super.displayMessage(eval, out);

        out.append("\nExpression: ");
        out.append(myExpression);
        out.append("\nResult:     ");
        safeWrite(eval, out, myResult);
    }
}
