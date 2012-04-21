// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import org.junit.Test;


public class DotTest
    extends CoreTestCase
{
    @Test
    public void testDotArity()
        throws Exception
    {
        expectArityFailure("(.)");
    }

    @Test
    public void testNoParts()
        throws Exception
    {
        assertEval("[]", "(. [])");
        assertEval("{}", "(.{})");
        assertEval("{f:true}", "(. {f:true})");
    }

    @Test
    public void testStructParts()
        throws Exception
    {
        assertEval("true", "(. {f:true} \"f\")");
        assertEval("true", "(. {f:true} (quote f))");
        assertEval("\"oy\"", "(. {f:{g:'''oy'''}, h:true} \"f\" (quote g))");
    }

    @Test
    public void testSequenceParts()
        throws Exception
    {
        assertEval("99", "(. [99, \"hello\"] 0)");
        assertEval("{{}}", "(. [99, [true, {{}}]] 1 1)");
    }

    @Test
    public void testMissingPart()
        throws Exception
    {
        assertEval("true", "(is_undef (. {f:1} \"g\"))");
        assertEval("true", "(is_undef (. {f:1,g:[2]} \"g\" 1 1))");
        assertEval("true", "(is_undef (. [1] 1))");
        assertEval("true", "(is_undef (. [1, {f:2}] 1 \"g\" 1))");
    }

    @Test
    public void testDotArgType()
        throws Exception
    {
        for (String form : nonContainerExpressions())
        {
            String expr = "(. " + form + ")";
            expectArgTypeFailure(expr, 0);

            expr = "(. " + form + " 12)";
            expectArgTypeFailure(expr, 0);
        }
    }

    @Test
    public void testNonContainerMidway()
        throws Exception
    {
        expectContractFailure("(. [true] 0 1)");
        expectContractFailure("(. [{f:null}] 0 \"f\" 0)");
    }
}
