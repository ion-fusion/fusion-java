// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.empty;
import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.fromArrays;
import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.fromEntries;
import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.fromSelectedKeys;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import dev.ionfusion.fusion.util.hamt.TransformTestCase.Remove3sIncrement2sXform;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MultiHashTrieImplTest
    extends MultiHashTrieTestCase
{
    @Override
    MultiHashTrie simpleSubject()
    {
        return multi(1,1, 2,2, 2,20);
    }


    //=========================================================================
    // Creation

    // fromEntries()

    @Test
    public void fromEntriesGivenEmptyIteratorReturnsEmpty()
    {
        MultiHashTrie h = fromEntries(Collections.<Entry<Object,Object>>emptyIterator());
        expectEmpty(h);
    }

    @Test
    public void fromEntriesGivenRepeatedKeyUsesBothValues()
    {
        Object[] array = {2, 6};
        Iterator i = iterate(entry(1, 1),
                             entry(3, 3),
                             entry(3, 6),
                             entry(1, array));

        MultiHashTrie h = fromEntries(i);
        assertThat(h, is(multi(1, array, 1, 1, 3, 3, 3, 6)));
    }


    // fromArrays()

    @Test
    public void fromArraysGivenEmptyArraysReturnsEmpty()
    {
        MultiHashTrie t = fromArrays(new Object[0], new Object[0]);
        expectEmpty(t);
    }

    @Test
    public void fromArraysRequiresEqualLengthArrays()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> fromArrays(new Object[]{ 1    },
                                      new Object[]{ 1, 2 }));
    }

    @Test
    public void fromArraysGivenRepeatedKeyReturnsAll()
    {
        MultiHashTrie t = fromArrays(new Object[] { 1, 2, 1, 1 },
                                     new Object[] { 3, 4, 5, 3 });
        assertThat(t, is(multi(1,3, 1,3, 1,5, 2,4)));

        t = fromArrays(new Object[] { 5, 6, 5 },
                       new Object[] { 8, 9, 0 });
        assertThat(t, is(multi(5, 8, 6, 9, 5, 0)));
    }


    // fromSelectedKeys()

    @Test
    public void fromSelectedKeysReturnsSubset()
    {
        MultiHashTrie origin = multi(1, 1,  2, 2,  2, 4,  3, 3);

        MultiHashTrie t = fromSelectedKeys(origin, 3, 2);

        assertThat(t, is(multi(2, 2, 2, 4, 3, 3)));
    }

    @Test
    public void fromSelectedKeysReturnsEmptySingleton()
    {
        MultiHashTrie origin = multi(1, 1, 2, 2, 1, 3);

        MultiHashTrie t = fromSelectedKeys(origin);
        expectEmpty(t);

        t = fromSelectedKeys(origin, 3, 4);
        expectEmpty(t);
    }


    //=========================================================================
    // Inspection
    //   These are exercised extensively via hash() and other fixture methods.

    @Test
    public void getMultiResultRejectsWriteThrough()
    {
        MultiHashTrie s = multi(1, 1, 1, 2, 1, 3);
        Collection values = s.getMulti(1);

        assertThrows(UnsupportedOperationException.class,
                     () -> values.add(4));
    }


    //=========================================================================
    // Modification

    // with1()

    @Test
    public void with1ReplacesExistingValues()
    {
        expectMapping(simpleSubject().with1(1, 3), 1, 3);
    }

    @Test
    public void with1GivenSameValueReturnsSelf()
    {
        MultiHashTrie s = simpleSubject();
        assertThat(s.with1(1, s.get(1)), sameInstance(s));
    }


    // withMulti()

    @Test
    public void withMultiRepeatsKeys()
    {
        MultiHashTrie s = empty().withMulti(1, 1).withMulti(1, 2).withMulti(1, 1);
        expectMapping(s, 1, 1, 2, 1);
    }


    // withoutKey()

    @Test
    public void withoutKeyRemovesSingleValue()
    {
        MultiHashTrie s = multi(1, 1, 2, 2, 2, 4, 3, 3);
        MultiHashTrie r = s.withoutKey(3);
        assertThat(r, is(multi(1, 1, 2, 2, 2, 4)));
        assertThat(r, not(s));
    }

    @Test
    public void withoutKeyRemovesMultiValue()
    {
        MultiHashTrie s = multi(1, 1, 2, 2, 2, 4, 3, 3);
        MultiHashTrie r = s.withoutKey(2);
        assertThat(r, is((MultiHashTrie) hash1(1, 1, 3, 3)));
        assertThat(r, not(s));
    }


    // withoutKeys()

    @Test
    public void withoutKeysGivenSomeKeysReturnsSubset()
    {
        MultiHashTrie s = multi(1, 1, 2, 2, 2, 4, 3, 3);
        MultiHashTrie r = s.withoutKeys(1, 3, 5);

        assertThat(r, is(multi(2, 2, 2, 4)));
        assertThat(r, not(s));
    }


    // merge1()

    @Test
    public void merge1GivenEmptyReturnsOneified()
    {
        MultiHashTrie s = multi(10,1, 11,1, 12,1, 20,2, 20,2, 21,2, 21,2, 22,2, 22,2);
        MultiHashTrie r = s.merge1(empty());
        assertThat(r, is((MultiHashTrie) s.oneify()));
    }

    @Test
    public void merge1GivenValidInputReturnsSingleHash()
    {
        MultiHashTrie h1 = multi(10,1, 11,1, 12,1, 20,2, 20,2, 21,2, 21,2, 22,2, 22,2);
        MultiHashTrie h2 = multi( 1,5, 2,6, 2,6, 11,5, 12,6, 12,6, 21,5, 22,6, 22,6);

        FunctionalHashTrie r = h1.merge1(h2);
        assertThat(r, is(hash1(1,5, 2,6, 10,1, 11,5, 12,6, 20,2, 21,5, 22,6)));
    }


    // mergeMulti()

    @Test
    public void mergeMultiGivenValidInputReturnsUnion()
    {
        MultiHashTrie h1 = multi(10,1, 11,1, 12,1, 20,2, 20,2, 21,2, 21,2, 22,2, 22,2);
        MultiHashTrie h2 = multi( 1,5, 2,6, 2,6, 11,5, 12,6, 12,6, 21,5, 22,2, 22,6, 22,6);

        MultiHashTrie r = mergeMulti(h1, h2);
        assertThat(r, is(multi(1,5, 2,6, 2,6, 10,1, 11,1, 11,5, 12,1, 12,6, 12,6,
                               20,2, 20,2, 21,2, 21,2, 21,5, 22,2, 22,2, 22,2, 22,6, 22,6)));
    }


    // transform()

    @Test
    public void transformReturnsEmpty()
    {
        MultiHashTrie s = multi(1,3, 3,3, 4,6, 4,9, 5,15, 5,15);
        expectEmpty(s.transform(new Remove3sIncrement2sXform()));
    }

    @Test
    public void transformGivenNoopReturnsSame()
    {
        MultiHashTrie s = multi(1,1, 2,1, 4,5, 4,5, 4,7);
        assertThat(s.transform(new Remove3sIncrement2sXform()), sameInstance(s));
    }

    @Test
    public void transformMixedChanges()
    {
        MultiHashTrie h = multi(1,1, 2,2, 3,3,
                                4,1, 4,2, 4,2, 4,3,
                                5,1, 5,3, 5,2,
                                6,2, 6,3, 6,1,
                                7,3, 7,6, 7,1,
                                8,3, 8,6);
        assertThat(h.transform(new Remove3sIncrement2sXform()),
                   is(multi(1,1, 2,3,
                            4,1, 4,3, 4,3,
                            5,1, 5,3,
                            6,1, 6,3,
                            7,1)));
    }


    // oneify()

    @Test
    public void oneifyBehavesNormally()
    {
        checkOneify(multi(1, 1, 1, 1, 1, 1));
        checkOneify(multi(1, 1, 2, 2, 2, 3, 4, 4));
        checkOneify(multi(1, 1, 2, 2, 2, 3, 4, 4, 4, 9));
        checkOneify(multi(10,1, 11,1, 12,1, 20,2, 20,2, 21,2, 21,2, 22,2, 22,2));
    }
}
