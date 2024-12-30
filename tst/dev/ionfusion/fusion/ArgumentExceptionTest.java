// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Tests that redacted exceptions do not contain actual arguments.
 */
public class ArgumentExceptionTest
    extends CoreTestCase
{
    private static final String REDACTED_STRING = "{{{REDACTED}}}";

    @Test
    public void testSanitizedFactoryMethod()
        throws Exception
    {
        ArgumentException exception = new ArgumentException(
            "testSanitized", "nop", 0, "super secret data");
        ArgumentException sanitized =
            ArgumentException.makeSanitizedException(exception);

        assertEquals(exception.getName(), sanitized.getName());
        assertEquals(exception.getExpectation(), sanitized.getExpectation());
        assertEquals(exception.getBadPos(), sanitized.getBadPos());
        assertEquals(exception.getActualsLength(), sanitized.getActualsLength());

        StringBuilder buff = new StringBuilder();
        sanitized.displayMessage(evaluator(), buff);
        assertEquals(
            "testSanitized expects nop as 1st argument, given " + REDACTED_STRING,
            buff.toString());
    }

    @Test
    public void testSanitizedFactoryMethodWithMultiple()
        throws Exception
    {
        ArgumentException exception = new ArgumentException(
            "testSanitized", "nop", -1, "super secret data 1",
            "super secret data 2", "super secret data 3");
        ArgumentException sanitized =
            ArgumentException.makeSanitizedException(exception);

        assertEquals(exception.getName(), sanitized.getName());
        assertEquals(exception.getExpectation(), sanitized.getExpectation());
        assertEquals(exception.getBadPos(), sanitized.getBadPos());
        assertEquals(exception.getActualsLength(), sanitized.getActualsLength());

        StringBuilder buff = new StringBuilder();
        sanitized.displayMessage(evaluator(), buff);
        assertEquals(
            "testSanitized expects nop\nArguments were:" +
            "\n  " + REDACTED_STRING +
            "\n  " + REDACTED_STRING +
            "\n  " + REDACTED_STRING,
            buff.toString());
    }
}
