// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.FunctionalHashTrie.fromEntries;
import static dev.ionfusion.fusion.util.hamt.MultiHashTrie.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class MultiHashTrieTestCase
{
    static void expectEmpty(MultiHashTrie t)
    {
        assertTrue(t.isEmpty());
        assertThat(t.keyCount(), is(0));
        assertThat(t, sameInstance((MultiHashTrie) empty()));
        assertTrue(t.equals(empty()));
        assertTrue(empty().equals(t));
    }


    static void expectMapping(MultiHashTrie<Object, Object> hash,
                              Object key,
                              Object... expectedValues)
    {
        assertThat(hash.containsKey(key), is(true));
        assertThat(hash.get(key), isIn(expectedValues));
        assertThat(hash.getMulti(key), containsInAnyOrder(expectedValues));
    }


    /**
     * Wraps {@link FunctionalHashTrie#with1(Object, Object)} with validation.
     */
    static FunctionalHashTrie with1(FunctionalHashTrie h, Object k, Object v)
    {
        Object priorValue = h.get(k);

        FunctionalHashTrie r = h.with1(k, v);
        expectMapping(r, k, v);

        if (priorValue == v)
        {
            assertSame(h, r);
        }

        return r;
    }

    /**
     * Wraps {@link MultiHashTrie#withMulti(Object, Object)} with validation.
     */
    static MultiHashTrie withMulti(MultiHashTrie h, Object k, Object v)
    {
        ArrayList expectedValues = new ArrayList(h.getMulti(k));
        expectedValues.add(v);

        MultiHashTrie r = h.withMulti(k, v);
        expectMapping(r, k, expectedValues.toArray());

        // When the key is absent, with1 and withMulti have the same effect.
        if (! h.containsKey(k))
        {
            assertEquals(h.with1(k, v), r);
        }

        return r;
    }


    static FunctionalHashTrie hash1(Object... kvPairs)
    {
        assertEquals(0, kvPairs.length % 2);

        FunctionalHashTrie hash = empty();
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            hash = with1(hash, kvPairs[i], kvPairs[i+1]);
        }

        // Test iteration and construction through another route.
        assertEquals(hash, fromEntries(hash.iterator()));

        FunctionalHashTrie.Builder b = FunctionalHashTrie.builder1();
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            b.with1(kvPairs[i], kvPairs[i+1]);
        }
        assertEquals(hash, b.build());

        return hash;
    }


    static MultiHashTrie hash(Object... kvPairs)
    {
        assertEquals(0, kvPairs.length % 2);

        MultiHashTrie hash = empty();
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            hash = withMulti(hash, kvPairs[i], kvPairs[i+1]);
        }

        // Test iteration and construction through another route.
        assertEquals(hash, MultiHashTrie.fromEntries(hash.iterator()));

        MultiHashTrie.Builder b = MultiHashTrie.builderMulti();
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            b.withMulti(kvPairs[i], kvPairs[i+1]);
        }
        assertEquals(hash, b.build());


        return hash;
    }

    static MultiHashTrie multi(Object... kvPairs)
    {
        MultiHashTrie r = hash(kvPairs);
        assertTrue(r.keyCount() < r.size());
        return r;
    }


    static Entry entry(Object k, Object v)
    {
        return new AbstractMap.SimpleEntry(k, v);
    }

    static Iterator iterate(Object... values)
    {
        return Arrays.asList(values).iterator();
    }


    /**
     * Return a simple hash of the relevant subtype (multi/single/empty).
     */
    abstract MultiHashTrie simpleSubject();


    //=========================================================================
    // Inspection
    //   These are exercised extensively via hash() and other fixture methods.

    // containsKey()

    @Test
    public void containsKeyRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().containsKey(null));
    }


    // get() -- Exercised via hash() and other fixture methods.

    @Test
    public void getRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().get(null));
    }


    // getMulti()

    @Test
    public void getMultiRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().getMulti(null));
    }

    @Test
    public void getMultiGivenAbsentKeyReturnsEmptyCollection()
    {
        assertTrue(simpleSubject().getMulti(-1).isEmpty());
    }


    //=========================================================================
    // Modification

    // with1()

    @Test
    public void with1RejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().with1(null, 1));
    }

    @Test
    public void with1RejectsNullValue()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().with1(2, null));
    }


    // withMulti()

    @Test
    public void withMultiRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withMulti(null, 1));
    }

    @Test
    public void withMultiRejectsNullValue()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withMulti(2, null));
    }


    // withoutKey()

    @Test
    public void withoutKeyRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withoutKey(null));
    }

    @Test
    public void withoutKeyGivenAbsentKeyReturnsSelf()
    {
        MultiHashTrie subject = simpleSubject();
        assertFalse(subject.containsKey(-1));
        assertThat(subject.withoutKey(-1), sameInstance(subject));
    }


    // withoutKeys()

    @Test
    public void withoutKeysRejectsNullArray()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withoutKeys((Object[]) null));
    }

    @Test
    public void withoutKeysRejectsNullKey()
    {
        assertThrows(NullPointerException.class,
                     () -> simpleSubject().withoutKeys(1, null, 3));
    }

    @Test
    public void withoutKeysGivenAbsentKeysReturnsSelf()
    {
        MultiHashTrie s = simpleSubject();
        assertFalse(s.containsKey(-1));
        assertFalse(s.containsKey(-2));
        assertThat(s.withoutKeys(-1, -2), sameInstance(s));
    }

    @Test
    public void withoutKeysGivenAllKeysReturnsEmpty()
    {
        MultiHashTrie s = simpleSubject();
        Object[]      keys = s.keySet().toArray();
        expectEmpty(s.withoutKeys(keys));
    }


    // mergeMulti()

    /**
     * Wraps {@link FunctionalHashTrie#mergeMulti(MultiHashTrie)} with validation.
     */
    static MultiHashTrie mergeMulti(MultiHashTrie h1, MultiHashTrie h2)
    {
        // Ensure symmetry of the operation.
        MultiHashTrie r1 = h1.mergeMulti(h2);
        MultiHashTrie r2 = h2.mergeMulti(h1);

        assertEquals(r1, r2);
        assertEquals(r2, r1);
        assertTrue(h2.isEmpty() || ! r1.equals(h1));
        assertTrue(h1.isEmpty() || ! r2.equals(h2));

        return r1;
    }


    @Test
    public void mergeMultiGivenEmptyReturnsSelf()
    {
        MultiHashTrie s = simpleSubject();
        assertThat(mergeMulti(s, empty()), sameInstance(s));
    }


    // oneify()

    static void checkOneify(MultiHashTrie h)
    {
        FunctionalHashTrie<Object, Object> r = h.oneify();
        assertThat(r.keyCount(), is(h.keyCount()));

        for (Entry<Object, Object> entry : r)
        {
            Object key = entry.getKey();
            assertThat(entry.getValue(), isIn(h.getMulti(key)));
            h = h.withoutKey(key); // So we don't match the key again.
        }

        assertThat(h.size(), is(0));
    }
}
