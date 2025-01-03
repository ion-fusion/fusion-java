// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.TailCallTest.STACK_OVERFLOW_DEPTH;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ApplyTest
    extends CoreTestCase
{
    @Test
    public void testApply()
        throws Exception
    {
        assertEval(0, "(apply + [])");
        assertEval(1, "(apply + [1])");
        assertEval(3, "(apply + [1, 2])");

        assertEval(10, "(apply + 10 [])");
        assertEval(22, "(apply + 10 11 [1])");
        assertEval(13, "(apply + 10 [1, 2])");

        assertEval(0, "(apply + (quote ()))");
        assertEval(1, "(apply + (quote (1)))");
        assertEval(3, "(apply + (quote (1 2)))");

        assertEval(10, "(apply apply + 1 [2, [3, 4]])");
    }

    @Test
    public void testApplyArity()
        throws Exception
    {
        expectArityExn("(apply)");
        expectArityExn("(apply +)");
    }

    @Test
    public void testApplyBadProc()
        throws Exception
    {
        expectContractExn("(apply 12 [])");
    }

    @Test
    public void testApplyBadRest()
        throws Exception
    {
        expectContractExn("(apply + 12)");
        expectContractExn("(apply + 12 13 {})");
    }

    @Test
    public void testTailCall()
        throws Exception
    {
        eval("(define countup" +
            "  (lambda (i limit)" +
            "    (if (= i limit) i" +
            "      (apply countup [(+ 1 i), limit]))))");

       assertEval(STACK_OVERFLOW_DEPTH * 100,
                  "(countup 0 " + STACK_OVERFLOW_DEPTH * 100 + ")");
    }
}
