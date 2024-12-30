// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.empty;
import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.fromArrays;
import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.fromEntries;
import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.fromMap;
import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.fromSelectedKeys;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class FunctionalHashTrieTest
    extends MultiHashTrieTestCase
{
    private HashMap<Object, Object>            baselineMap;
    private FunctionalHashTrie<Object, Object> fht;

    public void setup(int size)
    {
        baselineMap = new HashMap<>();
        for (int i = 0; i < size; i++)
        {
            baselineMap.put(new Object(), new Object());
        }
        fht = fromMap(baselineMap);
    }

    private void doRemoval()
    {
        Object[] keys = baselineMap.keySet().toArray();
        Set<Object> keysToRemove = new LinkedHashSet<>(keys.length / 2);
        for (int i = 0; i < keys.length / 2; i++)
        {
            keysToRemove.add(keys[i]);
        }

        baselineMap.keySet().removeAll(keysToRemove);

        for (Object key : keysToRemove)
        {
            FunctionalHashTrie<Object, Object> newHash = fht.withoutKey(key);
            assertNotEquals(fht, newHash);
            fht = newHash;
        }
    }

    private void doAddition()
    {
        int toAdd = baselineMap.size();
        for (int i = 0; i < toAdd; i++)
        {
            Object key   = new Object();
            Object value = new Object();

            baselineMap.put(key, value);
            FunctionalHashTrie<Object, Object> newHash = fht.with1(key, value);
            assertNotEquals(fht, newHash);
            fht = newHash;
        }
    }

    private void checkSizing()
    {
        assertEquals(baselineMap.size(), fht.size());
    }

    private void checkKeys()
    {
        Set<Object> keySet = fht.keySet();
        Set<Object> baselineSet = baselineMap.keySet();
        assertEquals(baselineSet, keySet);
    }

    private void compareEntries()
    {
        Map<Object, Object> remainder = (Map<Object, Object>) baselineMap.clone();

        for (Map.Entry<Object, Object> entry : fht)
        {
            assertEquals(entry.getValue(), remainder.remove(entry.getKey()));
        }

        assertEquals(0, remainder.size(), "elements remaining");
    }

    private void compareWithBaseline()
    {
        checkSizing();
        checkKeys();
        compareEntries();
    }

    private void performTests()
    {
        compareWithBaseline();
        doRemoval();
        compareWithBaseline();
        doAddition();
        compareWithBaseline();
    }

    @Test
    public void checkEmpty()
    {
        setup(0);
        assertTrue(fht.isEmpty());
        assertSame(empty(), fht);
        performTests();

        FunctionalHashTrie without = fht.withoutKey("anything");
        assertSame(without, fht);
        assertSame(empty(), fht);
    }

    @Test
    public void checkSingle()
    {
        setup(1);
        performTests();
    }

    @Test
    public void checkSmall()
    {
        setup(10);
        performTests();
    }

    @Test
    public void checkVeryLarge()
    {
        setup(10000);
        performTests();
    }

    @Test
    public void checkVeryVeryLarge()
    {
        setup(1000000);
        performTests();
    }


    //=========================================================================
    // Expectations and fixture setup

    @Override
    FunctionalHashTrie simpleSubject()
    {
        return hash1(1, 1);
    }


    //=========================================================================
    // Creation

    @Test
    public void compareMutableCreateVsSequential()
    {
        setup(10000);

        FunctionalHashTrie seq = empty();
        for (Map.Entry e : fht)
        {
            seq = seq.with1(e.getKey(), e.getValue());
        }

        assertEquals(fht, seq);
        assertEquals(fht.size(), seq.size());
        for (Map.Entry e : fht)
        {
            Object key = e.getKey();
            assertEquals(fht.get(key), seq.get(key));
        }
    }


    // fromMap()

    @Test
    public void fromMapGivenEmptyMapReturnsEmptySingleton()
    {
        FunctionalHashTrie h = fromMap(Collections.emptyMap());
        expectEmpty(h);
    }

    @Test
    public void fromMapGivenValidInputReturnsHash()
    {
        Map m = new HashMap();
        m.put(1, 1);
        m.put(2, 2);
        m.put(3, 3);

        FunctionalHashTrie h = fromMap(m);
        assertThat(h, is(hash1(1, 1, 2, 2, 3, 3)));
    }

    @Test
    public void fromMapGivenArrayValuesReturnsHash()
    {
        Object[] arrayValue = {2, 6};

        Map m = new HashMap();
        m.put(1, 1);
        m.put(2, arrayValue);
        m.put(3, 3);

        FunctionalHashTrie h = MultiHashTrie.fromMap(m);
        assertThat(h, is(hash1(1, 1, 2, arrayValue, 3, 3)));
    }


    // fromEntries()

    @Test
    public void fromEntriesGivenEmptyIteratorReturnsEmptySingleton()
    {
        FunctionalHashTrie h = fromEntries(Collections.<Map.Entry<Object,Object>>emptyIterator());
        expectEmpty(h);
    }
    @Test
    public void fromEntriesGivenRepeatedKeyUsesLastValue()
    {
        Object[] array = {2, 6};
        Iterator i = iterate(entry(1, 1),
                             entry(3, 3),
                             entry(3, 6),
                             entry(1, array));

        FunctionalHashTrie h = fromEntries(i);
        assertThat(h, is(hash1(1, array, 3, 6)));
    }


    // fromArrays()

    @Test
    public void fromArraysGivenEmptyArraysReturnsEmptySingleton()
    {
        FunctionalHashTrie t = fromArrays(new Object[0], new Object[0]);
        expectEmpty(t);
    }

    @Test
    public void fromArraysRequiresEqualLengthArrays()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> fromArrays(new Object[]{1}, new Object[]{1, 2}));
    }

    @Test
    public void fromArraysGivenUniqueKeysReturnsAll()
    {
        FunctionalHashTrie t = fromArrays(new Object[] { 1, 2 },
                                          new Object[] { 3, 4 });
        assertThat(t, is(hash1(1, 3, 2, 4)));
    }

    @Test
    public void fromArraysGivenRepeatedKeyUsesLastValue()
    {
        FunctionalHashTrie t = fromArrays(new Object[] { 5, 6, 5 },
                                          new Object[] { 8, 9, 0 });
        assertThat(t, is(hash1(5, 0, 6, 9)));
    }


    // fromSelectedKeys()

    @Test
    public void fromSelectedKeysReturnsSubset()
    {
        FunctionalHashTrie origin = hash1(1, 1, 2, 2, 3, 3);

        FunctionalHashTrie t = fromSelectedKeys(origin, new Object[]{ 1, 3, 5 });

        assertThat(t, is(hash1(1, 1, 3, 3)));
    }

    @Test
    public void fromSelectedKeysReturnsEmptySingleton()
    {
        FunctionalHashTrie origin = hash1(1, 1, 2, 2);

        FunctionalHashTrie t = fromSelectedKeys(origin, new Object[]{});
        expectEmpty(t);

        t = fromSelectedKeys(origin, new Object[]{ 3, 4 });
        expectEmpty(t);
    }


    //=========================================================================
    // Inspection
    //   These are exercised extensively via hash() and other fixture methods.

    @Test
    public void getMultiResultRejectsWriteThrough()
    {
        MultiHashTrie s = hash1(1, 1, 2, 2, 3, 3);
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
        FunctionalHashTrie s = simpleSubject();
        assertThat(s.with1(1, s.get(1)), sameInstance(s));
    }


    // withMulti()
    // Expected behavior is tested through {@link #hash}.

    @Test
    public void withMultiGivenNewKeyReturnsSingleHash()
    {
        FunctionalHashTrie s = hash1(1, 1);
        FunctionalHashTrie r = (FunctionalHashTrie) s.withMulti(2, 2);
        assertThat(r, is(hash1(1,1, 2,2)));
    }

    @Test
    public void withMultiGivenExistingKeyReturnsMultiHash()
    {
        FunctionalHashTrie s = hash1(1, 1);
        MultiHashTrie r = s.withMulti(1, 1);
        assertInstanceOf(MultiHashTrieImpl.class, r);
        assertThat(r, is(multi(1,1, 1,1)));
    }


    // withoutKey()

    @Test
    public void withoutKeyRemovesSingleValue()
    {
        FunctionalHashTrie subject = hash1(1, 1, 2, 2, 3, 3);

        FunctionalHashTrie r = subject.withoutKey(3);
        assertThat(r, is(hash1(1, 1, 2, 2)));
        assertThat(r, not(subject));
    }


    // withoutKeys()

    @Test
    public void withoutKeysGivenSomeKeysReturnsSubset()
    {
        FunctionalHashTrie origin = hash1(1, 1, 2, 2, 3, 3);
        FunctionalHashTrie t = origin.withoutKeys(1, 3, 5);

        assertThat(t, is(hash1(2, 2)));
        assertThat(t, not(origin));
    }


    // merge1()

    @Test
    public void merge1GivenEmptyReturnsSelf()
    {
        FunctionalHashTrie s = simpleSubject();
        FunctionalHashTrie r = s.merge1(empty());
        assertThat(r, sameInstance(s));
    }

    @Test
    public void merge1GivenValidInputReturnsSingleHash()
    {
        FunctionalHashTrie h1 = hash1(10, 1, 11, 1, 12, 1, 20, 2, 21, 2, 22, 2);
        FunctionalHashTrie h2 = hash1(1, 5, 2, 6, 11, 5, 12, 6, 21, 5, 22, 6);

        FunctionalHashTrie r = h1.merge1(h2);
        assertThat(r, is(hash1(1, 5, 2, 6, 10, 1, 11, 5, 12, 6, 20, 2, 21, 5, 22, 6)));
    }


    // mergeMulti()

    @Test
    public void mergeMultiGivenMultiHashReturnsUnion()
    {
        MultiHashTrie h1 = hash1(10,1, 11,1, 12,1);
        MultiHashTrie h2 = multi( 1,5, 2,6, 2,6, 11,5, 12,1, 12,6, 12,6);

        MultiHashTrie r = mergeMulti(h1, h2);
        assertThat(r, is(hash(1,5, 2,6, 2,6, 10,1, 11,1, 11,5, 12,1, 12,1, 12,6, 12,6)));
    }


    // transform()

    @Test
    public void transformReturnsEmpty()
    {
        FunctionalHashTrie h1 = hash1(1, 3, 3, 3, 4, 6, 5, 15);
        expectEmpty(h1.transform(new TransformTestCase.Remove3sIncrement2sXform()));
    }

    @Test
    public void transformGivenNoopReturnsSame()
    {
        FunctionalHashTrie h = hash1(1, 1, 2, 1, 4, 5);
        assertThat(h.transform(new TransformTestCase.Remove3sIncrement2sXform()),
                   sameInstance(h));
    }

    @Test
    public void transformMixedChanges()
    {
        FunctionalHashTrie h = hash1(1, 1, 2, 2, 3, 3, 4, 1, 5, 1, 6, 2, 7, 3, 8, 3);
        assertThat(h.transform(new TransformTestCase.Remove3sIncrement2sXform()),
                   is(hash1(1, 1, 2, 3, 4, 1, 5, 1, 6, 3)));
    }


    // oneify()

    @Test
    public void oneifyBehavesNormally()
    {
        checkOneify(hash1(1, 1));
        checkOneify(hash1(1, 1, 2, 2, 3, 3, 4, 4));
    }
}
