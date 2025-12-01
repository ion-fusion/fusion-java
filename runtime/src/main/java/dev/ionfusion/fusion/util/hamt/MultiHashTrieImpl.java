// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.NOTHING;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * A persistent, immutable hash table allowing multiple entries for the same key.
 * <p>
 * Neither null keys nor null values are supported.
 * </p>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
@SuppressWarnings("unchecked")
class MultiHashTrieImpl<K, V>
    extends MultiHashTrie<K, V>
{
    /**
     * Manages changes to a MultiHashTrieImpl, using private {@link MultiValue}
     * instances to store repeated fields.
     */
    static class Changes
        extends MultiHashTrie.Changes
    {
        private int valueCountDelta = 0;

        int valueCountDelta()
        {
            return valueCountDelta;
        }

        @Override
        protected Object inserting(Object givenValue)
        {
            // Must allow MultiValue here for fromSelectedKeys() to work.
            return givenValue;
        }

        @Override
        protected Object replacing(Object storedValue, Object givenValue)
        {
            if (givenValue instanceof MultiValue)
            {
                // This case is used by mergeMulti
                if (storedValue instanceof MultiValue)
                {
                    return ((MultiValue) givenValue).add((MultiValue) storedValue);
                }
                return ((MultiValue) givenValue).add(storedValue);
            }
            if (storedValue instanceof MultiValue)
            {
                return ((MultiValue) storedValue).add(givenValue);
            }
            return new MultiValue(storedValue, givenValue);
        }

        @Override
        protected void keyAdded(Object key, Object newValue)
        {
            valueCountDelta += count(newValue);
            super.keyAdded(key, newValue);
        }

        @Override
        protected void keyReplaced(Object key, Object oldValue, Object newValue)
        {
            valueCountDelta += count(newValue) - count(oldValue);
            super.keyReplaced(key, oldValue, newValue);
        }

        @Override
        protected void keyRemoved(Object key, Object oldValue)
        {
            valueCountDelta -= count(oldValue);
            super.keyRemoved(key, oldValue);
        }

        private static int count(Object value)
        {
            if (value instanceof MultiValue)
            {
                return ((MultiValue) value).count();
            }
            return 1;
        }

        <K, V> MultiHashTrie<K, V> resultFrom(MultiHashTrie<K, V> t, TrieNode<K, V> newRoot)
        {
            if (changeCount() != 0)
            {
                int newKeyCount = t.keyCount() + keyCountDelta();
                if (newKeyCount == 0) return empty();
                int newValueCount = t.size() + valueCountDelta();

                // It's a single-hash IFF it has the same number of keys and values.
                return (newKeyCount == newValueCount
                            ? new FunctionalHashTrie<>(newRoot, newKeyCount)
                            : new MultiHashTrieImpl<>(newRoot, newKeyCount, newValueCount));
            }

            assert t.root == newRoot;
            return t;
        }
    }


    /**
     * Returns a single value from a possibly multi-value hash element.
     */
    private static Object oneify(Object value)
    {
        if (value instanceof MultiValue)
        {
            return ((MultiValue) value).oneify();
        }
        return value;
    }

    /**
     * Manages changes to a hash, ensuring changed fields only have a
     * single value.
     */
    private static class OneifyChanges
        extends Changes
    {
        /**
         * This accepts {@link MultiValue} so that `merge1` has the right results.
         */
        @Override
        public Object inserting(Object givenValue)
        {
            return oneify(givenValue);
        }

        @Override
        public Object replacing(Object storedValue, Object givenValue)
        {
            return oneify(givenValue);
        }
    }

    /**
     * HAMT transform that replaces all multi-value entries with a single value.
     */
    private static final BiFunction<Object, Object, Object> ONEIFY_XFORM =
        new BiFunction<Object, Object, Object>()
        {
            @Override
            public Object apply(Object key, Object value)
            {
                return oneify(value);
            }
        };


    private final int myValueCount;

    private MultiHashTrieImpl(TrieNode<K, V> root, int keyCount, int valueCount)
    {
        super(root, keyCount);

        assert keyCount != valueCount;

        myValueCount = valueCount;
    }


    //=========================================================================
    // Inspection

    @Override
    public int size()
    {
        return myValueCount;
    }


    @Override
    public Iterator<Entry<K, V>> iterator()
    {
        return new SingleValueIterator<>(root.iterator());
    }

    @Override
    public Iterator<Entry<K, V>> oneifyIterator()
    {
        return new OneifyIterator<>(root.iterator());
    }


    @Override
    public V get(K key)
    {
        validateKey(key);

        return (V) oneify(root.get(key));
    }


    @Override
    public Collection<V> getMulti(K key)
    {
        validateKey(key);

        V o = root.get(key);

        if (o instanceof MultiValue) return ((MultiValue) o).getMulti();

        if (o == null) return Collections.emptySet();

        return Collections.singleton(o);
    }


    //=========================================================================
    // Modification

    @Override
    public MultiHashTrie<K, V> with1(K key, V value)
    {
        validateKeyAndValue(key, value);

        Changes changes = new OneifyChanges();
        TrieNode<K, V> newRoot = root.with(key, value, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> merge1(MultiHashTrie<K, V> that)
    {
        OneifyChanges changes = new OneifyChanges();
        // TODO see if its better to oneify after merging. That might visit more
        //   entries but make fewer changes.
        TrieNode<K, V> newRoot = root.transform((BiFunction<K, V, V>) ONEIFY_XFORM, changes);
        if (! that.isEmpty())
        {
            newRoot = newRoot.with(that.oneifyIterator(), changes);
        }
        return (FunctionalHashTrie<K, V>) changes.resultFrom(this, newRoot);
    }


    @Override
    public MultiHashTrie<K, V> withoutKey(K key)
    {
        validateKey(key);

        Changes changes        = new Changes();
        TrieNode<K, V> newRoot = root.without(key, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public MultiHashTrie<K, V> withoutKeys(K... keys)
    {
        Changes changes        = new Changes();
        TrieNode<K, V> newRoot = root.withoutKeys(keys, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public MultiHashTrie<K, V> transform(BiFunction<K, V, V> xform)
    {
        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.transform(new MultiXform(xform), changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> oneify()
    {
        OneifyChanges changes = new OneifyChanges();
        TrieNode<K, V> newRoot = root.transform((BiFunction<K, V, V>) ONEIFY_XFORM, changes);
        return (FunctionalHashTrie<K, V>) changes.resultFrom(this, newRoot);
    }


    //=========================================================================
    // Comparison

    @Override
    public boolean equals(Object that)
    {
        return (this == that)
                   || (that instanceof MultiHashTrieImpl
                           && this.equals((MultiHashTrieImpl<K, V>) that));
    }

    public boolean equals(MultiHashTrieImpl<K, V> that)
    {
        return equals(that, EQUALS_BIPRED);
    }

    @Override
    protected boolean mappingEquals(V lv, V rv, BiPredicate<V, V> comp)
    {
        if (lv instanceof MultiValue) return ((MultiValue) lv).equals(rv, comp);

        if (rv instanceof MultiValue) return false;

        return comp.test(lv, rv);
    }


    //=========================================================================
    // Support Classes

    /**
     * Holds multiple values associated with a single key for storage in the
     * underlying {@link HashArrayMappedTrie}.
     */
    private static final class MultiValue
    {
        private final Object[] myValues;

        private MultiValue(Object[] values)
        {
            myValues = values;
        }

        private MultiValue(Object v1, Object v2)
        {
            myValues = new Object[] { v1, v2 };
        }

        // TODO introduce randomness to add() so ordering isn't stable.

        private MultiValue add(Object newValue)
        {
            int len = myValues.length;
            Object[] newValues = Arrays.copyOf(myValues, len + 1);
            newValues[len] = newValue;
            return new MultiValue(newValues);
        }

        private MultiValue add(MultiValue that)
        {
            int thisLen = this.myValues.length;
            int thatLen = that.myValues.length;
            Object[] newValues = new Object[thisLen + thatLen];
            System.arraycopy(this.myValues, 0, newValues,       0, thisLen);
            System.arraycopy(that.myValues, 0, newValues, thisLen, thatLen);
            return new MultiValue(newValues);
        }

        private int count()
        {
            return myValues.length;
        }

        private Object oneify()
        {
            return myValues[0];
        }

        private <V> Collection<V> getMulti()
        {
            return (Collection<V>) Collections.unmodifiableCollection(Arrays.asList(myValues));
        }

        /* When multiple values are mapped from the same key, they form a bag --
         * an unordered collection which allows duplicates.  So here, when
         * comparing those bags, we match items in any order.  Otherwise, two
         * hashes with the same key-value mappings could be unequal if the
         * entries were added in different orders.
         */
        private <V> boolean equals(Object rv, BiPredicate<V, V> comp)
        {
            if (! (rv instanceof MultiValue)) return false;

            Object[] lArray = myValues;
            Object[] rArray = ((MultiValue) rv).myValues;

            int lCount = lArray.length;
            int rCount = rArray.length;
            if (lCount != rCount) return false;

            rArray = Arrays.copyOf(rArray, rCount);
            for (Object l : lArray)
            {
                // Seek a matching element from rArray
                boolean found = false;
                for (int j = 0; j < rCount; j++)
                {
                    Object r = rArray[j];
                    if (comp.test((V) l, (V) r))
                    {
                        found = true;
                        rArray[j] = rArray[--rCount];
                        break;
                    }
                }

                if (!found) return false;
            }

            // By now we've found a match for everything!
            assert rCount == 0;

            return true;
        }

        @SuppressWarnings("rawtypes")
        private Object transform(Object key, BiFunction myWrappedXform)
        {
            Object[] children = myValues;
            int childCount = children.length;

            Object[] newChildren = null; // Allocated only when an entry changes.
            int j = 0;                   // Result index

            for (int i = 0; i < childCount; i++)
            {
                Object child = children[i];
                Object newChild = myWrappedXform.apply(key, child);
                if (newChild != NOTHING)
                {
                    if (newChildren != null)
                    {
                        newChildren[j] = newChild;
                    }
                    else if (newChild != child || i != j)
                    {
                        // We need a new array the first time a child changes, or when we need
                        // to shift children down to fill a gap caused by entries being removed.
                        // Both scenarios imply that we cannot reuse the original array.
                        newChildren    = Arrays.copyOf(children, childCount - (i - j));
                        newChildren[j] = newChild;
                    }
                    j++;
                }
            }

            if (j == 0) return NOTHING;

            if (newChildren == null) return this;

            if (j == 1) return newChildren[0];

            if (j != newChildren.length) newChildren = Arrays.copyOf(newChildren, j);

            return new MultiValue(newChildren);
        }
    }


    /**
     * Adapts a transform function to handle {@link MultiValue} entries.
     */
    @SuppressWarnings("rawtypes")
    private static final class MultiXform
        implements BiFunction
    {
        private final BiFunction myWrappedXform;

        private MultiXform(BiFunction wrappedXform)
        {
            myWrappedXform = wrappedXform;
        }

        @Override
        public Object apply(Object key, Object storedValue)
        {
            if (storedValue instanceof MultiValue)
            {
                return ((MultiValue) storedValue).transform(key, myWrappedXform);
            }
            return myWrappedXform.apply(key, storedValue);
        }
    }


    /**
     * Iterates by individual key-value pair, returning multiple Entries when a
     * key maps to multiple values.
     *
     * TODO: Add MultiValueIterator, that returns the Collection of values for
     * each key.
     */
    private static final class SingleValueIterator<K, V>
        implements Iterator<Entry<K, V>>
    {
        private final Iterator<Entry<K, V>> myHamtIterator;

        private K        myCurrentKey;
        private Object[] myCurrentArray;
        private int      myNextArrayIndex;

        SingleValueIterator(Iterator<Entry<K, V>> hamtIterator)
        {
            myHamtIterator = hamtIterator;
        }

        @Override
        public boolean hasNext()
        {
            if (myCurrentArray != null && myNextArrayIndex < myCurrentArray.length)
            {
                return true;
            }
            return myHamtIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Entry<K, V> next()
        {
            while (true)
            {
                if (myCurrentArray == null)
                {
                    Entry<K, V> entry = myHamtIterator.next();
                    if (!(entry.getValue() instanceof MultiValue))
                    {
                        return entry;
                    }

                    myCurrentKey     = entry.getKey();
                    myCurrentArray   = ((MultiValue) entry.getValue()).myValues;
                    myNextArrayIndex = 0;
                }

                if (myNextArrayIndex < myCurrentArray.length)
                {
                    return new SimpleEntry<>(myCurrentKey, (V) myCurrentArray[myNextArrayIndex++]);
                }

                myCurrentKey   = null;
                myCurrentArray = null;
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Iterates by key, returning one entry when a key maps to multiple values.
     * Equivalent to iterating the result of {@code this.oneify()}, but faster.
     */
    private static final class OneifyIterator<K, V>
        implements Iterator<Entry<K, V>>
    {
        private final Iterator<Entry<K, V>> myHamtIterator;

        OneifyIterator(Iterator<Entry<K, V>> hamtIterator)
        {
            myHamtIterator = hamtIterator;
        }

        @Override
        public boolean hasNext()
        {
            return myHamtIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Entry<K, V> next()
        {
            Entry  entry = myHamtIterator.next();
            Object value = entry.getValue();
            if (value instanceof MultiValue)
            {
                entry.setValue(((MultiValue)value).oneify());
            }
            return entry;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
