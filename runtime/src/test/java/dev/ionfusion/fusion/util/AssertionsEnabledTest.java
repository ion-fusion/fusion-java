// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;


public class AssertionsEnabledTest
{
    /**
     * Make sure our unit test framework has assertions enabled.
     */
    @Test
    public void testAssertionsEnabled()
    {
        String message = "Java assertion failure";
        try
        {
            assert false : message;
            fail("Assertions not enabled");
        }
        catch (AssertionError e)
        {
            assertEquals(message, e.getMessage());
        }
    }
}
