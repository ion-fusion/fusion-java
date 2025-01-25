// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NumericsTest
    extends CoreTestCase
{
    static final BigInteger VERY_BIG_INTEGER =
        new BigInteger(Long.MAX_VALUE + "123");


    @BeforeEach
    public void requires()
        throws FusionException
    {
        topLevel().requireModule("/fusion/timestamp");
    }

    @Test
    public void testSum()
        throws Exception
    {
        //assertEval(0, "(+)");
        assertBigInt(1, "(+ 1)");
        assertBigInt(2, "(+ 1 1)");
        assertBigInt(40, "(+ 1 19 20)");
        eval("(define ten 10)");
        assertBigInt(20, "(+ ten (+ ten (+ 3 -3)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888889";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(+ "+bigIntStr+" 1)");

        String largerExpectedStr = "17777777777777777777777777777777777777777777777777777777776";
        BigInteger largerExpected = new BigInteger(largerExpectedStr);
        assertEval(largerExpected,"(+ "+bigIntStr+" "+bigIntStr+")");
    }

    @Test
    public void testSumMixed()
        throws Exception
    {
        assertEval("10.0", "(+ 100d-1)");
        assertEval("10.25", "(+ 100d-1 0.25)");
        assertEval("12.25", "(+ 100d-1 0.25 2)");
        assertEval("12.00", "(+ 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("10.50", "(+ quarter (+ quarter (+ 0.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888888.5";
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEval(expected,"(+ "+bigIntStr+" 0.5)");

        // big decimal test suite
        String bigDecStr = "8888888888888888888888888888888888888888888888888888888888.5";
        String expectedStr2 = "8888888888888888888888888888888888888888888888888888888888.25";
        BigDecimal expected2 = new BigDecimal(expectedStr2);
        assertEval(expected2,"(+ "+bigDecStr+" -0.25)");
    }

    @Test
    public void testSumArgType()
        throws Exception
    {
        expectArgumentExn("(+ 10 10e-2)",1);
    }

    @Test
    public void testProduct()
        throws Exception
    {
        assertBigInt(1, "(*)");
        assertBigInt(4, "(* 4)");
        assertBigInt(20, "(* 4 5)");
        assertBigInt(-20, "(* 4 -5)");
        assertBigInt(40, "(* 4 5 2)");
        eval("(define ten 10)");
        assertBigInt(11, "(+ ten (+ ten (* 3 -3)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "17777777777777777777777777777777777777777777777777777777776";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(* "+bigIntStr+" 2)");

        BigInteger expected2 = new BigInteger("-"+bigIntStr);
        assertEval(expected2,"(* "+bigIntStr+" -1)");
    }

    @Test
    public void testProductMixed()
        throws Exception
    {
        assertEval("10.0", "(* 100d-1)");
        assertEval("2.500", "(* 100d-1 0.25)");
        assertEval("5.000", "(* 100d-1 0.25 2)");
        assertEval("-1.250000", "(* 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("1.25000", "(* quarter (* quarter (* 2.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "4444444444444444444444444444444444444444444444444444444444";
        BigDecimal expected = new BigDecimal(expectedStr + ".0");
        assertEval(expected,"(* "+bigIntStr+" 0.5)");
    }

    @Test
    public void testProductArgType()
        throws Exception
    {
        expectArgumentExn("(* 10 10e-2)",1);
    }

    @Test
    public void testDifference()
        throws Exception
    {
        assertBigInt(-1, "(- 1)");
        assertBigInt(-1, "(- 3 4)");
        assertBigInt(-6, "(- 3 4 5)");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "4444444444444444444444444444444444444444444444444444444444";
        BigInteger expected = new BigInteger(expectedStr);
        assertEval(expected,"(- "+bigIntStr+" "+expectedStr+")");

        BigInteger expected2 = new BigInteger("-"+expectedStr);
        assertEval(expected2,"(- "+bigIntStr+" "+expectedStr+" "+expectedStr+" "+expectedStr+")");
    }

    @Test
    public void testDifferenceMixed()
        throws Exception
    {
        //assertEval("-10.0", "(- 100d-1)");
        //assertEval("9.75", "(- 100d-1 0.25)");
        assertEval("7.75", "(- 100d-1 0.25 2)");
        assertEval("8.00", "(- 100d-1 0.25 2.0 -0.25)");
        eval("(define quarter 0.25)");
        assertEval("-7.99", "(- 0.26 (- quarter (- 2.0 10)))");

        // big integer test suite
        String bigIntStr = "8888888888888888888888888888888888888888888888888888888888";
        String expectedStr = "8888888888888888888888888888888888888888888888888888888887.75";
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEval(expected,"(- "+bigIntStr+" 0.25)");
    }

    @Test
    public void testDifferenceNoArgs()
        throws Exception
    {
        expectArityExn("(-)");
    }

    @Test
    public void testDifferenceArgType()
        throws Exception
    {
        expectArgumentExn("(- 10 10e-2)",1);
    }


    //=========================================================================

    private static final class IsMaxLongProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args) throws FusionException
        {
            long l = checkIntArgToJavaLong(eval, this, 0, args);
            return FusionBool.makeBool(eval, l == Long.MAX_VALUE);
        }
    }

    /**
     * Regression test; longs were getting truncated by
     * {@link FusionNumber#checkIntArgToJavaLong(Evaluator, Procedure, int, Object...)}.
     */
    @Test
    public void testArgToLong()
        throws Exception
    {
        topLevel().define("is_max_long", new IsMaxLongProc());
        assertEval(true, "(is_max_long " + Long.MAX_VALUE + ")");
    }
}
