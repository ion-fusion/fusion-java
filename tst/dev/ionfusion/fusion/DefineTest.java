// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.Test;


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
