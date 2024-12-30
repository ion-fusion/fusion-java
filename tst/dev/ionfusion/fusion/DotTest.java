// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.Test;


public class DotTest
    extends CoreTestCase
{
    @Test
    public void testDotArity()
        throws Exception
    {
        expectArityExn("(.)");
    }

    @Test
    public void testNoParts()
        throws Exception
    {
        // FUnit still can't compare structs
        assertEval("{f:true}", "(. {f:true})");
    }


    @Test
    public void testNonContainerMidway()
        throws Exception
    {
        expectContractExn("(. [true] 0 1)");
        expectContractExn("(. [{f:null}] 0 \"f\" 0)");
    }
}
