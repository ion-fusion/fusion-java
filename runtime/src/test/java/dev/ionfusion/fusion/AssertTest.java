// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * These checks can't be ported to Fusion yet because there's no way to inspect an
 * exception's message.
 */
public class AssertTest
    extends CoreTestCase
{
    private void expectAssertFailure(String expr)
        throws Exception
    {
        try
        {
            eval("(assert " + expr + ")");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals(null, e.getUserMessage());
        }

        try
        {
            eval("(assert " + expr + " \"barney\")");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals("barney", e.getUserMessage());
        }

        try
        {
            eval("(assert " + expr + " \"barney\" 13)");
            Assertions.fail("Expected exception");
        }
        catch (FusionAssertionException e)
        {
            Assertions.assertEquals("barney13", e.getUserMessage());
        }
    }

    @Test
    public void testAssertFailure()
        throws Exception
    {
        for (String form : BooleanTest.UNTRUTHY_EXPRESSIONS)
        {
            expectAssertFailure(form);
        }
    }
}
