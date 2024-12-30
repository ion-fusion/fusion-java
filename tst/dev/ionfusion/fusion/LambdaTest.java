// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import org.junit.jupiter.api.Test;


public class LambdaTest
    extends CoreTestCase
{
    @Test
    public void testBasicLambda()
        throws Exception
    {
        assertEval(3, "((lambda (p) 3) 4)");
    }

    @Test
    public void testLambdaArity()
        throws Exception
    {
        expectSyntaxExn("(lambda)");
        expectSyntaxExn("(lambda (arg))");
    }

    @Test
    public void testDefineLambda()
        throws Exception
    {
        eval("(define f (lambda (p) 3))");
        assertEval(3, "(f 4)");
    }

    @Test
    public void testArgShadowsTopLevel()
        throws Exception
    {
        eval("(define p 19)");
        eval("(define f (lambda (p) p))");
        assertEval(4, "(f 4)");
    }

    @Test
    public void testBodySeesTopLevel()
        throws Exception
    {
        eval("(define p 19)");
        eval("(define f (lambda (p1) p))");
        assertEval(19, "(f 4)");
        eval("(define p 20)");
        assertEval(20, "(f 4)");
    }

    @Test
    public void testBodySeesOuterArg()
        throws Exception
    {
        eval("(define p 19)");
        eval("(define f (lambda (p) (lambda (p1) p)))");
        evalToProcedure("(f 4)");
        assertEval(4, "((f 4) 5)");
    }

    @Test
    public void testLambdaArguments()
        throws Exception
    {
        eval("(define p 19)");
        eval("(define f (lambda (g) (lambda (p) (g p))))");
        evalToProcedure("(f 4)");
        assertEval(76, "((f (lambda (x) 76)) 5)");
        eval("(define f2 (f (lambda (x) 77)))");
        assertEval(77, "(f2 5)");
    }

    @Test
    public void testNoArgs()
        throws Exception
    {
        assertEval("true", "((lambda () true))");
        assertEval(13, "((lambda (f) (f)) (lambda () 13))");
        assertEval(1, "((let ((x 1)) (lambda () x)))");
    }

    @Test
    public void testMultipleArguments()
        throws Exception
    {
        assertEval(1, "((lambda (x y) x) 1 2)");
        assertEval(2, "((lambda (x y) y) 1 2)");
        assertEval(2, "((lambda (x y) (x y)) (lambda (x) x) 2)");

        eval("(define i (lambda ( x )x))");
        assertEval(2, "((lambda (x y) ((y y) (y x))) 2 i)");
    }

    @Test
    public void testMultipleBodyForms()
        throws Exception
    {
        assertEval(2, "((lambda () 1 2))");
        assertEval(1, "((lambda (x y) y x) 1 2)");
    }

    @Test
    public void testFormalsSyntaxFailure()
        throws Exception
    {
        expectSyntaxExn("(lambda (1) 1)");
        expectSyntaxExn("(lambda (a 2) 1)");
        expectSyntaxExn("(lambda (a []) 1)");
    }

    @Test
    public void testArgSyntaxFailure()
        throws Exception
    {
        expectSyntaxExn("((lambda (x) 1) (if 2))");
    }

    @Test
    public void testWrongNumberOfArguments()
        throws Exception
    {
        expectArityExn("((lambda (x y) (x y)) (lambda () 1) 2)");
    }
}
