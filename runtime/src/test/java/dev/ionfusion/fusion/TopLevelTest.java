// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.runtime.embed.TopLevel;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TopLevelTest
    extends CoreTestCase
{
    @Test
    public void testLookupFromImport()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("pair");
        assertTrue(FusionProcedure.isProcedure(top, fv));
    }

    @Test
    public void testLookupNoBinding()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("no binding!");
        assertNull(fv);
    }

    @Test
    public void testDefineAndLookup()
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", 12);
        Object fv = top.lookup("v");
        assertTrue(FusionNumber.isInt(top, fv));

        top.define("A questionable name", null);
        fv = top.lookup("A questionable name");
        assertTrue(FusionVoid.isVoid(top, fv));
    }

    @Test
    public void testCallProcByValue()
        throws Exception
    {
        TopLevel top = topLevel();
        Object plus = top.lookup("+");
        assertTrue(FusionProcedure.isProcedure(top, plus));
        checkLong(3, top.call(plus, 1, 2));
    }
}
