// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import dev.ionfusion.fusion.util.hamt.TransformTestCase.IncrementXform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EmptyHashTrieTest
    extends MultiHashTrieTestCase
{
    @Override
    MultiHashTrie simpleSubject()
    {
        return empty();
    }


    //=========================================================================
    // Modification


    // withoutKey()

    @Test
    public void withoutKeyReturnsSingleton()
    {
        expectEmpty(empty().withoutKey("absent"));
    }


    // withoutKeys()

    @Test @Disabled
    @Override
    public void withoutKeysRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withoutKeys(1, null, 3));
    }

    @Test
    public void withoutKeysReturnsSingleton()
    {
        expectEmpty(empty().withoutKeys("absent", "missing", -1));
    }


    // merge1()

    @Test
    public void merge1GivenSingleValuesReturnsIt()
    {
        FunctionalHashTrie h = hash1(10,1, 11,1, 12,1);
        assertThat(empty().merge1(h), sameInstance(h));
    }

    @Test
    public void merge1GivenMultiValuesReturnsOneified()
    {
        MultiHashTrie h = hash(10,1, 11,1, 12,1, 20,2, 20,2, 21,2, 21,2, 22,2, 22,2);
        assertThat(empty().merge1(h), is((MultiHashTrie) h.oneify()));
    }


    // mergeMulti()

    @Test
    public void mergeMultiGivenEmptyReturnsSingleton()
    {
        expectEmpty(mergeMulti(empty(), empty()));
    }

    @Test
    public void mergeMultiGivenSingleValuesReturnsArgument()
    {
        MultiHashTrie h = hash1(1,1, 2,2, 3,3);
        assertThat(mergeMulti(empty(), h), sameInstance(h));
    }

    @Test
    public void mergeMultiGivenMultiValuesReturnsArgument()
    {
        MultiHashTrie h = multi(1,1, 2,2, 2,2);
        assertThat(mergeMulti(empty(), h), sameInstance(h));
    }


    // transform()

    @Test
    public void transformReturnsSingleton()
    {
        expectEmpty(empty().transform(new IncrementXform()));
    }


    // oneify()

    @Test
    public void oneifyReturnsSingleton()
    {
        expectEmpty(empty().oneify());
    }
}
