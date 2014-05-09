// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionList.unsafeListElement;
import static com.amazon.fusion.FusionList.unsafeListSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import com.amazon.ion.IonList;
import com.amazon.ion.IonValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ListTest
    extends CoreTestCase
{
    @Before
    public void listTestBegin()
        throws Exception
    {
        topLevel().requireModule("/fusion/list");

        eval("(define smList "+ionListGenerator(1)+")");
        eval("(define medList "+ionListGenerator(3)+")");
        eval("(define lgList "+ionListGenerator(8888)+")");
    }


    //========================================================================
    // Adding and removing elements


    /** Tests interaction with IonValues. */
    @Test
    public void testAddToIonList()
        throws Exception
    {
        IonList list = (IonList) system().singleValue("[0, sym]");
        Object result = topLevel().call("add", list, 2);
        assertEquals(2, list.size());
        assertEquals(3, unsafeListSize(null, result));
        checkIon(list.get(0), unsafeListElement(null, result, 0));
        checkIon(list.get(1), unsafeListElement(null, result, 1));
    }


    /** Tests interaction with IonValues. */
    @Test
    public void testAddMToIonList()
        throws Exception
    {
        IonList list = (IonList) system().singleValue("[0, sym]");
        Object result = topLevel().call("add_m", list, 2);
        assertEquals(2, list.size());
        assertEquals(3, unsafeListSize(null, result));
        checkIon(list.get(0), unsafeListElement(null, result, 0));
        checkIon(list.get(1), unsafeListElement(null, result, 1));
    }


    @Test
    public void testAdd()
        throws Exception
    {
        assertEval("[1]", "(add null.list 1)");
        assertEval("[1]", "(add [] 1)");

        assertEval("(1)", "(add (quote null.sexp) 1)");
        assertEval("(1)", "(add (quote ()) 1)");
    }

    @Test
    public void testMutationOfConstants()
        throws Exception
    {
        eval("(define nl (lambda () null.list))");
        assertEval("[1]", "(add (nl) 1)");
        assertEval("[2]", "(add (nl) 2)");

        eval("(define l (lambda () [1]))");
        assertEval("[1, 2]", "(add (l) 2)");
        assertEval("[1, 3]", "(add (l) 3)");
    }

    @Test
    public void testAddAnyIon()
        throws Exception
    {
        for (String form : allIonExpressions())
        {
            String expr = "(add [] " + form + ")";
            IonList result = (IonList) evalToIon(expr);
            Assert.assertEquals(1, result.size());
        }
    }

    @Test
    public void testDeepAdd()
        throws Exception
    {
        assertEval("{f:[2]}",
                   "(let ((s {f:(stretchy_list)}))" +
                   "  (add_m (. s \"f\") 2)" +
                   "  s)");
        // Can't do the same thing with sexp since add_m returns a new pair.
    }

    @Test
    public void testAddArgType()
        throws Exception
    {
        for (String form : nonSequenceExpressions())
        {
            String expr = "(add " + form + " 12)";
            expectArgumentExn(expr, 0);
        }
    }


    //========================================================================


    @Test
    public void testSize()
        throws Exception
    {
        assertEval(0, "(size null.list)");
        assertEval(0, "(size [])");
        assertEval(1, "(size [1])");
        assertEval(2, "(size [2,2])");

        assertEval(0, "(size (quote null.sexp))");
        assertEval(0, "(size (quote ()))");
        assertEval(1, "(size (quote (1)))");
        assertEval(2, "(size (quote (2 2)))");
    }


    @Test
    public void testSizeArity()
        throws Exception
    {
        expectArityExn("(size)");
        expectArityExn("(size [] [])");
    }

    @Test
    public void testSizeArgType()
        throws Exception
    {
        for (String form : nonContainerExpressions())
        {
            String expr = "(size " + form + ")";
            expectArgumentExn(expr, 0);
        }
    }

    public String ionListGeneratorWithOffset(int length, int offset)
    {
        StringBuilder resultStr = new StringBuilder("[");

        for (int i = 0; i < length; i++)
        {
            resultStr.append(Integer.toString(i+offset));
            if (i != length-1)
            {
                resultStr.append(",");
            }

        }

        resultStr.append("]");

        return resultStr.toString();
    }

    public String ionListGenerator(int length)
    {
        return ionListGeneratorWithOffset(length, 0);
    }


    @Test
    public void testIsList()
        throws Exception
    {
        assertEval(true, "(is_list [])");
        assertEval(true, "(is_list [1,3])");
        assertEval(true, "(is_list [[[]],[3,3]])");
        assertEval(true, "(is_list [{a:3},{a:3}])");

        assertEval(false, "(is_list (void))");
        assertEval(false, "(is_list {})");
        assertEval(false, "(is_list \"[1,2,3]\")");
    }


    //========================================================================
    // Append

    /** Tests interaction with IonValues. */
    @Test
    public void testAppendM()
        throws Exception
    {
        Object fList = eval("(stretchy_list 1)");
        IonList iList = (IonList) system().singleValue("[2, sym, true]");
        Object result = topLevel().call("append_m", fList, iList);
        assertSame(fList, result);
        assertEquals(4, unsafeListSize(null, result));

        // Elements should be lazily fusion-ized
        assertSame(iList.get(1), topLevel().call("list_element", result, 2));

        // Now mutate the IonValue
        fList = eval("(stretchy_list 4)");
        result = topLevel().call("append_m", iList, fList);
        assertEquals(4, unsafeListSize(null, result));

        checkIon(iList.get(0), unsafeListElement(null, result, 0));
        checkIon(iList.get(1), unsafeListElement(null, result, 1));
        checkIon(iList.get(2), unsafeListElement(null, result, 2));
        assertSame(unsafeListElement(null, fList, 0),
                   unsafeListElement(null, result, 3));
    }


    /** Traps a crash caused by append_m not injecting its varargs lists. */
    @Test
    public void testLazyInjectionFailure()
        throws Exception
    {
        IonValue boom =
            system().singleValue("[ [ { value:1 } ], [ { value:2 } ] ]");

        topLevel().define("$t", boom);

        assertEval(2, "(. (apply append_m $t) 1 \"value\")");
    }


    //========================================================================
    // List iteration

    @Test
    public void testInvalidIteration()
        throws Exception
    {
        expectContractExn("(list_iterator 3)");
        expectContractExn("(list_from_iterator 3)");
        expectContractExn("(list_from_iterator [2,3])");
    }


    @Test()
    public void testIterationArityCheck()
        throws Exception
    {
        expectArityExn("(list_iterator)");
        expectArityExn("(list_iterator [] 1)");

        expectArityExn("(list_from_iterator)");
        expectArityExn("(list_from_iterator empty_iterator 1)");
    }
}
