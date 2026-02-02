// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;

@SuppressWarnings("serial")
final class FusionAssertionException
    extends FusionErrorException
{
    private final String myUserMessage;

    /**
     * @param userMessage may be null.
     */
    private FusionAssertionException(String displayMessage, String userMessage)
    {
        super(displayMessage);
        myUserMessage = userMessage;
    }

    /**
     * Returns the formatted message as provided by the application.
     *
     * @return may be null if no message values were provided.
     */
    public String getUserMessage()
    {
        return myUserMessage;
    }


    static FusionException makeAssertError(Evaluator eval,
                                           String userMessage,
                                           String expression,
                                           Object result)
    {
        StringBuilder out = new StringBuilder("Assertion failure: ");

        if (userMessage != null)
        {
            out.append(userMessage);
        }

        out.append("\nExpression: ");
        safeWrite(eval, out, expression);

        out.append("\nResult:     ");
        safeWrite(eval, out, result);

        return new FusionAssertionException(out.toString(), userMessage);
    }
}
