// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.fail;

import dev.ionfusion.runtime.base.SourceLocation;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StackTraceTest
    extends CoreTestCase
{
    private Iterator<SourceLocation> myStack;


    private void evalForTrace(String code)
        throws Exception
    {
        FusionException e = assertEvalThrows(FusionException.class, code);
        List<SourceLocation> locations = e.getContext();
        if (locations.isEmpty())
        {
            // This is probably a parsing error, since the evaluator will
            // otherwise ensure a topmost locations.
            throw e;
        }
        myStack = locations.iterator();
    }


    private SourceLocation popLocation()
    {
        SourceLocation loc = myStack.next();

        // Ignore library code, which all has SourceName attached.
        while (loc.getSourceName() != null)
        {
            loc = myStack.next();
        }
        return loc;
    }


    private void expectLocation(int line, int column)
    {
        SourceLocation loc = popLocation();
        if (line != loc.getLine() || column != loc.getColumn())
        {
            fail("Expected L" + line +" C" + column
                     + " found L" + loc.getLine() + " C" + loc.getColumn());
        }
    }



    //========================================================================

    // Testing `let_values` is a bit more thorough since it compiles differently
    // when all the binding-pairs introduce a single binding, in which case
    // it's compiled like `let`.

    @Test
    public void testLetValues1()
        throws Exception
    {
        // These examples compile like `let`:
        evalForTrace("(let_values [((a) (first 1))] a)");
        expectLocation(1, 19);
        expectLocation(1, 1);

        // These examples do not compile like `let`:
        evalForTrace("(let_values [((a x) (first 1))] a)");
        expectLocation(1, 21);
        expectLocation(1, 1);
    }

    @Test
    public void testLetValues2()
        throws Exception
    {
        // These examples compile like `let`:
        evalForTrace("(let_values [((a)  (first 1)),\n" +
                     "             ((b) 2)]\n" +
                     "  a)");
        expectLocation(1, 20);
        expectLocation(1, 1);

        evalForTrace("(let_values [((a)  1),\n" +
                     "             ((b) (first 2))]\n" +
                     "  a)");
        expectLocation(2, 19);
        expectLocation(1, 1);

        // These examples do not compile like `let`:
        evalForTrace("(let_values [((a x) (first 1)),\n" +
                     "             ((b)   2)]\n" +
                     "  a)");
        expectLocation(1, 21);  // TODO how dow this work?
        expectLocation(1, 1);

        evalForTrace("(let_values [((a x) (values 1 2)),\n" +
                     "             ((b)   (first 1))]\n" +
                     "  a)");
        expectLocation(2, 21);
        expectLocation(1, 1);
    }

    @Test
    public void testLetValues3()
        throws Exception
    {
        // These examples compile like `let`:
        evalForTrace("(let_values [((a) (first 1)),\n" +
                     "             ((b) 2),\n" +
                     "             ((c) 3)]\n" +
                     "  a)");
        expectLocation(1, 19);
        expectLocation(1, 1);

        evalForTrace("(let_values [((a) 1),\n" +
                     "             ((b)   (first 1)),\n" +
                     "             ((c) 3)]\n" +
                     "  a)");
        expectLocation(2, 21);
        expectLocation(1, 1);

        evalForTrace("(let_values [((a) 1),\n" +
                     "             ((b) 2),\n" +
                     "             ((c)    (first 1))]\n" +
                     "  a)");
        expectLocation(3, 22);
        expectLocation(1, 1);

        // These examples do not compile like `let`:
        evalForTrace("(let_values [((a) (first 1)),\n" +
                     "             ((b x) (values 2 2)),\n" +
                     "             ((c) 3)]\n" +
                     "  a)");
        expectLocation(1, 19);
        expectLocation(1, 1);

        evalForTrace("(let_values [((a x) (values 1 1)),\n" +
                     "             ((b)   (first 1)),\n" +
                     "             ((c) 3)]\n" +
                     "  a)");
        expectLocation(2, 21);
        expectLocation(1, 1);

        evalForTrace("(let_values [((a x) (values 1 1)),\n" +
                     "             ((b) 2),\n" +
                     "             ((c)    (first 1))]\n" +
                     "  a)");
        expectLocation(3, 22);
        expectLocation(1, 1);
    }


    //========================================================================


    @Test
    public void testLet1()
        throws Exception
    {
        evalForTrace("(let [(a (first 1))] a)");
        expectLocation(1, 10);
        expectLocation(1, 1);

        evalForTrace("(let loop\n" +
                     "   [(a (first 1))] a)");
        expectLocation(2, 8);
        expectLocation(1, 1);
    }

    @Test
    public void testLet2()
        throws Exception
    {
        evalForTrace("(let [(a  (first 1)),\n" +
                     "      (b 2)]\n" +
                     "  a)");
        expectLocation(1, 11);
        expectLocation(1, 1);

        evalForTrace("(let [(a 1),\n" +
                     "      (b   (first 1))]\n" +
                     "  a)");
        expectLocation(2, 12);
        expectLocation(1, 1);
    }

    @Test
    public void testLet3()
        throws Exception
    {
        evalForTrace("(let [(a (first 1)),\n" +
                     "      (b 2),\n" +
                     "      (c 3)]\n" +
                     "  a)");
        expectLocation(1, 10);
        expectLocation(1, 1);

        evalForTrace("(let [(a 1),\n" +
                     "      (b   (first 1)),\n" +
                     "      (c 3)]\n" +
                     "  a)");
        expectLocation(2, 12);
        expectLocation(1, 1);

        evalForTrace("(let [(a 1),\n" +
                     "      (b 2),\n" +
                     "      (c    (first 1))]\n" +
                     "  a)");
        expectLocation(3, 13);
        expectLocation(1, 1);
    }


    //========================================================================


    @Test
    public void testLets1()
        throws Exception
    {
        evalForTrace("(lets [(a (first 1))] a)");
        expectLocation(1, 11);
        expectLocation(1, 1);
    }

    @Test
    public void testLets2()
        throws Exception
    {
        evalForTrace("(lets [(a  (first 1)),\n" +
                     "       (b 2)]\n" +
                     "  a)");
        expectLocation(1, 12);
        expectLocation(1, 1);

        evalForTrace("(lets [(a 1),\n" +
                     "       (b   (first 1))]\n" +
                     "  a)");
        expectLocation(2, 13);
        expectLocation(1, 1);
    }

    @Test
    public void testLets3()
        throws Exception
    {
        evalForTrace("(lets [(a (first 1)),\n" +
                     "       (b 2),\n" +
                     "       (c 3)]\n" +
                     "  a)");
        expectLocation(1, 11);
        expectLocation(1, 1);

        evalForTrace("(lets [(a 1),\n" +
                     "       (b   (first 1)),\n" +
                     "       (c 3)]\n" +
                     "  a)");
        expectLocation(2, 13);
        expectLocation(1, 1);

        evalForTrace("(lets [(a 1),\n" +
                     "       (b 2),\n" +
                     "       (c    (first 1))]\n" +
                     "  a)");
        expectLocation(3, 14);
        expectLocation(1, 1);
    }


    //========================================================================


    @Test
    public void testLetrec1()
        throws Exception
    {
        evalForTrace("(letrec [(a (first 1))] a)");
        expectLocation(1, 13);
        expectLocation(1, 1);
    }

    @Test
    public void testLetrec2()
        throws Exception
    {
        evalForTrace("(letrec [(a  (first 1)),\n" +
                     "         (b 2)]\n" +
                     "  a)");
        expectLocation(1, 14);
        expectLocation(1, 1);

        evalForTrace("(letrec [(a 1),\n" +
                     "         (b   (first 1))]\n" +
                     "  a)");
        expectLocation(2, 15);
        expectLocation(1, 1);
    }

    @Test
    public void testLetrec3()
        throws Exception
    {
        evalForTrace("(letrec [(a (first 1)),\n" +
                     "         (b 2),\n" +
                     "         (c 3)]\n" +
                     "  a)");
        expectLocation(1, 13);
        expectLocation(1, 1);

        evalForTrace("(letrec [(a 1),\n" +
                     "         (b   (first 1)),\n" +
                     "         (c 3)]\n" +
                     "  a)");
        expectLocation(2, 15);
        expectLocation(1, 1);

        evalForTrace("(letrec [(a 1),\n" +
                     "         (b 2),\n" +
                     "         (c    (first 1))]\n" +
                     "  a)");
        expectLocation(3, 16);
        expectLocation(1, 1);
    }
}
