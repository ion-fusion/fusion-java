// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * A {@link MultiHashTrie} that has at most one entry per key.
 * <p>
 * <b>Warning:</b> This class is an internal implementation detail of FusionJava;
 * it is not for use by applications.
 * </p>
 *
 * @param <K> The type of key in an entry.
 * @param <V> The type of value in an entry.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FunctionalHashTrie<K, V>
    extends MultiHashTrie<K, V>
{
    static class Changes
        extends MultiHashTrie.Changes
    {
        @Override
        <K, V> FunctionalHashTrie<K, V> resultFrom(MultiHashTrie<K, V> t,
                                                   TrieNode<K, V> newRoot)
        {
            assert t instanceof FunctionalHashTrie;

            if (changeCount() != 0)
            {
                int newSize = t.keyCount() + keyCountDelta();
                if (newSize == 0) return empty();
                return new FunctionalHashTrie<>(newRoot, newSize);
            }

            assert t.root == newRoot;
            return (FunctionalHashTrie<K, V>) t;
        }

        @Override
        <K, V> FunctionalHashTrie<K, V> resultFrom(TrieNode<K, V> newRoot)
        {
            if (changeCount() == 0) return empty();

            return new FunctionalHashTrie<>(newRoot, keyCountDelta());
        }
    }


    FunctionalHashTrie(TrieNode<K, V> root, int size)
    {
        super(root, size);
    }


    //=========================================================================
    // Creation

    public static final class Builder<K, V>
    {
        private       TrieNode<K, V> myTrie    = HashArrayMappedTrie.empty();
        private final Changes        myChanges = new Changes();

        private Builder() {}

        public void with1(K key, V value)
        {
            myTrie = myTrie.mWith(key, value, myChanges);
        }

        public FunctionalHashTrie<K, V> build()
        {
            return myChanges.resultFrom(myTrie);
        }
    }

    public static <K, V> Builder<K, V> builder1()
    {
        return new Builder<>();
    }


    public static <K, V> FunctionalHashTrie<K, V> fromEntries(Iterator<Entry<K, V>> items)
    {
        if (! items.hasNext()) return empty();

        Changes changes = new Changes();
        TrieNode<K, V> trie = HashArrayMappedTrie.fromEntries(items, changes);
        return changes.resultFrom(trie);
    }


    public static <K, V> FunctionalHashTrie<K, V> fromArrays(K[] keys,
                                                             V[] values)
    {
        if (keys.length == 0 && values.length == 0) return empty();

        Changes changes = new Changes();
        TrieNode<K, V> trie = HashArrayMappedTrie.fromArrays(keys, values, changes);
        return changes.resultFrom(trie);
    }


    /**
     * Creates a trie by copying entries for the {@code keys} from the
     * {@code origin} trie.
     */
    public static <K, V> FunctionalHashTrie<K, V>
    fromSelectedKeys(FunctionalHashTrie<K, V> origin, K[] keys)
    {
        if (keys.length == 0) return empty();

        Changes changes = new Changes();
        TrieNode<K, V> trie =
            HashArrayMappedTrie.fromSelectedKeys(origin.root, keys, changes);
        return changes.resultFrom(trie);
    }


    //=========================================================================
    // Inspection

    public int size()
    {
        return keyCount();
    }


    @Override
    public Iterator<Entry<K, V>> iterator()
    {
        return root.iterator();
    }

    @Override
    public Iterator<Entry<K, V>> oneifyIterator()
    {
        return root.iterator();
    }


    @Override
    public V get(K key)
    {
        validateKey(key);

        return root.get(key);
    }


    @Override
    public Collection<V> getMulti(K key)
    {
        validateKey(key);

        V o = root.get(key);

        if (o == null) return Collections.emptySet();

        return Collections.singleton(o);
    }


    //=========================================================================
    // Modification

    @Override
    public FunctionalHashTrie<K, V> with1(K key, V value)
    {
        validateKeyAndValue(key, value);

        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.with(key, value, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> merge1(MultiHashTrie<K, V> that)
    {
        if (that.isEmpty()) return this;

        Changes        changes = new Changes();
        TrieNode<K, V> newRoot = root.with(that.oneifyIterator(), changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> withoutKey(K key)
    {
        validateKey(key);

        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.without(key, changes);
        return changes.resultFrom(this, newRoot);
    }

    @Override
    public FunctionalHashTrie<K, V> withoutKeys(K... keys)
    {
        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.withoutKeys(keys, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> transform(BiFunction<K, V, V> xform)
    {
        Changes changes = new Changes();
        TrieNode<K, V> newRoot = root.transform(xform, changes);
        return changes.resultFrom(this, newRoot);
    }


    @Override
    public FunctionalHashTrie<K, V> oneify()
    {
        return this;
    }


    //=========================================================================
    // Comparison

    public boolean equals(Object that)
    {
        return (this == that)
                   || (that instanceof FunctionalHashTrie
                           && this.equals((FunctionalHashTrie) that));
    }

    public boolean equals(FunctionalHashTrie<K, V> that)
    {
        return equals(that, EQUALS_BIPRED);
    }

    @Override
    protected boolean mappingEquals(V lv, V rv, BiPredicate<V, V> comp)
    {
        return comp.test(lv, rv);
    }
}
