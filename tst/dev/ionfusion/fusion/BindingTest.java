// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class BindingTest
    extends CoreTestCase
{
    @Test
    public void testNoBindingAtTop()
        throws Exception
    {
        expectUnboundIdentifierExn("g");
    }


    @Test
    public void testNoProcBindingAtTop()
        throws Exception
    {
        expectUnboundIdentifierExn("(g)");
    }

    @Test
    public void testNoArgBindingAtTop()
        throws Exception
    {
        expectUnboundIdentifierExn("(is_int g)");
    }

    @Test
    public void testNoBindingInProcedureBody()
        throws Exception
    {
        expectUnboundIdentifierExn("((lambda () g))");
    }
}
