// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIterator.injectIonIterator;
import static dev.ionfusion.fusion.FusionIterator.injectIterator;
import com.amazon.ion.IonList;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;


public class IteratorTest
    extends CoreTestCase
{
    @Test
    public void testIterateValueFailures()
        throws Exception
    {
        expectArityExn("(value_iterator)");
        expectArityExn("(value_iterator 1 2)");
    }


    @Test
    public void testIteratorAppendFailures()
        throws Exception
    {
        expectArityExn("(iterator_append)");
        expectArityExn("(iterator_append empty_iterator)");
        expectArityExn("(iterator_append empty_iterator empty_iterator empty_iterator)");

        expectContractExn("(iterator_append empty_iterator [])");
        expectContractExn("(iterator_append [] empty_iterator)");
        expectContractExn("(iterator_append empty_iterator null)");
        expectContractExn("(iterator_append null empty_iterator)");
    }


    @Test
    public void testIteratorFilterFailures()
        throws Exception
    {
        expectArityExn("(iterator_choose)");
        expectArityExn("(iterator_choose is_null)");
        expectArityExn("(iterator_choose is_null empty_iterator 1)");

        expectContractExn("(iterator_choose 1 empty_iterator)");
        expectContractExn("(iterator_choose is_null [])");
    }


    @Test
    public void testIteratorMapFailures()
        throws Exception
    {
        expectArityExn("(iterator_map +)");
        expectArityExn("(iterator_map + empty_iterator empty_iterator)");

        eval("(define plus1 (lambda (n) (+ 1 n)))");

        expectContractExn("(iterator_map 1 empty_iterator)");
        expectContractExn("(iterator_map plus1 [])");
    }


    @Test
    public void testIteratorMapSplicingFailures()
        throws Exception
    {
        expectArityExn("(iterator_map_splicing value_iterator)");
        expectArityExn("(iterator_map_splicing value_iterator empty_iterator empty_iterator)");

        expectContractExn("(iterator_map_splicing 1 empty_iterator)");
        expectContractExn("(iterator_map_splicing value_iterator [])");
    }


    //========================================================================
    // Injection APIs

    private static final String ITER_HAS_NEXT = "(iterator_has_next iter)";
    private static final String ITER_NEXT = "(iterator_next iter)";

    @Test
    public void testIteratorInjecting()
        throws Exception
    {
        ArrayList<Object> list = new ArrayList<>();
        list.add("str");
        list.add(12);
        list.add(null);

        TopLevel top = topLevel();
        top.define("iter", injectIterator(null, list.iterator()));
        assertString("str", ITER_NEXT);
        assertEval(12, ITER_NEXT);
        assertVoid(ITER_NEXT);
        assertEval(false, ITER_HAS_NEXT);
    }

    @Test
    public void testIonIteratorInjecting()
        throws Exception
    {
        IonList list = (IonList) system().singleValue("['''str''', 12, null]");

        TopLevel top = topLevel();
        top.define("iter", injectIterator(null, list.iterator()));
        assertEval(true, ITER_HAS_NEXT);
        assertString("str", ITER_NEXT);
        assertEval(12, ITER_NEXT);
        assertEval("null.null", ITER_NEXT);
        assertEval(false, ITER_HAS_NEXT);

        top.define("iter", injectIonIterator(null, list.iterator()));
        assertEval(true, ITER_HAS_NEXT);
        assertString("str", ITER_NEXT);
        assertEval(12, ITER_NEXT);
        assertEval("null.null", ITER_NEXT);
        assertEval(false, ITER_HAS_NEXT);
    }
}
