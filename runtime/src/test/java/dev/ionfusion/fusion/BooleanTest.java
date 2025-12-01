// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.isBool;
import static dev.ionfusion.fusion.FusionBool.isFalse;
import static dev.ionfusion.fusion.FusionBool.isTrue;
import static dev.ionfusion.fusion.TailCallTest.STACK_OVERFLOW_DEPTH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class BooleanTest
    extends CoreTestCase
{
    @Test
    public void testBoolBasics()
        throws Exception
    {
        Object fv = topLevel().eval("true");
        assertTrue(isBool(topLevel(), fv));
        assertTrue(isTrue(topLevel(), fv));
        assertFalse(isFalse(topLevel(), fv));

        fv = topLevel().eval("false");
        assertTrue(isBool(topLevel(), fv));
        assertFalse(isTrue(topLevel(), fv));
        assertTrue(isFalse(topLevel(), fv));

        fv = topLevel().eval("null.bool");
        assertTrue(isBool(topLevel(), fv));
        assertFalse(isTrue(topLevel(), fv));
        assertFalse(isFalse(topLevel(), fv));

        fv = topLevel().eval("null");
        assertFalse(isBool(topLevel(), fv));
        assertFalse(isTrue(topLevel(), fv));
        assertFalse(isFalse(topLevel(), fv));
    }



    public static final String[] TRUTHY_EXPRESSIONS =
    {
        "true",
        "((lambda () true))",
        "0",
        "1",
        "0.",
        "nan",
        "+inf",
        "-inf",
        "0e0",
        "2012-10-19T12:54-08:00",
        "\"\"",
        "(quote '')",
        "(quote 'sym')",
        "{{}}",
        "{{\"\"}}",
        "[]",
        "(quote ())",
        "{}",
        "(letrec [(x y), (y 2)] x)",
    };

    public static final String[] UNTRUTHY_EXPRESSIONS =
    {
        "false",
        "((lambda () false))",
        "null",
        "null.bool",
        "null.int",
        "null.decimal",
        "null.float",
        "null.timestamp",
        "null.string",
        "(quote null.symbol)",
        "null.blob",
        "null.clob",
        "null.list",
        "(quote null.sexp)",
        "null.struct",
        "(quote ann::null)",  // Annotations don't affect truthiness
    };


    @Test
    public void testTruthiness()
        throws Exception
    {
        eval("(define n null)");
        assertEval(2, "(if null 1 2)");
        assertEval(2, "(if null.bool 1 2)");
        assertEval(2, "(if n 1 2)");
        assertEval(1, "(if [] 1 2)");
        assertEval(1, "(if (lambda (x) true) 1 2)");
    }

    @Test
    public void testTruthyIf()
        throws Exception
    {
        for (String form : TRUTHY_EXPRESSIONS)
        {
            assertEval(1, "(if " + form + " 1 2)");
        }
    }

    @Test
    public void testUntruthyIf()
        throws Exception
    {
        for (String form : UNTRUTHY_EXPRESSIONS)
        {
            assertEval(2, "(if " + form + " 1 2)");
        }
    }


    @Test
    public void testIfArity()
        throws Exception
    {
        expectSyntaxExn("(if)");
        expectSyntaxExn("(if true)");
        expectSyntaxExn("(if true 1)");
        expectSyntaxExn("(if true 1 2 3)");
    }


    //========================================================================


    @Test
    public void testAndOrTailCall()
        throws Exception
    {
        assertEval(true,
                   "(letrec ((is_even (lambda (n)" +
                   "                    (or (= 0 n)" +
                   "                      (is_odd (- n 1)))))" +
                   "         (is_odd (lambda (n)" +
                   "                   (and (not (= 0 n))" +
                   "                     (is_even (- n 1))))))" +
                   "  (is_even " + STACK_OVERFLOW_DEPTH + "0))");
    }


    //========================================================================


    @Test
    public void testNotArity()
        throws Exception
    {
        expectArityExn("(not)");
        expectArityExn("(not true false)");
    }


    @Test
    public void testTruthyNot()
        throws Exception
    {
        for (String form : TRUTHY_EXPRESSIONS)
        {
            assertEval(false, "(not " + form + ")");
        }
    }

    @Test
    public void testUntruthyNot()
        throws Exception
    {
        for (String form : UNTRUTHY_EXPRESSIONS)
        {
            assertEval(true, "(not " + form + ")");
        }
    }


    @Test
    public void testEqual()
        throws Exception
    {
        // integer test
        assertEval(true,  "(= 1 1)");
        assertEval(false, "(= 1 2)");

        // decimal test
        assertEval(true, "(= 1.6666 1.6666)");
        assertEval(false, "(= 1.6667 1.6666)");

        // big int test
        String reallyBigNumber = "8888888888888888888888888888888888888888888888888888888888";
        assertEval(true,  "(= "+reallyBigNumber+" "+reallyBigNumber+")");
        assertEval(false, "(= "+reallyBigNumber+" "+reallyBigNumber+"8)");

        // big dec test
        String reallyBigDec = "8888888888888888888888888888888888888888888888888888888888.88";
        assertEval(true, "(= "+reallyBigDec+" "+reallyBigDec+")");
        assertEval(false, "(= "+reallyBigDec+" "+reallyBigDec+"8)");

        // boolean test
        assertEval(true,  "(= true true)");
        assertEval(false, "(= false true)");

        // string test
        assertEval(true,  "(= \"hello\" \"hello\")");
        assertEval(false, "(= \"hello\" \"world\")");

        // date-and-time timestamp test
        assertEval(true,  "(= 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(false, "(= 2007-08-29T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");

        // date-only timestamp test
        assertEval(true,  "(= 2008-08-28 2008-08-28)");
        assertEval(false, "(= 2008-08-28 2007-08-28)");

        // mixed-state timestamp test
        assertEval(false, "(= 2008-08-28 2007-08-29T16:37:24.0000Z)");
        assertEval(true, "(= 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24Z)");
        assertEval(true, "(= 2008-08-28 2008-08-28T00:00:00.0000Z)");
        assertEval(true, "(= 2007-08-28 2007-08-28T00:00Z)");

    }

    @Test
    public void testLessThan()
        throws Exception
    {
        // int test
        assertEval(false, "(< 2 1)");
        assertEval(false, "(< 1 1)");
        assertEval(true,  "(< 1 2)");

        // decimal test
        assertEval(false, "(< 1.6666 1.6666)");
        assertEval(true,  "(< 1.6666 1.6667)");

        // big int test
        String reallyBigNumber = "8888888888888888888888888888888888888888888888888888888888";
        assertEval(false,  "(< "+reallyBigNumber+" "+reallyBigNumber+")");
        assertEval(true, "(< "+reallyBigNumber+" "+reallyBigNumber+"8)");
        assertEval(false, "(< "+reallyBigNumber+" 1)");

        // big dec test
        String reallyBigDec = "8888888888888888888888888888888888888888888888888888888888.88";
        assertEval(false, "(< "+reallyBigDec+" "+reallyBigDec+")");
        assertEval(true,  "(< "+reallyBigDec+" "+reallyBigDec+"8)");
        assertEval(false, "(< "+reallyBigDec+" 1.02)");

        // date-only timestamp test
        assertEval(true,  "(< 2008-08-28 2008-08-29)");
        assertEval(false, "(< 2008-08-29 2008-08-28)");
        assertEval(false, "(< 2008-08-29 2008-08-29)");

        // date-and-time timestamp test
        assertEval(true,  "(< 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:25.0000Z)");
        assertEval(false, "(< 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(false, "(< 2007-08-28T16:37:25.0000Z 2007-08-28T16:37:24.0000Z)");

        // mixed-state timestamp test
        assertEval(true,  "(< 2008-08-28 2008-08-28T16:37:24.0000Z)");
        assertEval(false, "(< 2007-08-28 2007-08-28T00:00Z)");
        assertEval(false, "(< 2007-08-29 2007-08-28T00:00Z)");
    }

    @Test
    public void testGreaterThan()
            throws Exception
    {
        // int
        assertEval(true,  "(> 2 1)");
        assertEval(false, "(> 1 1)");
        assertEval(false, "(> 1 2)");

        // decimal test
        assertEval(false, "(> 1.6666 1.6666)");
        assertEval(true,  "(> 1.6667 1.6666)");

        // big int test
        String reallyBigNumber = "8888888888888888888888888888888888888888888888888888888888";
        assertEval(false,  "(> "+reallyBigNumber+" "+reallyBigNumber+")");
        assertEval(true, "(> "+reallyBigNumber+"88 "+reallyBigNumber+"8)");

        // big dec test
        String reallyBigDec = "8888888888888888888888888888888888888888888888888888888888.88";
        assertEval(false, "(> "+reallyBigDec+" "+reallyBigDec+")");
        assertEval(true,  "(> "+reallyBigDec+"88 "+reallyBigDec+"8)");
        assertEval(true, "(> "+reallyBigDec+" 1.02)");

        // date-only timestamp test
        assertEval(false, "(> 2008-08-28 2008-08-29)");
        assertEval(true,  "(> 2008-08-29 2008-08-28)");
        assertEval(false, "(> 2008-08-29 2008-08-29)");

        // date-and-time timestamp test
        assertEval(false,  "(> 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:25.0000Z)");
        assertEval(false, "(> 2007-08-28T16:37:24.0000Z 2007-08-28T16:37:24.0000Z)");
        assertEval(true, "(> 2007-08-28T16:37:25.0000Z 2007-08-28T16:37:24.0000Z)");

        // mixed-state timestamp test
        assertEval(false,  "(> 2008-08-28 2008-08-28T16:37:24.0000Z)");
        assertEval(false, "(> 2007-08-28 2007-08-28T00:00Z)");
        assertEval(true, "(> 2007-08-29 2007-08-28T00:00Z)");
    }

    @Test
    public void testOrderingFailures()
        throws Exception
    {
        String [] ops = {"<", "<=", ">=", ">"};
        String reallyBigNumber = "8888888888888888888888888888888888888888888888888888888888";
        String reallyBigDec = "8888888888888888888888888888888888888888888888888888888888.88";

        for (int i = 0; i < ops.length; i++)
        {
            expectContractExn("("+ops[i]+" 1 true)");
            expectContractExn("("+ops[i]+" 1 \"hello\")");
            expectContractExn("("+ops[i]+" 1 2008-08-28)");
            expectContractExn("("+ops[i]+" 1 2007-08-28T16:37:24.0000Z)");
            expectContractExn("("+ops[i]+" 1 (void))");

            expectContractExn("("+ops[i]+" true 1)");
            expectContractExn("("+ops[i]+" true \"hello\")");
            expectContractExn("("+ops[i]+" true 2008-08-28)");
            expectContractExn("("+ops[i]+" true 2007-08-28T16:37:24.0000Z)");
            expectContractExn("("+ops[i]+" true (void))");
            expectContractExn("("+ops[i]+" true 1.667)");
            expectContractExn("("+ops[i]+" true "+reallyBigNumber+")");
            expectContractExn("("+ops[i]+" true "+reallyBigDec+")");

            expectContractExn("("+ops[i]+" \"hello\" 1)");
            expectContractExn("("+ops[i]+" \"hello\" true)");
            expectContractExn("("+ops[i]+" \"hello\" 2008-08-28)");
            expectContractExn("("+ops[i]+" \"hello\" 2007-08-28T16:37:24.0000Z)");
            expectContractExn("("+ops[i]+" \"hello\" (void))");
            expectContractExn("("+ops[i]+" \"hello\" 1.667)");
            expectContractExn("("+ops[i]+" \"hello\" "+reallyBigNumber+")");
            expectContractExn("("+ops[i]+" \"hello\" "+reallyBigDec+")");

            expectContractExn("("+ops[i]+" 2008-08-28 1)");
            expectContractExn("("+ops[i]+" 2008-08-28 \"hello\")");
            expectContractExn("("+ops[i]+" 2008-08-28 true)");
            expectContractExn("("+ops[i]+" 2008-08-28 (void))");
            expectContractExn("("+ops[i]+" 2008-08-28 1.667)");
            expectContractExn("("+ops[i]+" 2008-08-28 "+reallyBigNumber+")");
            expectContractExn("("+ops[i]+" 2008-08-28 "+reallyBigDec+")");

            expectContractExn("("+ops[i]+" 2007-08-28T16:37:24.0000Z true)");
            expectContractExn("("+ops[i]+" 2007-08-28T16:37:24.0000Z 1)");
            expectContractExn("("+ops[i]+" 2007-08-28T16:37:24.0000Z \"hello\")");
            expectContractExn("("+ops[i]+" 2008-08-28T16:37:24.0000Z (void))");
            expectContractExn("("+ops[i]+" 2008-08-28T16:37:24.0000Z 1.667)");
            expectContractExn("("+ops[i]+" 2008-08-28T16:37:24.0000Z "+reallyBigNumber+")");
            expectContractExn("("+ops[i]+" 2008-08-28T16:37:24.0000Z "+reallyBigDec+")");
        }
    }
}
