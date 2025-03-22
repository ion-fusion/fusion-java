// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.EmptyHashTrie.EMPTY;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * A persistent hash map based on {@link HashArrayMappedTrie}, allowing multiple
 * entries per key.
 * <p>
 * <b>Warning:</b> This class is an internal implementation detail of FusionJava;
 * it is not for use by applications.
 * </p>
 * <p>
 * There's currently no support for null keys or values. Attempts to use null
 * on operations such as {@link #get(Object)} or {@link #with1(Object, Object)}
 * will throw a {@link NullPointerException}.
 * </p>
 * <p>
 * Iteration order of this data structure is undefined and not guaranteed to be stable.
 * </p>
 *
 * @param <K> The type of key in an entry.
 * @param <V> The type of value in an entry.
 */
public abstract class MultiHashTrie<K, V>
    implements Iterable<Entry<K, V>>
{
    private static final String NULL_ERROR_MESSAGE =
        "Hashes do not support null keys or values";

    protected void validateKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }
    }

    protected void validateKeyAndValue(Object key, Object value)
    {
        if (key == null || value == null)
        {
            throw new NullPointerException(NULL_ERROR_MESSAGE);
        }
    }

    abstract static class Changes
        extends HashArrayMappedTrie.Changes
    {
        abstract <K, V> MultiHashTrie<K, V> resultFrom(MultiHashTrie<K, V> t,
                                                       TrieNode<K, V> newRoot);

        <K, V> MultiHashTrie<K, V> resultFrom(TrieNode<K, V> newRoot)
        {
            return resultFrom(MultiHashTrie.<K,V>empty(), newRoot);
        }
    }


    protected final TrieNode<K, V> root;
    private   final int            keyCount;

    MultiHashTrie(TrieNode<K, V> root, int keyCount)
    {
        this.root     = root;
        this.keyCount = keyCount;
    }


    //=========================================================================
    // Creation

    public static final class Builder<K, V>
    {
        private       TrieNode<K, V> myTrie    = HashArrayMappedTrie.empty();
        private final Changes        myChanges = new MultiHashTrieImpl.Changes();

        private Builder() {}

        public void withMulti(K key, V value)
        {
            myTrie = myTrie.mWith(key, value, myChanges);
        }

        public MultiHashTrie<K, V> build()
        {
            return myChanges.resultFrom(myTrie);
        }
    }

    public static <K, V> Builder<K, V> builderMulti()
    {
        return new Builder<>();
    }


    @SuppressWarnings("unchecked")
    public static <K, V> FunctionalHashTrie<K, V> empty()
    {
        return EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> FunctionalHashTrie<K, V> singleEntry(K key, V value)
    {
        TrieNode<K, V> trie = HashArrayMappedTrie.singleEntry(key, value);
        return new FunctionalHashTrie<>(trie, 1);
    }

    public static <K, V> FunctionalHashTrie<K, V> fromMap(Map<K, V> other)
    {
        if (other.isEmpty()) return empty();

        FunctionalHashTrie.Changes changes = new FunctionalHashTrie.Changes();
        TrieNode<K, V>             trie    = HashArrayMappedTrie.fromMap(other, changes);
        return changes.resultFrom(trie);
    }


    /**
     * When the same key is in multiple entries, a true multi-hash is created.
     */
    public static <K, V> MultiHashTrie<K, V> fromEntries(Iterator<Entry<K, V>> items)
    {
        if (! items.hasNext()) return empty();

        Changes        changes = new MultiHashTrieImpl.Changes();
        TrieNode<K, V> trie    = HashArrayMappedTrie.fromEntries(items, changes);
        return changes.resultFrom(trie);
    }


    public static <K, V> MultiHashTrie<K, V> fromArrays(K[] keys, V[] values)
    {
        if (keys.length == 0 && values.length == 0) return empty();

        Changes        changes = new MultiHashTrieImpl.Changes();
        TrieNode<K, V> trie    = HashArrayMappedTrie.fromArrays(keys, values, changes);
        return changes.resultFrom(trie);
    }


    /**
     * Creates a trie by copying entries for the {@code keys} from the
     * {@code origin} trie.
     */
    public static <K, V> MultiHashTrie<K, V>
    fromSelectedKeys(MultiHashTrie<K, V> origin, K... keys)
    {
        if (keys.length == 0) return empty();

        Changes changes = new MultiHashTrieImpl.Changes();
        TrieNode<K, V> trie =
            HashArrayMappedTrie.fromSelectedKeys(origin.root, keys, changes);
        return changes.resultFrom(trie);
    }


    //=========================================================================
    // Inspection

    public final boolean isEmpty()
    {
        return keyCount == 0;
    }

    /**
     * Returns the number of distinct keys in the hash.
     */
    public final int keyCount()
    {
        return keyCount;
    }

    /**
     * Returns the number of entries (key-value pairs) in the hash.
     */
    public abstract int size();


    /**
     * @param key to examine the map for.
     * @return true if the key is in the map, false otherwise.
     */
    public boolean containsKey(K key)
    {
        return root.get(key) != null;
    }

    /**
     * Get a value associated with the key.
     * If the key has multiple entries in this hash, one is selected arbitrarily.
     *
     * @param key the key to search for.
     * @return the value associated with key, null if it is not in this hash.
     */
    public abstract V get(K key);

    /**
     * Gets all values associated with a key.
     *
     * @param key the key to search for.
     *
     * @return an immutable collection.
     */
    public abstract Collection<V> getMulti(K key);


    /**
     * Iterate by each key-value entry, potentially returning multiple entries
     * for the same key.
     */
    public abstract Iterator<Entry<K, V>> iterator();

    /**
     * Iterate by key, returning only one value per key.
     * Equivalent to {@code this.oneify().iterator()} but much more efficient.
     */
    public abstract Iterator<Entry<K, V>> oneifyIterator();


    //=========================================================================
    // Modification

    /**
     * Replaces all existing entries for a key with a single value.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> with1(K key, V value);


    /**
     * Adds a new entry for the given key-value, retaining existing entries for
     * the key.
     */
    public MultiHashTrie<K, V> withMulti(K key, V value)
    {
        validateKeyAndValue(key, value);

        Changes        changes = new MultiHashTrieImpl.Changes();
        TrieNode<K, V> newRoot = root.with(key, value, changes);
        return changes.resultFrom(this, newRoot);
    }


    /**
     * Removes all existing entries for a key.
     *
     * @param key must not be null.

     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> withoutKey(K key);


    /**
     * Removes multiple keys from this trie.
     *
     * @param keys must not be null and must not contain a null element.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    @SuppressWarnings("unchecked")
    public abstract MultiHashTrie<K, V> withoutKeys(K... keys);


    /**
     * Merges two hashes, retaining one entry per key.
     * Values from the argument will replace those from this hash with the same key.
     * If a key has multiple entries, one is selected arbitrarily.
     */
    public abstract FunctionalHashTrie<K, V> merge1(MultiHashTrie<K, V> that);


    /**
     * Merges two hashes, retaining all entries for all keys.
     */
    public MultiHashTrie<K, V> mergeMulti(MultiHashTrie<K, V> that)
    {
        if (that.isEmpty()) return this;

        // Iterate the smaller trie for faster performance.
        if (this.keyCount <= that.keyCount)
        {
            return mergeMulti(this, that);
        }
        else
        {
            return mergeMulti(that, this);
        }
    }

    private static <K, V> MultiHashTrie<K, V> mergeMulti(MultiHashTrie<K, V> sm,
                                                         MultiHashTrie<K, V> lg)
    {
        Changes        changes = new MultiHashTrieImpl.Changes();
        TrieNode<K, V> newRoot = lg.root.with(sm.root.iterator(), changes);
        return changes.resultFrom(lg, newRoot);
    }


    /**
     * Applies a transformation function to each key-value entry in the trie.
     *
     * @param xform accepts the existing key and value, returning a transformed
     *              value for the key.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract MultiHashTrie<K, V> transform(BiFunction<K, V, V> xform);


    /**
     * Remove all but one value for every key, so that there's no remaining
     * multi-entry keys.
     * If a key has multiple entries, one is selected arbitrarily.
     *
     * @return the resulting trie; {@code this} if nothing has changed.
     */
    public abstract FunctionalHashTrie<K, V> oneify();


    //=========================================================================
    // Comparison

    @SuppressWarnings("rawtypes")
    protected static final BiPredicate EQUALS_BIPRED = new BiPredicate()
    {
        public boolean test(Object o1, Object o2)
        {
            return o1.equals(o2);
        }
    };

    /**
     * Compare against another hash, using a predicate to compare values.
     */
    public boolean equals(MultiHashTrie<K,V> that, BiPredicate<V, V> comp)
    {
        if (this.getClass() != that.getClass()) return false;

        if (size() != that.size()) return false;

        for (Entry<K, V> entry : root)
        {
            K fieldName = entry.getKey();

            V lv = entry.getValue();
            V rv = that.root.get(fieldName);

            if (rv == null) return false;

            if (! mappingEquals(lv, rv, comp)) return false;
        }

        return true;
    }

    protected abstract boolean mappingEquals(V lv, V rv, BiPredicate<V, V> comp);


    public Set<K> keySet()
    {
        // FIXME This is an extremely expensive implementation.
        return new AbstractSet<K>()
        {
            @Override
            public Iterator<K> iterator()
            {
                final Iterator<Entry<K, V>> entryIter = MultiHashTrie.this.iterator();
                return new Iterator<K>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return entryIter.hasNext();
                    }

                    @Override
                    public K next()
                    {
                        return entryIter.next().getKey();
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return keyCount;
            }
        };
    }
}
