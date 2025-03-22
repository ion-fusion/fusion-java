// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * Implementation of the Hash Array Mapped Trie data structure, as described in the paper
 * <em>Ideal Hash Tries</em> by Phil Bagwell.
 * <p>
 * This class is an internal implementation detail of FusionJava, is not intended
 * for reuse, and does not support Java nulls as keys or values.
 * <p>
 * Iteration order of this data structure is undefined. All key hashes used within
 * {@link HashArrayMappedTrie} are shuffled differently in each JRE. All hashes used for
 * navigating through the node structure should be routed through {@link #hashCodeFor(Object)}.
 * <p>
 * This implementation was originally based on Clojure's PersistentHashMap and so it:
 * <ul>
 *     <li>Uses path copying for persistence.</li>
 *     <li>Uses {@link CollisionNode}s rather than extended hashing.</li>
 *     <li>Uses node polymorphism rather than conditional checks.</li>
 * </ul>
 *
 * @see <a href="https://infoscience.epfl.ch/record/64398/files/idealhashtrees.pdf">Ideal Hash Trees</a>
 */
@SuppressWarnings({"unchecked"})
public final class HashArrayMappedTrie
{
    // This class isn't instantiable.
    private HashArrayMappedTrie() {}

    private static final int STOP_RELYING_ON_UNDEFINED_BEHAVIOR =
        new Random().nextInt();


    private static final class NoValueSentinel {}

    /**
     * Sentinel object denoting the lack of a value.
     */
    public static final Object NOTHING = new NoValueSentinel();


    /**
     * Intercepts and records changes made by one or more trie operations.
     * Instances can be reused across multiple calls into the same or different
     * tries, reducing the overall number of object allocations.
     */
    static class Changes
    {
        private int keysAdded     = 0;
        private int keysReplaced  = 0;
        private int keysRemoved   = 0;

        public Changes()
        {
        }

        /**
         * Returns the number of individual changes made using this instance.
         * Incremented when a key/value is added or removed, or a key's value is changed.
         * Due to mutation operations, this can be non-zero even when the
         * operation returns the same root node.
         */
        public int changeCount()
        {
            return keysAdded + keysReplaced + keysRemoved;
        }

        /**
         * Indicates how the operation changed the total number of keys in the trie.
         * This may be different from the number of keys changed: if one key was added and one
         * removed, then two keys were changed but the delta would be zero.
         */
        public int keyCountDelta()
        {
            return keysAdded - keysRemoved;
        }

        /**
         * Intercepts insertions, allowing a given value to be stored in a
         * different form.
         *
         * @param givenValue the value that was requested to be inserted.
         *
         * @return the value to be stored.
         */
        protected Object inserting(Object givenValue)
        {
            return givenValue;
        }

        /**
         * Records the addition of a new key to the trie.
         * Overrides <b>must</b> invoke this implementation!
         *
         * @param newValue the value newly stored in the trie.
         */
        protected void keyAdded(Object key, Object newValue)
        {
            keysAdded++;
        }

        /**
         * Records the removal of a key-value pair.
         * Overrides <b>must</b> invoke this implementation!
         *
         * @param oldValue the value being removed from the trie.
         */
        protected void keyRemoved(Object key, Object oldValue)
        {
            keysRemoved++;
        }

        /**
         * Intercepts the replacement of a value for the same key.
         *
         * @param storedValue the value currently present in the trie.
         * @param givenValue the value that the operation is inserting.
         *
         * @return the value to be stored.
         */
        protected Object replacing(Object storedValue, Object givenValue)
        {
            return givenValue;
        }

        /**
         * Records the replacement of a key-value pair.
         * Overrides <b>must</b> invoke this implementation!
         *
         * @param key the key that has been modified.
         * @param oldValue the value previously stored in the trie.
         * @param newValue the value newly stored in the trie.
         */
        protected void keyReplaced(Object key, Object oldValue, Object newValue)
        {
            keysReplaced++;
        }
    }


    /**
     * Returns an empty, immutable trie.
     */
    public static <K, V> TrieNode<K, V> empty()
    {
        return (TrieNode<K, V>) EmptyNode.SINGLETON;
    }


    public static <K, V> TrieNode<K, V> singleEntry(K key, V value)
    {
        return new FlatNode<>(key, value);
    }


    public static <K, V> TrieNode<K, V> fromMap(Map<K, V> map, Changes changes)
    {
        return fromEntries(map.entrySet().iterator(), changes);
    }

    public static <K, V> TrieNode<K, V> fromEntries(Iterator<Entry<K, V>> entries,
                                                    Changes changes)
    {
        TrieNode<K, V> trie = empty();
        return trie.mWith(entries, changes);
    }

    public static <K, V> TrieNode<K, V> fromArrays(K[] keys,
                                                   V[] values,
                                                   Changes changes)
    {
        TrieNode<K, V> trie = empty();
        return trie.mWithEntries(keys, values, changes);
    }

    /**
     * Creates a trie by copying entries for the {@code keys} from the
     * {@code origin} trie.
     */
    public static <K, V> TrieNode<K, V> fromSelectedKeys(TrieNode<K, V> origin,
                                                         K[] keys,
                                                         Changes changes)
    {
        TrieNode<K, V> trie = empty();
        return trie.mWithSelectedKeys(origin, keys, changes);
    }


    /**
     * Used to provide the hash code for a key in the {@link HashArrayMappedTrie}.
     * ALL trie node interfacing calls should use this method!!
     *
     * Visible for testing.
     * @param key Object to get the hash code for.
     * @return a consistently shuffled hash code.
     */
    static int hashCodeFor(Object key)
    {
        return key.hashCode() ^ STOP_RELYING_ON_UNDEFINED_BEHAVIOR;
    }


    /**
     * Abstract class interface for Nodes.
     * TODO: Convert to interface with default methods in Java 8+
     */
    public static abstract class TrieNode<K, V>
        implements Iterable<Entry<K, V>>
    {
        private TrieNode() {}

        /**
         * Computes the number of unique keys in this node and its children.
         * This is an O(n) operation.
         */
        public abstract int countKeys();

        /**
         * @return An iterator over all elements within the trie.
         */
        public abstract Iterator<Entry<K, V>> iterator();

        /**
         * Retrieves the value for a key in this trie.
         */
        public V get(K key)
        {
            return get(hashCodeFor(key), 0, key);
        }

        /**
         * Functionally inserts or updates a key in this trie, returning the resulting root node.
         * <p>
         * The returned root can be different even if nothing was changed; this allows
         * the implementation to amortize optimizations to the trie structure.
         * </p>
         */
        public TrieNode<K, V> with(K key, V value, Changes changes)
        {
            return with(hashCodeFor(key), 0, key, value, changes);
        }

        public TrieNode<K, V> with(Entry<K, V> entry, Changes changes)
        {
            return with(entry.getKey(), entry.getValue(), changes);
        }

        public TrieNode<K, V> with(Iterator<Entry<K, V>> entries,
                                   Changes changes)
        {
            TrieNode<K, V> root = this;
            while (entries.hasNext())
            {
                root = root.with(entries.next(), changes);
            }
            return root;
        }

        /**
         * Imperatively inserts or updates a key in this trie, returning the resulting root node.
         * This means that the operation may mutate this data structure, and therefore the root
         * must not be shared across containers, lest there be undesirable aliasing effects.
         * <p>
         * The returned root can be different even if nothing was changed; this allows
         * the implementation to amortize optimizations to the trie structure.
         * </p>
         */
        public TrieNode<K, V> mWith(K key, V value, Changes changes)
        {
            return mWith(hashCodeFor(key), 0, key, value, changes);
        }

        public TrieNode<K, V> mWith(Entry<K, V> entry, Changes changes)
        {
            return mWith(entry.getKey(), entry.getValue(), changes);
        }

        public TrieNode<K, V> mWith(Iterator<Entry<K, V>> entries, Changes changes)
        {
            TrieNode<K, V> root = this;
            while (entries.hasNext())
            {
                root = root.mWith(entries.next(), changes);
            }
            return root;
        }

        public TrieNode<K, V> mWithEntries(K[] keys, V[] values, Changes changes)
        {
            if (keys.length != values.length)
            {
                throw new IllegalArgumentException("keys and values have unequal length");
            }

            TrieNode<K, V> root = this;
            for (int i = 0; i < keys.length; i++)
            {
                root = root.mWith(keys[i], values[i], changes);
            }
            return root;
        }

        /**
         * Add to this trie the values of another trie at specific keys.
         */
        public TrieNode<K, V> mWithSelectedKeys(TrieNode<K, V> origin,
                                                K[] keys,
                                                Changes changes)
        {
            TrieNode<K, V> root = this;
            for (K key : keys)
            {
                V value = origin.get(key);
                if (value != null)
                {
                    root = root.mWith(key, value, changes);
                }
            }
            return root;
        }


        // TODO: Add a variation of with[out] that returns the previous value (if any).

        /**
         * Functionally removes a key from this trie, returning the resulting root node.
         * <p>
         * The returned root can be different even if nothing was removed; this allows
         * the implementation to amortize optimizations to the trie structure.
         * </p>
         */
        public TrieNode<K, V> without(K key, Changes changes)
        {
            return without(hashCodeFor(key), 0, key, changes);
        }


        /**
         * Functionally removes multiple keys from this trie.
         *
         * @param keys must not be null.
         *
         * @return the resulting root node.
         */
        public TrieNode<K, V> withoutKeys(K[] keys, Changes changes)
        {
            TrieNode<K, V> root = this;
            for (K key : keys)
            {
                root = root.without(key, changes);
            }
            return root;
        }


        /**
         * Functionally transform each entry in this trie, allowing for both
         * modification and removal of key-value entries.
         * <p>
         * The transform function {@code xform} is invoked with each key-value
         * entry in the trie, and the result determines the effect on the entry:
         * <ul>
         *     <li>
         *         When {@code xform} returns {@link #NOTHING}, the key-value
         *         entry is removed and {@link Changes#keyRemoved} is invoked.
         *     </li>
         *     <li>
         *         When {@code xform} returns a different ({@code !=}) object
         *         than it received (as its second argument), that instance
         *         becomes the new value associated with the key, and
         *         {@link Changes#keyReplaced} is invoked.
         *     </li>
         *     <li>
         *         Otherwise, the entry is left unchanged.
         *     </li>
         * </ul>
         * The transformation is optimized to reuse as much of the original trie
         * as possible; if no changes are made then result is the original trie.
         *
         * @param xform is applied to each key-value entry, and the result
         *              becomes the new value for the key.
         * @param changes is invoked when an entry is replaced or removed.
         *
         * @return the resulting root node.
         */
        public abstract TrieNode<K, V> transform(BiFunction<K, V, V> xform,
                                                 Changes changes);


        //---------------------------
        // Internal recursion methods

        static int hashFragment(int hash, int shift)
        {
            assert shift <= 30;
            return hash >>> shift & 0x1f;
        }

        static int bitPosition(int hashFragment)
        {
            return 1 << hashFragment;
        }


        /**
         * @return The value with the given key, or null if it does not exist in the trie.
         */
        abstract V get(int hash, int shift, K key);

        /**
         * Functionally modifies the trie to have the desired mapping from key to value.
         * @return Itself if it was not modified, else a new trie with the modification.
         */
        abstract TrieNode<K, V> with(int hash,
                                     int shift,
                                     K key,
                                     V value,
                                     Changes changes);

        /**
         * Mutates the trie to have the desired mapping from key to value.
         * This must only be called while constructing a new Trie, when its known that
         * nodes are not shared.
         * @return Itself
         */
        abstract TrieNode<K, V> mWith(int hash,
                                      int shift,
                                      K key,
                                      V value,
                                      Changes changes);

        /**
         * Functionally removes the mapping associated with the key from the trie.
         * @return Itself if it was not modified, else a new trie with the modification.
         */
        abstract TrieNode<K, V> without(int hash, int shift, K key, Changes changes);
    }


    private static final class EmptyNode<K, V>
        extends TrieNode<K, V>
    {
        private static final EmptyNode<Object, Object> SINGLETON = new EmptyNode<>();

        private EmptyNode() {}

        @Override
        public V get(K key)
        {
            return null;
        }

        @Override
        public int countKeys()
        {
            return 0;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        V get(int hash, int shift, K key)
        {
            return null;
        }

        @Override
        TrieNode<K, V> with(int hash, int shift, K key, V value, Changes changes)
        {
            Object newValue = changes.inserting(value);
            changes.keyAdded(key, newValue);
            return new FlatNode<>(key, newValue);
        }

        @Override
        TrieNode<K, V> mWith(int hash, int shift, K key, V value, Changes changes)
        {
            return with(hash, shift, key, value, changes);
        }

        @Override
        TrieNode<K, V> without(int hash, int shift, K key, Changes changes)
        {
            return this;
        }

        @Override
        public TrieNode<K, V> transform(BiFunction xform, Changes changes)
        {
            return this;
        }
    }


    /**
     * Base class for some node types, where entries are store as key/value
     * pairs in a single array.
     *
     * In the array, keys are stored in even indices and values at odd.
     * Where there is a null key, the following value is a {@link TrieNode}.
     *
     * In general, there may be empty (null/null) entries.
     */
    private abstract static class KvNode<K, V>
        extends TrieNode<K, V>
    {
        Object[] kvPairs;

        /**
         * For easier testing, we allow nodes with fewer children than are
         * possible to achieve via the public interface.
         */
        KvNode()
        {
            kvPairs = new Object[0];
        }

        KvNode(Object[] kvPairs)
        {
            this.kvPairs = kvPairs;
        }

        /**
         * Construct a node with the same concrete type.
         */
        abstract KvNode<K, V> makeSimilar(int bitmap, Object[] kvPairs);

        abstract int getBitmap();

        @Override
        public int countKeys()
        {
            int count = 0;
            for (int i = 0; i < kvPairs.length; i += 2)
            {
                Object keyOrNull = kvPairs[i];
                if (keyOrNull == null)
                {
                    Object node = kvPairs[i + 1];
                    if (node != null)
                    {
                        count += ((TrieNode<K, V>) node).countKeys();
                    }
                }
                else
                {
                    count++;
                }
            }
            return count;
        }

        @Override
        public final Iterator<Entry<K, V>> iterator()
        {
            return new KvArrayIterator<>(kvPairs);
        }


        /**
         * This transform() implementation covers all three KvNode subclasses,
         * since they are more similar than different. This means there's minor
         * wasted work done in each case (eg, the bitmap tracking is only needed
         * for BitMappedNode), but the algorithm is fairly complex and that
         * seems like a reasonable alternative to multiple nearly-identical
         * copies.
         */
        final public TrieNode<K, V> transform(BiFunction xform, Changes changes)
        {
            int newBitmap = getBitmap();
            Object[] newPairs = null;  // Allocated only when an entry changes.
            int j = 0;                 // Result index

            // Loop through kvPairs, and when an entry changes,
            // start copying entries from kvPairs[i] to newPairs[j].

            for (int i = 0, bit = 1; i < kvPairs.length; i += 2, bit = bit << 1)
            {
                Object keyOrNull = kvPairs[i];
                Object storedValue = kvPairs[i + 1];

                if (storedValue == null) continue;

                Object newValue;
                if (keyOrNull == null)
                {
                    // This child node will invoke Changes; don't do it below.
                    TrieNode<K, V> node = (TrieNode<K, V>) storedValue;
                    newValue = node.transform(xform, changes);
                    if (newValue == EmptyNode.SINGLETON)
                    {
                        newValue = NOTHING;
                    }
                    // TODO: If node has one entry, hoist it and drop the node.
                }
                else
                {
                    newValue = xform.apply(keyOrNull, storedValue);
                }

                if (newValue == NOTHING)
                {
                    // Turn off the bit for this entry.
                    newBitmap = newBitmap ^ bit;
                    if (newPairs == null)
                    {
                        newPairs = new Object[kvPairs.length - 2];
                        System.arraycopy(kvPairs, 0, newPairs, 0, j);
                    }
                    if (keyOrNull != null)
                    {
                        changes.keyRemoved(keyOrNull, storedValue);
                    }
                }
                else // keeping or modifying normal entry
                {
                    if (newPairs == null)
                    {
                        if (newValue != storedValue)
                        {
                            if (keyOrNull != null)
                            {
                                changes.keyReplaced(keyOrNull, storedValue, newValue);
                            }
                            newPairs = cloneAndModify(kvPairs, i + 1, newValue);
                        }
                        j += 2;
                    }
                    else
                    {
                        if (newValue != storedValue && keyOrNull != null)
                        {
                            changes.keyReplaced(keyOrNull, storedValue, newValue);
                        }
                        newPairs[j++] = keyOrNull;
                        newPairs[j++] = newValue;
                    }
                }
            }

            if (newPairs == null) return this;

            if (j == 0) return empty();

            if (j != newPairs.length)
            {
                // We need to drop unused entries from the end.
                newPairs = Arrays.copyOf(newPairs, j, Object[].class);
            }

            return makeSimilar(newBitmap, newPairs);
        }
    }


    /**
     * A trie node storing a small number of unsorted key/value pairs, and no child nodes except
     * to hold colliding keys.
     * This is more efficient than a {@link BitMappedNode} for holding leaves:
     * empirically, a linear search is faster for nodes with <= {@value #MAX_CHILDREN} keys.
     * <p>
     * Within the {@link #kvPairs} array, keys are stored in even indices, values at odd.
     * Where there is a null key, the following value is a {@link CollisionNode}.
     * </p>
     * <p>
     * Nodes of this type are created by inserting into an {@link EmptyNode}, or by inserting a
     * non-colliding key into a {@code CollisionNode}.
     * They may grow to hold up to {@value #MAX_CHILDREN} entries before {@link #expand}ing
     * into a {@link BitMappedNode}.
     * </p>
     */
    static class FlatNode<K, V>
        extends KvNode<K, V>
    {
        /**
         * Empirically, a linear search performs as fast or faster than a bitmapped
         * search, up to this many elements.
         */
        static final int MAX_CHILDREN = 8;


        FlatNode()
        {
        }

        FlatNode(Object... kvPairs)
        {
            super(kvPairs);
        }


        FlatNode<K, V> makeSimilar(Object[] kvPairs)
        {
            return new FlatNode<>(kvPairs);
        }

        @Override
        FlatNode<K, V> makeSimilar(int bitmap, Object[] kvPairs)
        {
            return makeSimilar(kvPairs);
        }


        @Override
        public String toString()
        {
            return "FlatNode::" + Arrays.toString(kvPairs);
        }

        @Override
        final int getBitmap()
        {
            return 0;
        }

        int linearSearch(int hash, K key)
        {
            for (int i = 0; i < kvPairs.length; i += 2)
            {
                Object keyOrNull = kvPairs[i];
                if (keyOrNull == null)
                {
                    CollisionNode<K, V> node = (CollisionNode<K, V>) kvPairs[i + 1];
                    if (node.hash == hash)
                    {
                        return i;
                    }
                }
                else if (equivKeys(keyOrNull, key))
                {
                    return i;
                }
            }
            return -1;
        }


        @Override
        V get(int hash, int shift, K key)
        {
            int index = linearSearch(hash, key);
            if (index == -1)
            {
                return null;
            }

            Object keyOrNull = kvPairs[index];
            Object valOrNode = kvPairs[index + 1];
            if (keyOrNull == null)
            {
                return ((CollisionNode<K, V>) valOrNode).get(hash, shift, key);
            }
            else
            {
                return (V) valOrNode;
            }
        }


        @Override
        TrieNode<K, V> with(int hash,
                            int shift,
                            K key,
                            V value,
                            Changes changes)
        {
            int index = linearSearch(hash, key);
            if (index != -1)
            {
                Object keyOrNull = kvPairs[index];
                Object valOrNode = kvPairs[index + 1];
                if (keyOrNull == null)
                {
                    CollisionNode<K, V> node = (CollisionNode<K, V>) valOrNode;
                    TrieNode<K, V> newNode = node.with(hash, shift, key, value, changes);
                    if (newNode == node)
                    {
                        return this;
                    }
                    else
                    {
                        // Child node has updated the Changes.
                        return replaceValue(index, newNode);
                    }
                }
                else
                {
                    Object storedValue = valOrNode;
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue == newValue)
                    {
                        return this;
                    }
                    else // Same key, different value
                    {
                        changes.keyReplaced(keyOrNull, storedValue, newValue);
                        return replaceValue(index, newValue);
                    }
                }
            }
            else // Adding a new child
            {
                if (kvPairs.length >= 2 * MAX_CHILDREN)
                {
                    return expand(shift).mWith(hash, shift, key, value, changes);
                }
                else
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    Object[] newPairs = cloneAndExtend(kvPairs, key, newValue);
                    return new FlatNode<>(newPairs);
                }
            }
        }


        @Override
        TrieNode<K, V> mWith(int hash,
                             int shift,
                             K key,
                             V value,
                             Changes changes)
        {
            int index = linearSearch(hash, key);
            if (index != -1)
            {
                Object keyOrNull = kvPairs[index];
                Object valOrNode = kvPairs[index + 1];
                if (keyOrNull == null)
                {
                    CollisionNode<K, V> node = (CollisionNode<K, V>) valOrNode;
                    TrieNode<K, V> newNode = node.mWith(hash, shift, key, value, changes);
                    if (newNode != node)
                    {
                        kvPairs[index + 1] = newNode;
                    }
                    // Child node has updated the Changes.
                }
                else
                {
                    Object storedValue = valOrNode;
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue != newValue)
                    {
                        changes.keyReplaced(keyOrNull, storedValue, newValue);
                        kvPairs[index + 1] = newValue;
                    }
                }
            }
            else
            {
                if (kvPairs.length >= 2 * MAX_CHILDREN)
                {
                    return expand(shift).mWith(hash, shift, key, value, changes);
                }
                else
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    kvPairs = cloneAndExtend(kvPairs, key, newValue);
                }
            }

            return this;
        }


        @Override
        TrieNode<K, V> without(int hash,
                               int shift,
                               K key,
                               Changes changes)
        {
            int keyIndex = linearSearch(hash, key);
            if (keyIndex == -1)
            {
                return this;
            }

            Object keyOrNull = kvPairs[keyIndex];
            Object valOrNode = kvPairs[keyIndex + 1];
            if (keyOrNull == null)
            {
                CollisionNode<K, V> node = (CollisionNode<K, V>) valOrNode;
                TrieNode<K, V> newNode = node.without(hash, shift, key, changes);
                if (newNode == node)
                {
                    return this;
                }
                else if (newNode != EmptyNode.SINGLETON)
                {
                    return replaceValue(keyIndex, newNode);
                }
                else
                {
                    return withoutPair(keyIndex);
                }
            }
            else
            {
                changes.keyRemoved(keyOrNull, valOrNode);
                return withoutPair(keyIndex);
            }
        }


        FlatNode<K, V> replaceValue(int keyIndex, Object value)
        {
            Object[] newPairs = cloneAndModify(kvPairs, keyIndex + 1, value);
            return makeSimilar(newPairs);
        }


        TrieNode<K, V> withoutPair(int index)
        {
            if (kvPairs.length == 2)
            {
                return empty();
            }
            else
            {
                return makeSimilar(cloneAndRemovePair(kvPairs, index));
            }
        }

        private TrieNode<K, V> expand(int shift)
        {
            // Don't count expansion as changing the results.
            Changes changes = new Changes();

            // Give the new BitMappedNode some buffer space for faster mWith calls.
            // Supposing perfect hash distribution within the FlatNode, the new BitMappedNode's
            // array would require four extra allocations to perform expansion.
            // In the degenerate case of extremely similar hashes, this is a slight waste of memory.

            // TODO There's no point in overallocating unless this is called from mWith().

            TrieNode<K, V> newNode = new BitMappedNode<>(0, new Object[8]);
            for (int i = 0; i < kvPairs.length; i += 2)
            {
                Object keyOrNull = kvPairs[i];
                Object valOrNode = kvPairs[i + 1];
                if (keyOrNull == null)
                {
                    newNode = newNode.mWith(((CollisionNode<K, V>) valOrNode).hash,
                                            shift,
                                            null,
                                            (V) valOrNode, // inaccurate, but required cast
                                            changes);
                }
                else
                {
                    newNode = newNode.mWith(hashCodeFor(keyOrNull),
                                            shift,
                                            (K) keyOrNull,
                                            (V) valOrNode,
                                            changes);
                }
            }
            return newNode;
        }
    }


    /**
     * A trie node storing key/value pairs, where all keys have the same hashcode.
     * <p>
     * Like {@link FlatNode}, the entries are not sorted, so keys are found
     * via linear search and compared using {@link #equivKeys}.
     * Unlike {@code FlatNode}, this node never has child nodes since there are
     * no more hashcode bits to use.
     * </p>
     * <p>
     * Within the {@link #kvPairs} array, keys are stored in even indices, values at odd.
     * The array is always "full", with each pair holding key/value pairs.
     * Collisions are expected to be rare, so we optimize for space.
     * </p>
     * <p>
     * Nodes of this type are created when inserting a key into a {@link BitMappedNode}
     * that holds a different key (per {@link #equivKeys}) with the same hashcode.
     * They are replaced by a normal {@code FlatNode} when inserting a key with
     * a different hashcode, pushing the collisions down a level in the trie.
     * They are not eliminated when they shrink to one entry (and thus without a collision);
     * this is suboptimal and should be corrected.
     * </p>
     */
    static final class CollisionNode<K, V>
        extends FlatNode<K, V>
    {
        private final int hash;

        CollisionNode(int hash, Object[] kvPairs)
        {
            super(kvPairs);
            this.hash = hash;
        }


        @Override
        FlatNode<K, V> makeSimilar(Object[] kvPairs)
        {
            return new CollisionNode<>(hash, kvPairs);
        }


        @Override
        public String toString()
        {
            return "CollisionNode::{hash:" + hash +
                    ", kvPairs:" + Arrays.toString(kvPairs) + "}";
        }


        @Override
        public int countKeys()
        {
            return kvPairs.length / 2;
        }


        /**
         * Optimized linear search since all hashes are the same.
         */
        @Override
        int linearSearch(int hash, Object key)
        {
            for (int i = 0; i < kvPairs.length; i += 2)
            {
                Object keyOrNull = kvPairs[i];
                if (equivKeys(keyOrNull, key))
                {
                    return i;
                }
            }
            return -1;
        }


        @Override
        TrieNode<K, V> with(int hash,
                            int shift,
                            K key,
                            V value,
                            Changes changes)
        {
            if (hash == this.hash)
            {
                int index = linearSearch(hash, key);
                if (index != -1)
                {
                    Object storedValue = kvPairs[index + 1];
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue == newValue)
                    {
                        return this;
                    }
                    else
                    {
                        changes.keyReplaced(kvPairs[index], storedValue, newValue);
                        return replaceValue(index, newValue);
                    }
                }
                else
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    Object[] newPairs = cloneAndExtend(kvPairs, key, newValue);
                    return makeSimilar(newPairs);
                }
            }

            // If it doesn't share the hash, push this collision node down a level.
            Object newValue = changes.inserting(value);
            changes.keyAdded(key, newValue);
            return new FlatNode<>(null, this, key, newValue);
        }


        @Override
        TrieNode<K, V> mWith(int hash,
                             int shift,
                             K key,
                             V value,
                             Changes changes)
        {
            if (hash == this.hash)
            {
                int index = linearSearch(hash, key);
                if (index != -1)
                {
                    Object storedValue = kvPairs[index + 1];
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue != newValue)
                    {
                        changes.keyReplaced(kvPairs[index], storedValue, newValue);
                        kvPairs[index + 1] = newValue;
                    }
                }
                else
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    kvPairs = cloneAndExtend(kvPairs, key, newValue);
                }

                return this;
            }

            // If it doesn't share the hash, push this collision node down a level.
            Object newValue = changes.inserting(value);
            changes.keyAdded(key, newValue);
            return new FlatNode<>(null, this, key, newValue);
        }
    }


    /**
     * A trie node storing a mix of key/value pairs and child nodes.
     * <p>
     * Within the {@link #kvPairs} array, keys are stored in even indices, values at odd.
     * Where there is a null key, the following value is either a child {@link TrieNode}
     * or null. The latter occurs only at the high end of the array, and results from
     * optimizations in {@link #mWith} and {@link FlatNode#expand} intended to reduce
     * allocations while building a trie.
     * </p>
     * <p>
     * Nodes of this type are created when a {@link FlatNode} grows beyond its performant capacity
     * of {@value FlatNode#MAX_CHILDREN} entries.
     * They may grow to hold up to {@value #MAX_CHILDREN} entries before being replaced by a
     * {@link HashArrayMappedNode}.
     * They are never packed back down into {@code FlatNode}s because they may have child nodes
     * that are not {@link CollisionNode}s (which would break {@code FlatNode}'s ability to
     * linear search).
     * </p>
     */
    static class BitMappedNode<K, V>
        extends KvNode<K, V>
    {
        /**
         * We convert {@link BitMappedNode}s to {@link HashArrayMappedNode}s when they exceed
         * this many elements. Empirically, this conversion provides a performance gain of up to
         * 20% on lookup and insert operations as the trie grows with randomly distributed keys.
         */
        static final int MAX_CHILDREN = 16;

        int bitmap;


        BitMappedNode()
        {
            bitmap  = 0;
        }

        BitMappedNode(int bitmap, Object[] kvPairs)
        {
            super(kvPairs);
            this.bitmap = bitmap;
        }

        @Override
        BitMappedNode<K, V> makeSimilar(int bitmap, Object[] kvPairs)
        {
            return new BitMappedNode<>(bitmap, kvPairs);
        }


        @Override
        public String toString()
        {
            return "BitMappedNode::" + Arrays.toString(kvPairs);
        }

        @Override
        int getBitmap()
        {
            return bitmap;
        }

        /**
         * The index in the array corresponds to the number of bits "below" this one.
         */
        private int arrayIndex(int bit)
        {
            return Integer.bitCount(bitmap & (bit - 1));
        }

        private boolean isInBitmap(int bit)
        {
            return (bitmap & bit) != 0;
        }


        @Override
        V get(int hash, int shift, K key)
        {
            int hashFragment = hashFragment(hash, shift);
            int bit = bitPosition(hashFragment);
            if (!isInBitmap(bit))
            {
                return null;
            }

            int keyIndex = 2 * arrayIndex(bit);
            Object keyOrNull = kvPairs[keyIndex];
            Object valOrNode = kvPairs[keyIndex + 1];
            if (keyOrNull == null)
            {
                return ((TrieNode<K, V>) valOrNode).get(hash, shift + 5, key);
            }
            else if (equivKeys(keyOrNull, key))
            {
                return (V) valOrNode;
            }
            else
            {
                return null;
            }
        }


        @Override
        TrieNode<K, V> with(int hash,
                            int shift,
                            K key,
                            V value,
                            Changes changes)
        {
            int hashFragment = hashFragment(hash, shift);
            int bit = bitPosition(hashFragment);
            int keyIndex = 2 * arrayIndex(bit);
            if (isInBitmap(bit))
            {
                Object keyOrNull = kvPairs[keyIndex];
                Object valOrNode = kvPairs[keyIndex + 1];
                if (keyOrNull == null)
                {
                    TrieNode<K, V> node = (TrieNode<K, V>) valOrNode;
                    TrieNode<K, V> newNode = node.with(hash, shift + 5, key, value, changes);
                    if (newNode == node)
                    {
                        return this;
                    }
                    else
                    {
                        // Child node has updated the Changes.
                        return replaceValue(keyIndex, newNode);
                    }
                }
                else if (equivKeys(keyOrNull, key))
                {
                    Object storedValue = valOrNode;
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue == newValue)
                    {
                        return this;
                    }
                    else // Same key, different value
                    {
                        changes.keyReplaced(keyOrNull, storedValue, newValue);
                        return replaceValue(keyIndex, newValue);
                    }
                }
                else // Hash fragment collision
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    FlatNode<K, V> newNode = resolveCollision(hashCodeFor(keyOrNull),
                                                              (K) keyOrNull,
                                                              (V) valOrNode,
                                                              hash,
                                                              key,
                                                              (V) newValue);
                    return replacePair(keyIndex, null, newNode);
                }
            }
            else // Adding a new child
            {
                Object newValue = changes.inserting(value);
                changes.keyAdded(key, newValue);
                int numVals = Integer.bitCount(bitmap);
                if (numVals >= MAX_CHILDREN)
                {
                    return expand(key, (V) newValue, hashFragment, numVals);
                }
                else
                {
                    Object[] newPairs = cloneExtendAndInsert(kvPairs,
                                                              numVals,
                                                              2 * (numVals + 1),
                                                              keyIndex,
                                                              key,
                                                              newValue);
                    return new BitMappedNode<>(bitmap | bit, newPairs);
                }
            }
        }


        /**
         * {@inheritDoc}
         *
         * In addition to mutating the trie, mWith allocates some extra space
         * to the {@link BitMappedNode} so that it can add nodes without needing
         * to expand the array as much.
         */
        @Override
        TrieNode<K, V> mWith(int hash,
                             int shift,
                             K key,
                             V value,
                             Changes changes)
        {
            int hashFragment = hashFragment(hash, shift);
            int bit = bitPosition(hashFragment);
            int keyIndex = 2 * arrayIndex(bit);
            if (isInBitmap(bit))
            {
                Object keyOrNull = kvPairs[keyIndex];
                Object valOrNode = kvPairs[keyIndex + 1];
                if (keyOrNull == null)
                {
                    TrieNode<K, V> node = (TrieNode<K, V>) valOrNode;
                    TrieNode<K, V> newNode = node.mWith(hash, shift + 5, key, value, changes);
                    if (newNode != node)
                    {
                        kvPairs[keyIndex + 1] = newNode;
                    }
                    // Child node has updated the Changes.
                }
                else if (equivKeys(keyOrNull, key))
                {
                    Object storedValue = valOrNode;
                    Object newValue = changes.replacing(storedValue, value);
                    if (storedValue != newValue)
                    {
                        changes.keyReplaced(keyOrNull, storedValue, newValue);
                        kvPairs[keyIndex + 1] = newValue;
                    }
                }
                else // Hash fragment collision
                {
                    Object newValue = changes.inserting(value);
                    changes.keyAdded(key, newValue);
                    TrieNode<K, V> newNode = resolveCollision(hashCodeFor(keyOrNull),
                                                              (K) keyOrNull,
                                                              (V) valOrNode,
                                                              hash,
                                                              key,
                                                              (V) newValue);
                    kvPairs[keyIndex] = null;
                    kvPairs[keyIndex + 1] = newNode;
                }
            }
            else // New hash fragment
            {
                Object newValue = changes.inserting(value);
                changes.keyAdded(key, newValue);
                int numVals = Integer.bitCount(bitmap);
                if (numVals >= MAX_CHILDREN)
                {
                    return expand(key, newValue, hashFragment, numVals);
                }
                else if (numVals * 2 < kvPairs.length)
                {
                    // There's extra room in our kvPairs array, so shift things
                    // over to make room for the new entry.
                    System.arraycopy(kvPairs, keyIndex, kvPairs, keyIndex + 2, 2 * numVals - keyIndex);
                    kvPairs[keyIndex] = key;
                    kvPairs[keyIndex + 1] = newValue;
                    bitmap |= bit;
                }
                else
                {
                    // Give the new array some extra space.
                    kvPairs = cloneExtendAndInsert(kvPairs,
                                                   numVals,
                                                   2 * (numVals + 2),
                                                   keyIndex,
                                                   key,
                                                   newValue);
                    bitmap |= bit;
                }
            }

            return this;
        }


        @Override
        TrieNode<K, V> without(int hash, int shift, K key, Changes changes)
        {
            int hashFragment = hashFragment(hash, shift);
            int bit = bitPosition(hashFragment);
            if (!isInBitmap(bit))
            {
                return this;
            }

            int keyIndex = 2 * arrayIndex(bit);
            Object keyOrNull = kvPairs[keyIndex];
            Object valOrNode = kvPairs[keyIndex + 1];
            if (keyOrNull == null)
            {
                TrieNode<K, V> node = (TrieNode<K, V>) valOrNode;
                TrieNode<K, V> newNode = node.without(hash, shift + 5, key, changes);
                if (newNode == node)
                {
                    return this;
                }
                else if (newNode != EmptyNode.SINGLETON)
                {
                    return replaceValue(keyIndex, newNode);
                }
                else
                {
                    return withoutPair(bit, keyIndex);
                }
            }
            else if (equivKeys(keyOrNull, key))
            {
                changes.keyRemoved(keyOrNull, valOrNode);
                return withoutPair(bit, keyIndex);
            }
            else
            {
                return this;
            }
        }

        private TrieNode<K, V> withoutPair(int bit, int keyIndex)
        {
            if (bitmap == bit)
            {
                return empty();
            }
            else
            {
                Object[] newPairs = cloneAndRemovePair(kvPairs, keyIndex);
                return new BitMappedNode<>(bitmap ^ bit, newPairs);
            }
        }


        private BitMappedNode<K, V> replaceValue(int keyIndex, Object value)
        {
            Object[] newPairs = cloneAndModify(kvPairs, keyIndex + 1, value);
            return new BitMappedNode<>(bitmap, newPairs);
        }

        private BitMappedNode<K, V> replacePair(int keyIndex,
                                                Object key,
                                                Object value)
        {
            Object[] newPairs =
                cloneAndModify(kvPairs, keyIndex, key, keyIndex + 1, value);
            return new BitMappedNode<>(bitmap, newPairs);
        }


        private HashArrayMappedNode<K, V> expand(K key,
                                                 Object value,
                                                 int hashFragment,
                                                 int numVals)
        {
            TrieNode<K, V>[] nodes = new TrieNode[32];
            nodes[hashFragment] = new FlatNode<>(key, value);

            int j = 0;
            for (int i = 0; i < 32; i++)
            {
                if (isInBitmap(1 << i))
                {
                    Object keyOrNull = kvPairs[j];
                    Object valOrNode = kvPairs[j + 1];
                    if (keyOrNull == null)
                    {
                        nodes[i] = (TrieNode<K, V>) valOrNode;
                    }
                    else
                    {
                        nodes[i] = new FlatNode<>((K) keyOrNull, (V) valOrNode);
                    }
                    j += 2;
                }
            }
            return new HashArrayMappedNode<>(numVals + 1, nodes);
        }


        /**
         * Used for resolving hashFragment collisions within {@link BitMappedNode}s.
         *
         * Keys with hashes that are exactly the same will be placed into a {@link CollisionNode},
         * while keys with hashes that aren't exactly the same will be nested in a {@link FlatNode}.
         */
        private FlatNode<K, V> resolveCollision(int key1hash, K key1, V value1,
                                                int key2hash, K key2, V value2)
        {
            if (key1hash == key2hash)
            {
                return new CollisionNode<>(key1hash, new Object[]{key1, value1, key2, value2});
            }
            else
            {
                return new FlatNode<>(key1, value1, key2, value2);
            }
        }
    }


    /**
     * A trie node storing only child nodes, and no direct key/value pairs.
     * Storage is always full-width, so there's no bitmap to track which entries are present.
     * <p>
     * Nodes of this type are created when a {@link BitMappedNode} grows to contain more than
     * {@value BitMappedNode#MAX_CHILDREN} children.
     * Conversely, when they shrink to contain fewer than {@value #MIN_CHILDREN} child nodes,
     * they are packed back into a {@code BitMappedNode}.
     * </p>
     */
    static class HashArrayMappedNode<K, V>
        extends TrieNode<K, V>
    {
        static final int MIN_CHILDREN = 8;
        static final int MAX_CHILDREN = 32;

        /**
         * count refers to the number of non-null nodes in {@link #nodes},
         * not the size of the subtrie.
         */
        private int count;
        final TrieNode<K, V>[] nodes;

        HashArrayMappedNode()
        {
            count = 0;
            nodes = new TrieNode[MAX_CHILDREN];
        }

        HashArrayMappedNode(int count, TrieNode<K, V>[] nodes)
        {
            this.count = count;
            this.nodes = nodes;
            assert nodes.length == MAX_CHILDREN;
        }


        @Override
        public String toString()
        {
            return "HamNode::" + Arrays.toString(nodes);
        }


        @Override
        public int countKeys()
        {
            int count = 0;
            for (TrieNode<K, V> node : nodes)
            {
                if (node != null) count += node.countKeys();
            }
            return count;
        }


        @Override
        public Iterator<Entry<K, V>> iterator()
        {
            return new NodeArrayIterator<>(nodes);
        }


        @Override
        public V get(int hash, int shift, K key)
        {
            int index = hashFragment(hash, shift);
            TrieNode<K, V> node = nodes[index];
            if (node == null)
            {
                return null;
            }
            else
            {
                return node.get(hash, shift + 5, key);
            }
        }


        @Override
        public TrieNode<K, V> with(int hash,
                                   int shift,
                                   K key,
                                   V value,
                                   Changes changes)
        {
            int index = hashFragment(hash, shift);
            TrieNode<K, V> node = nodes[index];

            if (node == null)
            {
                Object newValue = changes.inserting(value);
                changes.keyAdded(key, newValue);
                TrieNode<K, V> newNode = new FlatNode<>(key, (V) newValue);
                return replace(count + 1, index, newNode);
            }
            else
            {
                TrieNode<K, V> newNode = node.with(hash, shift + 5, key, value, changes);
                if (newNode == node)
                {
                    return this;
                }
                else
                {
                    // Child node has updated the Changes.
                    return replace(count, index, newNode);
                }
            }
        }


        @Override
        TrieNode<K, V> mWith(int hash,
                             int shift,
                             K key,
                             V value,
                             Changes changes)
        {
            int index = hashFragment(hash, shift);
            TrieNode<K, V> node = nodes[index];

            if (node == null)
            {
                Object newValue = changes.inserting(value);
                changes.keyAdded(key, newValue);
                TrieNode<K, V> newNode = new FlatNode<>(key, newValue);
                nodes[index] = newNode;
                count++;
            }
            else
            {
                TrieNode<K, V> newNode = node.mWith(hash, shift + 5, key, value, changes);
                if (newNode != node)
                {
                    nodes[index] = newNode;
                }
                // Child node has updated the Changes.
            }

            return this;
        }


        @Override
        public TrieNode<K, V> without(int hash, int shift, K key, Changes changes)
        {
            int index = hashFragment(hash, shift);
            TrieNode<K, V> node = nodes[index];
            if (node == null)
            {
                return this;
            }

            // Child node updates Changes
            TrieNode<K, V> newNode = node.without(hash, shift + 5, key, changes);
            if (newNode == node)
            {
                return this;
            }
            else if (newNode != EmptyNode.SINGLETON)
            {
                return replace(count, index, newNode);
            }
            else if (count <= MIN_CHILDREN)
            {
                return pack(count - 1, nodes, index);
            }
            else
            {
                return replace(count - 1, index, null);
            }
        }


        @Override
        public TrieNode<K, V> transform(BiFunction xform, Changes changes)
        {
            int newCount = count;
            TrieNode<K, V>[] newNodes = null;

            for (int i = 0; i < nodes.length; i++)
            {
                TrieNode<K, V> child = nodes[i];
                if (child != null)
                {
                    // No need to invoke Changes here since there are no direct
                    // key-values entries to be changed.
                    TrieNode<K, V> newChild = child.transform(xform, changes);
                    if (newChild != child)
                    {
                        if (newNodes == null)
                        {
                            newNodes = nodes.clone();
                        }

                        if (newChild == EmptyNode.SINGLETON)
                        {
                            newNodes[i] = null;
                            newCount--;
                        }
                        else
                        {
                            newNodes[i] = newChild;
                        }
                    }
                }
            }

            if (newNodes == null) return this;

            if (newCount == 0) return empty();

            if (newCount < MIN_CHILDREN) return pack(newCount, newNodes, -1);

            return new HashArrayMappedNode<>(newCount, newNodes);
        }


        private TrieNode<K, V> replace(int count, int index, TrieNode<K, V> value)
        {
            TrieNode<K, V>[] newPairs = cloneAndModify(nodes, index, value);
            return new HashArrayMappedNode<>(count, newPairs);
        }


        /**
         * Pack the given nodes into a {@link BitMappedNode}, optionally
         * removing one entry.
         *
         * @param removeIndex can be <0 to avoid removing anything.
         */
        private BitMappedNode<K, V> pack(int count, TrieNode<K, V>[] nodes, int removeIndex)
        {
            Object[] kvPairs = new Object[2 * count];
            int j = 1;
            int bitmap = 0;
            for (int i = 0; i < nodes.length; i++)
            {
                if (i != removeIndex && nodes[i] != null)
                {
                    kvPairs[j] = nodes[i];
                    bitmap |= 1 << i;
                    j += 2;
                }
            }
            assert Integer.bitCount(bitmap) == count;
            return new BitMappedNode<>(bitmap, kvPairs);
        }
    }


    //==========================================================================
    // Utility methods for node classes.

    /**
     * In case key comparison needs to change in the future.
     * @return true if keys are equal (however we define that), false otherwise.
     */
    private static <K> boolean equivKeys(K key1, K key2)
    {
        return key1 == key2 || key1.equals(key2);
    }


    private static Object[] cloneAndExtend(Object[] array,
                                           Object key,
                                           Object value)
    {
        int len = array.length;
        Object[] newArray = Arrays.copyOf(array, len + 2, Object[].class);
        newArray[len] = key;
        newArray[len + 1] = value;
        return newArray;
    }

    private static Object[] cloneExtendAndInsert(Object[] array,
                                                 int oldNumVals,
                                                 int newLen,
                                                 int keyIndex,
                                                 Object key,
                                                 Object value)
    {
        Object[] newArray = Arrays.copyOf(array, newLen, Object[].class);
        newArray[keyIndex] = key;
        newArray[keyIndex + 1] = value;
        System.arraycopy(array, keyIndex, newArray, keyIndex + 2, 2 * oldNumVals - keyIndex);
        return newArray;
    }

    private static <K, V> TrieNode<K, V>[] cloneAndModify(TrieNode<K, V>[] array,
                                                          int i,
                                                          TrieNode<K, V> a)
    {
        TrieNode<K, V>[] clone = array.clone();
        clone[i] = a;
        return clone;
    }

    private static Object[] cloneAndModify(Object[] array, int i, Object a)
    {
        Object[] clone = array.clone();
        clone[i] = a;
        return clone;
    }

    private static Object[] cloneAndModify(Object[] array,
                                           int i,
                                           Object a,
                                           int j,
                                           Object b)
    {
        Object[] clone = array.clone();
        clone[i] = a;
        clone[j] = b;
        return clone;
    }

    private static Object[] cloneAndRemovePair(Object[] array, int keyIndex)
    {
        Object[] clone = Arrays.copyOf(array, array.length - 2, Object[].class);
        System.arraycopy(array, keyIndex + 2, clone, keyIndex, clone.length - keyIndex);
        return clone;
    }

}
