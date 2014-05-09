// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import org.junit.Test;


public class DefineTest
    extends CoreTestCase
{
    @Test
    public void testRedefine()
        throws Exception
    {
        eval("(define x 93)");
        assertEval(93, "x");
        eval("(define y x)");
        assertEval(93, "x");
        assertEval(93, "y");
        eval("(define x 99)");
        assertEval(99, "x");
        assertEval(93, "y");
    }

    @Test
    public void badDefineSyntax()
        throws Exception
    {
        expectSyntaxExn("(define)");
        expectSyntaxExn("(define x)");
        expectSyntaxExn("(define x 2 3)");
        expectSyntaxExn("(define 1 2)");
        expectSyntaxExn("(define null.symbol 2)");
    }
}
