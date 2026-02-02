// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

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
}
