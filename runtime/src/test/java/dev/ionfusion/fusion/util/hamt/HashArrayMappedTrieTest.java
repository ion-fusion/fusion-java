// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.empty;
import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.fromArrays;
import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.fromSelectedKeys;
import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.hashCodeFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.BitMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.Changes;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.CollisionNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.FlatNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.HashArrayMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HashArrayMappedTrieTest
{
    /**
     * Wraps every value in our HAMT to test invocation of Results.
     * @param <V>
     */
    private static class Wrapper<V>
    {
        final V target;

        Wrapper(V target)
        {
            this.target = target;
        }
    }


    private static void assertEmpty(TrieNode node)
    {
        assertSame(empty(), node);
    }

    private static <K, V> V assertValuePresent(TrieNode<K, V> node, K key)
    {
        Wrapper<V> found = (Wrapper<V>) node.get(key);
        assertNotNull(found);
        return found.target;
    }

    private static <K, V> void assertValueAbsent(TrieNode<K, V> node, K key)
    {
        if (node != null)
        {
            V found = node.get(key);
            assertNull(found);
        }
    }

    private static <K, V> void assertValueEquals(V expectedValue, TrieNode<K, V> node, K key)
    {
        V found = assertValuePresent(node, key);
        assertEquals(expectedValue, found);
    }

    /**
     * Helper for writing fluent tests.
     */
    private static class Modifier<K, V>
    {
        private enum Mode { INSERT, REPLACE, REMOVE, NOOP }

        private final TrieNode<K, V> oldNode;
        private final int            oldSize;
        private final CheckingChanges changes = new CheckingChanges();

        private K              key;
        private V              oldValue;
        private V              newValue;
        private TrieNode<K, V> newNode;
        private Mode           mode;

        public Modifier(TrieNode<K, V> node)
        {
            oldNode = node;
            oldSize = node.countKeys();
        }


        private void recordKey(K key, V value)
        {
            this.key = key;

            Wrapper<V> wrapper = (Wrapper<V>) oldNode.get(key);
            oldValue = (wrapper == null ? null : wrapper.target);

            newValue = value;
        }

        public Modifier<K, V> with(K key, V value)
        {
            recordKey(key, value);

            newNode = oldNode.with(key, value, changes);
            assertTrue(changes.wrapped, "Changes not invoked");
            assertValueEquals(value, newNode, key);
            assertRootReplacementImpliesModification();

            return this;
        }

        public Modifier<K, V> mWith(K key, V value)
        {
            recordKey(key, value);

            newNode = oldNode.mWith(key, value, changes);
            assertTrue(changes.wrapped, "Changes not invoked");
            assertValueEquals(value, newNode, key);
            assertRootReplacementImpliesModification();

            return this;
        }

        public Modifier<K, V> without(K key)
        {
            recordKey(key, null);

            newNode = oldNode.without(key, changes);
            assertValueAbsent(newNode, key);
            assertRootReplacementImpliesModification();

            return this;
        }


        public Modifier<K, V> inserts()
        {
            // Precondition:
            assertNull(oldValue, "old value");

            mode = Mode.INSERT;
            changes.assertInserted();
            assertTrue(changes.changeCount() > 0, "no changes were made");
            assertSizeDelta(1);
            return this;
        }

        public Modifier<K, V> replaces()
        {
            // Preconditions:
            assertNotNull(oldValue, "old value");
            assertNotSame(oldValue, newValue, "mapped value");

            mode = Mode.REPLACE;
            changes.assertReplaced();
            assertTrue(changes.changeCount() > 0, "no changes were made");
            assertSizeDelta(0);
            return this;
        }

        public Modifier<K, V> removes()
        {
            // Precondition:
            assertNotNull(oldValue, "old value");

            mode = Mode.REMOVE;
            changes.assertRemoved();
            assertTrue(changes.changeCount() > 0, "no changes were made");
            assertSizeDelta(-1);
            return this;
        }

        public Modifier<K, V> noops()
        {
            // Precondition:
            assertSame(oldValue, newValue, "mapped value");

            mode = Mode.NOOP;
            assertEquals(0, changes.changeCount(), "unexpected changes were made");
            assertFalse(changes.inserted);
            assertFalse(changes.replaced);
            assertFalse(changes.removed);
            assertSizeDelta(0);
            return this;
        }


        public TrieNode<K, V> returnsNew() {
            assertNotSame(oldNode, newNode, "returned node");

            // Make sure the old node isn't changed.
            assertEquals(oldSize, oldNode.countKeys(), "old node size");

            // FIXME not correct in face of mutation.
            switch (mode) {
                case INSERT:
                    assertValueAbsent(oldNode, key);
                    break;
                case REMOVE:
                case REPLACE:
                case NOOP:
                    assertValueEquals(oldValue, oldNode, key);
                    break;
            }

            return newNode;
        }

        public TrieNode<K, V> returnsSame()
        {
            assertSame(oldNode, newNode);
            return newNode;
        }

        private void assertRootReplacementImpliesModification()
        {
            if (newNode != oldNode)
            {
                assertTrue(changes.changeCount() > 0, "no changes were made");
            }
        }

        private void assertSizeDelta(int delta)
        {
            assertEquals(delta, changes.keyCountDelta(), "size delta");
            assertEquals(oldSize + delta, newNode.countKeys(), "new size");
        }

        private class CheckingChanges
            extends Changes
        {
            boolean wrapped  = false;
            boolean inserted = false;
            boolean replaced = false;
            boolean removed  = false;

            @Override
            public Wrapper<V> inserting(Object givenValue)
            {
                wrapped = true;

                assertFalse(givenValue instanceof Wrapper, "wrapper shouldn't be given");
                assertSame(newValue, givenValue);
                return new Wrapper(givenValue);
            }

            @Override
            public Wrapper<V> replacing(Object storedValue, Object givenValue)
            {
                wrapped = true;
                Wrapper<V> wrapper = (Wrapper<V>) storedValue;
                assertSame(Modifier.this.oldValue, wrapper.target);
                assertSame(newValue, givenValue);

                if (givenValue == wrapper.target)
                {
                    // Don't replace an identical value.
                    return wrapper;
                }

                return new Wrapper(givenValue);
            }

            @Override
            public void keyAdded(Object key, Object newValue)
            {
                inserted = true;

                assertSame(Modifier.this.key, key);
                assertSame(Modifier.this.newValue, ((Wrapper<V>) newValue).target);

                super.keyAdded(key, newValue);
            }

            @Override
            protected void keyReplaced(Object key, Object oldValue, Object newValue)
            {
                replaced = true;

                assertEquals(Modifier.this.key, key);
                assertSame(Modifier.this.oldValue, ((Wrapper<V>) oldValue).target);
                assertSame(Modifier.this.newValue, ((Wrapper<V>) newValue).target);

                super.keyReplaced(key, oldValue, newValue);
            }

            @Override
            protected void keyRemoved(Object key, Object oldValue)
            {
                removed = true;

                assertEquals(Modifier.this.key, key);
                assertSame(Modifier.this.oldValue, ((Wrapper<V>) oldValue).target);

                super.keyRemoved(key, oldValue);
            }

            void assertInserted()
            {
                assertTrue(inserted, "Results.inserting() not invoked");
            }

            void assertReplaced()
            {
                assertTrue(replaced, "Results.replacing() not invoked");
            }

            void assertRemoved()
            {
                assertTrue(removed, "Results.removing() not invoked");
            }
        }
    }


    private static <K, V> Modifier<K, V> with(TrieNode<K,V> node, K key, V value)
    {
        return new Modifier(node).with(key, value);
    }

    private static <K, V> Modifier<K, V> mWith(TrieNode<K,V> node, K key, V value)
    {
        return new Modifier(node).mWith(key, value);
    }

    private static <K, V> Modifier<K, V> without(TrieNode<K,V> node, K key)
    {
        return new Modifier(node).without(key);
    }


    private <K, V> TrieNode<K, V> insert(TrieNode<K, V> node, K key, V value)
    {
        return with(node, key, value).inserts().returnsNew();
    }

    private <K, V> TrieNode<K, V> mInsert(TrieNode<K, V> node, K key, V value)
    {
        return mWith(node, key, value).inserts().returnsSame();
    }

    private <K, V> TrieNode<K, V> replace(TrieNode<K, V> node, K key, V value)
    {
        return with(node, key, value).replaces().returnsNew();
    }

    private <K, V> TrieNode<K, V> mReplace(TrieNode<K, V> node, K key, V value)
    {
        return mWith(node, key, value).replaces().returnsSame();
    }

    private <K, V> TrieNode<K, V> noopReplace(TrieNode<K, V> node, K key, V value)
    {
        return mWith(node, key, value).noops().returnsSame();
    }

    private <K, V> TrieNode<K, V> remove(TrieNode<K, V> node, K key)
    {
        return without(node, key).removes().returnsNew();
    }

    private <K, V> TrieNode<K, V> noopRemove(TrieNode<K, V> node, K key)
    {
        return without(node, key).noops().returnsSame();
    }


    //=========================================================================
    // EmptyNode

    @Test
    public void testEmptyNodeIsSingleton()
    {
        assertSame(empty(), empty());
    }

    @Test
    public void testEmptyNodeIsEmpty()
    {
        TrieNode<Object, Object> empty = empty();
        assertEquals(0, empty.countKeys());

        Iterator<Entry<Object, Object>> iterator = empty.iterator();
        assertFalse(iterator.hasNext(), "iterator isn't empty");
    }

    @Test
    public void testEmptyNodeInsert()
    {
        TrieNode node = empty();
        node = insert(node, 1, 1);
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testEmptyNodeMutatingInsert()
    {
        TrieNode node = empty();
        node = mWith(node, 1, 1).inserts().returnsNew();
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testEmptyNodeRemove()
    {
        TrieNode empty = empty();
        TrieNode node = noopRemove(empty, 1);
        assertSame(empty, node);
    }

    @Test
    public void emptyNodeWithoutKeysRequiresNonNullArray()
    {
        assertThrows(NullPointerException.class,
                     () -> empty().withoutKeys(null, new Changes()));
    }

    @Test
    public void emptyNodeWithoutKeysGivenNoKeysReturnsSame()
    {
        TrieNode trie = empty();
        assertSame(trie, trie.withoutKeys(new Object[0], new Changes()));
    }

    @Test
    public void emptyNodeWithoutKeysGivenKeysReturnsSame()
    {
        TrieNode trie = empty();
        assertSame(trie, trie.withoutKeys(new Object[]{ 1, 2 }, new Changes()));
    }


    //=========================================================================
    // FlatNode

    static <K, V> FlatNode<K, V> flatNodeForPairs(Object... kvPairs)
    {
        assertTrue(kvPairs.length < FlatNode.MAX_CHILDREN * 2,
                  "too many pairs");
        assertEquals(0, kvPairs.length % 2);

        for (int i = 0; i < kvPairs.length; i += 2)
        {
            V value = (V) kvPairs[i+1];
            kvPairs[i+1] = new Wrapper(value);
        }

        return new FlatNode<>(kvPairs);
    }

    @Test
    public void testFlatNodeInsertAndReplace()
    {
        TrieNode node = flatNodeForPairs(1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = replace(node, 1, "one");
        node = replace(node, 2, "two");
        node = replace(node, 3, "three");

        assertEquals(3, node.countKeys());
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testFlatNodeMutatingInsertAndReplace()
    {
        TrieNode node = flatNodeForPairs(1, 1);
        node = mInsert(node, 2, 2);
        node = mInsert(node, 3, 3);
        node = mReplace(node, 1, "one");
        node = mReplace(node, 2, "two");
        node = mReplace(node, 3, "three");

        assertEquals(3, node.countKeys());
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testFlatNodeRemove()
    {
        TrieNode node = flatNodeForPairs(1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = noopRemove(node, 4);
        node = remove(node, 2);
        node = remove(node, 1);
        node = remove(node, 3);
        node = noopRemove(node, 2);

        assertEmpty(node);
    }

    @Test
    public void testFlatNodeCollision()
    {
        CustomKey foo = new CustomKey("foo");
        CustomKey bar = foo.collide("bar");

        TrieNode node = flatNodeForPairs(foo, foo);
        node = insert(node, bar, bar);

        // FlatNode can handle collisions without pushing down a CollisionNode.
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testFlatNodeMutatingCollision()
    {
        CustomKey foo = new CustomKey("foo");
        CustomKey bar = foo.collide("bar");

        TrieNode node = flatNodeForPairs(foo, foo);
        node = mInsert(node, bar, bar);

        // FlatNode can handle collisions without pushing down a CollisionNode.
        assertEquals(FlatNode.class, node.getClass());
    }

    @Test
    public void testFlatNodeCollisionRemoval()
    {
        CustomKey foo1 = new CustomKey("foo1");
        CustomKey foo2 = foo1.collide("foo2");
        CustomKey foo3 = foo1.collide("foo3");
        CustomKey bar  = new CustomKey("bar");

        TrieNode node = collisionNodeForPairs(foo1, foo1, foo2, foo2);
        node = insert(node, bar, bar);
        assertEquals(FlatNode.class, node.getClass());

        node = noopRemove(node, foo3);
    }

    @Test
    public void checkFlatNodeExpansion()
    {
        List<Entry> values = new ArrayList<>();
        for (int i = 0; i <= FlatNode.MAX_CHILDREN; i++)
        {
            CustomKey key = new CustomKey(i, Integer.toString(i));
            Entry entry = new AbstractMap.SimpleEntry<>(key, new Object());
            values.add(entry);
        }

        TrieNode flatNode = new FlatNode();

        // A flat node should hold this many entries before expanding.
        for (int i = 0; i < FlatNode.MAX_CHILDREN; i++)
        {
            Entry entry = values.get(i);
            flatNode = insert(flatNode, entry.getKey(), entry.getValue());
            assertTrue(flatNode instanceof FlatNode);
        }

        Entry next = values.get(FlatNode.MAX_CHILDREN);
        TrieNode nowBitMap = insert(flatNode, next.getKey(), next.getValue());
        assertTrue(nowBitMap instanceof BitMappedNode);
        assertEquals(9, nowBitMap.countKeys());

        BitMappedNode bm = (BitMappedNode) nowBitMap;
        Object[] kvPairs = bm.kvPairs;
        // BMNodes will allocate some extra space during expansion since expansion uses the mWith() method.
        assertEquals(values.size() * 2 + 2, kvPairs.length);
        for (int i = 0; i < 9; i++)
        {
            Entry entry = values.get(i);
            boolean found = false;
            for (int k = 0; k < kvPairs.length; k += 2)
            {
                if (entry.getKey().equals(kvPairs[k]))
                {
                    assertFalse(found);
                    found = true;
                    Wrapper w = (Wrapper) kvPairs[k + 1];
                    assertSame(entry.getValue(), w.target);
                }
            }
        }
    }

    @Test
    public void checkFlatNodeMutatingExpansion()
    {
        int overflow = FlatNode.MAX_CHILDREN + 1;
        final FlatNode original = new FlatNode();
        TrieNode node = original;
        for (int i = 1; i < overflow; i++)
        {
            node = mInsert(node, i, i);
        }
        assertSame(original, node);

        TrieNode expanded = mWith(node, overflow, overflow).inserts().returnsNew();
        assertTrue(expanded instanceof BitMappedNode);
    }


    //=========================================================================
    // CollisionNode

    static <K, V> CollisionNode<K, V> collisionNodeForPairs(Object... kvPairs)
    {
        assertEquals(0, kvPairs.length % 2);
        int hash = hashCodeFor(kvPairs[0]);
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            assertEquals(hash, hashCodeFor(kvPairs[i]));
            V value = (V) kvPairs[i+1];
            kvPairs[i+1] = new Wrapper(value);
        }
        return new CollisionNode<>(hash, kvPairs);
    }

    @Test
    public void testCollisionNodeInsertAndReplace()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");
        CustomKey key3 = key1.collide("key3");

        TrieNode node = collisionNodeForPairs(key1, 1);
        node = insert(node, key2, 2);
        node = insert(node, key3, 3);
        node = replace(node, key1, "one");
        node = replace(node, key2, "two");
        node = replace(node, key3, "three");

        assertEquals(3, node.countKeys());
        assertEquals(CollisionNode.class, node.getClass());
    }

    @Test
    public void testCollisionNodeMutatingInsertAndReplace()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");
        CustomKey key3 = key1.collide("key3");

        TrieNode node = collisionNodeForPairs(key1, 1);
        node = mInsert(node, key2, 2);
        node = mInsert(node, key3, 3);
        node = mReplace(node, key1, "one");
        node = mReplace(node, key2, "two");
        node = mReplace(node, key3, "three");

        assertEquals(3, node.countKeys());
        assertEquals(CollisionNode.class, node.getClass());
    }

    @Test
    public void testCollisionNodeRemove()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");
        CustomKey key3 = key1.collide("key3");

        TrieNode node = collisionNodeForPairs(key1, 1);
        node = insert(node, key2, 2);
        node = insert(node, key3, 3);
        node = noopRemove(node, key1.collide("key4"));
        node = remove(node, key2);
        node = remove(node, key1);
        node = remove(node, key3);
        node = noopRemove(node, key2);

        assertEmpty(node);
    }

    @Test
    public void testCollisionNodeExpansion()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");

        TrieNode collisionNode = collisionNodeForPairs(key1, 1, key2, 2);

        CustomKey key3 = key1.nonCollide("key3");

        TrieNode withNewNonCollidingKey = insert(collisionNode, key3, 3);
        // We expect the collision node to have been pushed down a level.
        assertTrue(withNewNonCollidingKey instanceof FlatNode);

        assertValueEquals(1, withNewNonCollidingKey, key1);
        assertValueEquals(2, withNewNonCollidingKey, key2);
        assertValueEquals(3, withNewNonCollidingKey, key3);
    }

    @Test
    public void testCollisionNodeMutatingExpansion()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");
        TrieNode collisionNode = collisionNodeForPairs(key1, 1, key2, 2);

        CustomKey key3 = key1.nonCollide("key3");
        TrieNode withNewNonCollidingKey =
            mWith(collisionNode, key3, 3).inserts().returnsNew();

        // We expect the collision node to have been pushed down a level.
        assertTrue(withNewNonCollidingKey instanceof FlatNode);

        assertValueEquals(1, withNewNonCollidingKey, key1);
        assertValueEquals(2, withNewNonCollidingKey, key2);
        assertValueEquals(3, withNewNonCollidingKey, key3);
    }

    @Test
    public void checkCollisionNodeBehavior()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");
        TrieNode collisionNode = collisionNodeForPairs(key1, "foo", key2, "bar");

        CustomKey key3 = key1.nonCollide("key3");
        TrieNode withNewNonCollidingKey = insert(collisionNode, key3, "baz");

        // We expect the collision node to have been pushed down a level.
        assertTrue(withNewNonCollidingKey instanceof FlatNode);
        assertValueEquals("foo", withNewNonCollidingKey, key1);
        assertValueEquals("bar", withNewNonCollidingKey, key2);
        assertValueEquals("baz", withNewNonCollidingKey, key3);

        CustomKey key4 = key1.collide("key4");
        TrieNode withCollidingKey = insert(withNewNonCollidingKey, key4, "biz");
        assertValueEquals("biz", withCollidingKey, key4);

        assertValueEquals("foo", withNewNonCollidingKey, key1);
        TrieNode withoutKey1 = remove(withCollidingKey, key1);
        assertValueEquals("bar", withoutKey1, key2);
        assertValueEquals("baz", withoutKey1, key3);
        assertValueEquals("biz", withoutKey1, key4);

        TrieNode without1And2 = remove(withoutKey1, key2);
        assertValueEquals("baz", without1And2, key3);
        assertValueEquals("biz", withoutKey1,  key4);

        TrieNode without123 = remove(without1And2, key3);
        assertValueEquals("biz", without123, key4);

        TrieNode empty = remove(without123, key4);
        assertEmpty(empty);
    }


    @Test
    public void checkFlatNodeExpansionWithCollisionNode()
    {
        Object[] values = {"key1", "foo", "key2", "bar"};
        TrieNode trieNode = new CollisionNode(hashCodeFor(0), values);

        List<CustomKey> keys = new ArrayList<>(8);

        for (int i = 1; i < 9; i++)
        {
            CustomKey key = new CustomKey(i, Integer.toString(i));
            keys.add(key);
            trieNode = insert(trieNode, key, new Object());
        }

        // 10 because the nested collision node has 2 elements.
        assertEquals(10, trieNode.countKeys());

        assertTrue(trieNode instanceof BitMappedNode);
        BitMappedNode bitMappedNode = (BitMappedNode) trieNode;
        Object[] kvPairs = bitMappedNode.kvPairs;
        List<Object> kvList = Arrays.asList(kvPairs);
        for (Object key : keys)
        {
            assertTrue(kvList.contains(key));
        }

        int collisionNodeIndex = -1;
        for (int i = 0; i < kvPairs.length; i++)
        {
            if (kvPairs[i] instanceof CollisionNode)
            {
                collisionNodeIndex = i;
            }
        }

        assertNotEquals(-1, collisionNodeIndex, "CollisionNode does not exist within BitMappedNode");

        CollisionNode collisionNode = (CollisionNode) bitMappedNode.kvPairs[collisionNodeIndex];
        assertEquals("key1", collisionNode.kvPairs[0]);
        assertEquals("foo", collisionNode.kvPairs[1]);
        assertEquals("key2", collisionNode.kvPairs[2]);
        assertEquals("bar", collisionNode.kvPairs[3]);
    }


    //=========================================================================
    // BitMappedNode

    @Test
    public void testBitMappedNodeInsertAndReplace()
    {
        TrieNode node = new BitMappedNode(0, new Object[0]);
        node = insert(node, 1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = replace(node, 1, "one");
        node = replace(node, 2, "two");
        node = replace(node, 3, "three");

        assertEquals(BitMappedNode.class, node.getClass());
    }

    @Test
    public void testBitMappedNodeMutatingInsertAndReplace()
    {
        TrieNode node = new BitMappedNode(0, new Object[0]);
        node = mInsert(node, 1, 1);
        node = mInsert(node, 2, 2);
        node = mInsert(node, 3, 3);
        node = mReplace(node, 1, "one");
        node = mReplace(node, 2, "two");
        node = mReplace(node, 3, "three");

        assertEquals(BitMappedNode.class, node.getClass());
    }

    @Test
    public void testBitMappedNodeRemove()
    {
        TrieNode node = new BitMappedNode(0, new Object[0]);
        node = insert(node, 1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = noopRemove(node, 4);
        node = remove(node, 2);
        node = remove(node, 1);
        node = remove(node, 3);
        node = noopRemove(node, 2);

        assertEmpty(node);
    }

    @Test
    public void testBitMappedNodeCollision()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");

        TrieNode node = new BitMappedNode(0, new Object[0]);
        node = insert(node, key1, 1);
        node = insert(node, key2, 2);

        assertEquals(BitMappedNode.class, node.getClass());
    }

    @Test
    public void testBitMappedNodeMutatingCollision()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");

        TrieNode node = new BitMappedNode(0, new Object[0]);
        node = mInsert(node, key1, 1);
        node = mInsert(node, key2, 2);

        assertEquals(BitMappedNode.class, node.getClass());
    }

    @Test
    public void testBitMappedNodeExpansion()
    {
        int overflow = BitMappedNode.MAX_CHILDREN + 1;
        TrieNode node = new BitMappedNode(0, new Object[0]);
        for (int i = 1; i < overflow; i++)
        {
            node = insert(node, i, i);
        }
        assertEquals(BitMappedNode.class, node.getClass());

        node = insert(node, overflow, overflow);
        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void testBitMappedNodeMutatingExpansion()
    {
        int overflow = BitMappedNode.MAX_CHILDREN + 1;
        TrieNode origNode = new BitMappedNode(0, new Object[0]);
        TrieNode node = origNode;
        for (int i = 1; i < overflow; i++)
        {
            node = mInsert(node, i, i);
        }
        assertSame(origNode, node);

        node = mWith(node, overflow, overflow).inserts().returnsNew();
        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void checkCollisionNodeCreation()
    {
        CustomKey foo = new CustomKey("foo");
        CustomKey bar = foo.collide("bar");
        TrieNode trieNode = new BitMappedNode();
        trieNode = insert(trieNode, foo, new Object());
        trieNode = insert(trieNode, bar, new Object());

        Object shouldBeNull = ((BitMappedNode) trieNode).kvPairs[0];
        assertNull(shouldBeNull);

        Object shouldBeNode = ((BitMappedNode) trieNode).kvPairs[1];
        assertTrue(shouldBeNode instanceof CollisionNode);
        assertEquals(2, trieNode.countKeys());
        // This only works because values are stored into collision nodes in order of insertion...
        assertEquals(foo, ((CollisionNode) shouldBeNode).kvPairs[0]);
        assertEquals(bar, ((CollisionNode) shouldBeNode).kvPairs[2]);
    }

    @Test
    public void checkBitMappedNodeAddAndRemoveKey()
    {
        Changes changes = new Changes();
        Object key1 = new CustomKey(0x00, 0);

        TrieNode bmn0 = new BitMappedNode();
        TrieNode bmn1 = bmn0.with(0x00, 20, key1, key1, changes);
        TrieNode bmn2 = bmn1.without(0x00, 20, key1, changes);
        assertEmpty(bmn2);
    }

    @Test
    public void checkBitMappedNodeAddAndRemoveSubtrie()
    {
        Changes changes = new Changes();
        Object key1 = new CustomKey(0x00, 0);
        Object key2 = new CustomKey(0x01, 1);

        // Setup a BitMappedNode that contains a subtrie as its only element.
        TrieNode bmn0 = new BitMappedNode();
        TrieNode bmn1 = bmn0.with(0x00, 20, key1, key1, changes);
        TrieNode bmn2 = bmn1.with(0x01, 20, key2, key2, changes);

        TrieNode bmn3 = bmn2.without(0x00, 20, key1, changes);
        TrieNode bmn4 = bmn3.without(0x01, 20, key2, changes);
        assertEmpty(bmn4);
    }

    @Test
    public void checkBitMappedNodeExpansionAndArrayNodeCompression()
    {
        final int max = BitMappedNode.MAX_CHILDREN;

        Changes changes = new Changes();
        Object[] keys = new Object[17];
        TrieNode trieNode = new BitMappedNode();
        for (int i = 0; i < max; i++)
        {
            Object key = keys[i] = Integer.toString(i);
            trieNode = trieNode.with(i, 0, key, key, changes);
        }

        assertTrue(trieNode instanceof BitMappedNode);
        assertEquals(max, trieNode.countKeys());

        Object overConversionLimitKey = keys[max] = "" + max;
        trieNode = trieNode.with(max, 0, overConversionLimitKey, overConversionLimitKey, changes);
        assertTrue(trieNode instanceof HashArrayMappedNode);
        assertEquals(17, trieNode.countKeys());
        for (int i = 0; i < 17; i++)
        {
            assertSame(keys[i], trieNode.get(i, 0, keys[i]), "Key " + i);
        }

        for (int i = 0; i < 9; i++)
        {
            Object key = keys[i];
            keys[i] = null;
            trieNode = trieNode.without(i, 0, key, changes);
        }
        assertTrue(trieNode instanceof HashArrayMappedNode);
        assertEquals(8, trieNode.countKeys());
        for (int i = 9; i < 17; i++)
        {
            assertSame(keys[i], trieNode.get(i, 0, keys[i]), "Key " + i);
        }

        trieNode = trieNode.without(9, 0, keys[9], changes);
        keys[9] = null;

        assertTrue(trieNode instanceof BitMappedNode);
        assertEquals(7, trieNode.countKeys());

        for (int i = 10; i < 17; i++)
        {
            assertSame(keys[i], trieNode.get(i, 0, keys[i]), "Key " + i);
        }
    }

    @Test
    public void keysNotInBitMap()
    {
        Changes changes = new Changes();
        TrieNode node = new BitMappedNode();
        TrieNode stillSame = node.without(0, 0, new Object(), changes);
        assertEquals(0, changes.changeCount());
        assertEquals(0, changes.keyCountDelta());
        assertSame(node, stillSame);
        assertNull(node.get(0, 0, new Object()));
    }


    //=========================================================================
    // HashArrayMappedNode

    @Test
    public void testHashArrayMappedNodeInsertAndReplace()
    {
        TrieNode node = new HashArrayMappedNode<>();
        node = insert(node, 1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = replace(node, 1, "one");
        node = replace(node, 2, "two");
        node = replace(node, 3, "three");

        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void testHashArrayMappedNodeMutatingInsertAndReplace()
    {
        TrieNode node = new HashArrayMappedNode<>();
        node = mInsert(node, 1, 1);
        node = mInsert(node, 2, 2);
        node = mInsert(node, 3, 3);
        node = mReplace(node, 1, "one");
        node = mReplace(node, 2, "two");
        node = mReplace(node, 3, "three");

        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void testHashArrayMappedNodeRemove()
    {
        TrieNode node = new HashArrayMappedNode<>();
        node = insert(node, 1, 1);
        node = insert(node, 2, 2);
        node = insert(node, 3, 3);
        node = noopRemove(node, 4);
        node = remove(node, 2);
        node = remove(node, 1);
        node = remove(node, 3);
        node = noopRemove(node, 2);

        assertEmpty(node);
    }

    @Test
    public void testHashArrayMappedNodeCollision()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");

        TrieNode node = new HashArrayMappedNode<>();
        node = insert(node, key1, 1);
        node = insert(node, key2, 2);

        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void testHashArrayMappedNodeMutatingCollision()
    {
        CustomKey key1 = new CustomKey("key1");
        CustomKey key2 = key1.collide("key2");

        TrieNode node = new HashArrayMappedNode<>();
        node = mInsert(node, key1, 1);
        node = mInsert(node, key2, 2);

        assertEquals(HashArrayMappedNode.class, node.getClass());
    }

    @Test
    public void checkHashArrayMappedNode()
    {
        TrieNode node = new FlatNode();

        for (int i = 0; i < 16; i++) {
            CustomKey key = new CustomKey(i, new Object());
            Object value = new Object();
            node = insert(node, key, value);
        }
        CustomKey checkKey = new CustomKey(16, "foo");
        String checkVal = "bar";
        node = insert(node, checkKey, checkVal);

        assertTrue(node instanceof HashArrayMappedNode);
        assertEquals(17, node.countKeys());

        noopRemove(node, new CustomKey(30, "notInHAMNode"));
        noopRemove(node, new CustomKey(10, "notInFlatNode"));
        noopReplace(node, checkKey, checkVal);
    }


    //=========================================================================
    // General structural tests

    /**
     * This test verifies that we shift along the hash value correctly when going "down"
     * a level in the trie. An early, pre-release implementation had
     * a bug where one of the {@code shift + 5} code statements was just {@code shift}.
     *
     * What this test does is that it creates a two level trie.
     * The first level nests the second level in the first slot.
     * The second level contains a value at the second slot.
     *
     * Then, we try to add in a new value which should go into the third slot of the second level.
     * If the shifting along the hash did not get added, it would be placed in the first slot of the second level.
     */
    @Test
    public void testCorrectShifting()
    {
        Changes changes = new Changes();

        TrieNode nested = new BitMappedNode().with(0x20, 5, new CustomKey(0, "redherring"), 1, changes);
        // 0x20 = 0b00001 00000 -> the second slot in a bitmap with a node at shift 5.
        assertEquals(0b010, ((BitMappedNode) nested).bitmap);

        TrieNode top = new BitMappedNode(0b1, new Object[]{null, nested});
        // a bitmap of 0b1 to indicate that the hashFragment corresponding to 0b00000 is occupied.
        // 0b00000 is occupied because a hash of 0x20 with a shift of 0 would be nested at 0b00000

        top = top.with(0x40, 0, new CustomKey(4, "ignoreme"), 2, changes);
        // 0x40 = 0b00010 00000 -> the first slot (0b00000) in a bitmap with a node at shift 0.
        // Since that spot is already occupied by a node, we'll insert it in
        // the third slot (0b00010) in the nested node with shift 5.

        Object[] topVals = ((BitMappedNode) top).kvPairs;
        assertEquals(2, topVals.length);
        assertEquals(0b1, ((BitMappedNode) top).bitmap);

        BitMappedNode newNested = (BitMappedNode) topVals[1];
        assertEquals(0b110, newNested.bitmap);
        // If the "bar" key used a shift of 0 when added to a nested node
        // (like in a previous buggy impl) then the hashFragment would have
        // been 0b00000 and the bitmap would have been 0b011

        Object[] newNestedVals = newNested.kvPairs;
        assertEquals(4, newNestedVals.length);
        assertEquals(1, newNestedVals[1]);
        assertEquals(2, newNestedVals[3]);
    }

    /**
     * This constructs a trie such that there are collisions on input hashcode zero,
     * and that the collisions are pushed to the lowest possible node of the trie.
     *
     * The test verifies that at the very lowest level, we will have {@link CollisionNode}s
     * which contain all values with direct hash collisions and that we never fail
     * the {@code shift <= 30} assertion within {@link TrieNode#hashFragment(int, int)}.
     */
    @Test
    public void testDeepShifts()
    {
        CustomKey foo = new CustomKey(0, "foo");
        CustomKey bar = foo.collide("bar");

        TrieNode customFHT = empty();

        Changes changes = new Changes();

        // These two will be in the collision node at the bottom.
        customFHT = customFHT.with(foo, "A", changes);
        customFHT = customFHT.with(bar, "B", changes);

        // For each level, push in enough nodes s.t. we convert from a FlatNode to a BitMappedNode
        for (int shift = 0; shift <= 25; shift += 5)
        {
            // Spread out hash keys so that only "A" and "B" are in the CollisionNode at the bottom.
            for (int i = 1; i < 32; i++)
            {
                CustomKey key = new CustomKey(i << shift, UUID.randomUUID().toString());
                customFHT = customFHT.with(key, key, changes);
            }
        }
        // The last level has to be more custom built since the only possible hash keys are going to be 00, 01, 10, and 11.
        // The last level will also only ever be a BitMappedNode, full of collision nodes at the bottom.
        // If we iterated over i for keys then every 4th key would end up in the CollisionNode at the bottom.
        for (int i = 0; i < 3; i++)
        {
            for (int j = 1; j < 4; j++)
            {
                CustomKey key = new CustomKey(j << 30, UUID.randomUUID().toString());
                customFHT = customFHT.with(key, key, changes);
            }
        }

        assertEquals("A", customFHT.get(foo));
        assertEquals("B", customFHT.get(bar));

        int shuffledHash = hashCodeFor(0);                                 // Note, due to shuffling it's not necessarily at the first node of the tree.
        TrieNode node = customFHT;                                              // Shift 0:     HAMNode [↓, UUIDs...]
        for (int shift = 0; shift <= 25; shift += 5)                            // Shift 5:     HAMNode [↓, UUIDs...]
        {                                                                       // Shift 10:    HAMNode [↓, UUIDs...]
            assertTrue(node instanceof HashArrayMappedNode);                    // Shift 15:    HAMNode [↓, UUIDs...]
            int nestedIndex = (shuffledHash >>> shift) & 0x1f;                  // Shift 20:    HAMNode [↓, UUIDs...]
            node = ((HashArrayMappedNode) node).nodes[nestedIndex];             // Shift 25:    HAMNode [↓, UUIDs...]
        }                                                                       // Shift 30:    BMNode [↓, UUIDs...]
        assertTrue(node instanceof BitMappedNode);                              // Shift NA:    CollisionNode["foo", "A", "bar", "B"]

        // this abuses the fact that a "full" bottom level BMNode won't require bit counting to index into the array.
        int bitMappedNodeIndex = ((shuffledHash >>> 30) & 0x1f) * 2 + 1;
        node = (TrieNode) ((BitMappedNode) node).kvPairs[bitMappedNodeIndex];
        assertTrue(node instanceof CollisionNode);

        List<Object> values = Arrays.asList(((CollisionNode) node).kvPairs);
        assertTrue(values.contains("A"));
        assertTrue(values.contains("B"));


        // Now let's verify that accesses don't shift over 30. There's an assert in hashFragment()
        customFHT = customFHT.with(new CustomKey(4 << 30, "baz"), "C", changes); // 4 << 30 == 0 << 30 (for 32 bit values)

        assertEquals("A", customFHT.get(foo));
        assertEquals("B", customFHT.get(bar));
        assertEquals("C", customFHT.get(new CustomKey(0, "baz")));

        TrieNode newNode = customFHT;                                           // Shift 0:     HAMNode [↓, UUIDs...]
        for (int shift = 0; shift <= 25; shift += 5)                            // Shift 5:     HAMNode [↓, UUIDs...]
        {                                                                       // Shift 10:    HAMNode [↓, UUIDs...]
            assertTrue(newNode instanceof HashArrayMappedNode);                 // Shift 15:    HAMNode [↓, UUIDs...]
            int nestedIndex = (shuffledHash >>> shift) & 0x1f;                  // Shift 20:    HAMNode [↓, UUIDs...]
            newNode = ((HashArrayMappedNode) newNode).nodes[nestedIndex];       // Shift 25:    HAMNode [↓, UUIDs...]
        }                                                                       // Shift 30:    BMNode [↓, UUIDs...]
        assertTrue(newNode instanceof BitMappedNode);                           // Shift NA:    CollisionNode["foo", "A", "bar", "B", "baz", "C"]

        // this abuses the fact that a "full" bottom level BMNode won't require bit counting to index into the array.
        bitMappedNodeIndex = ((shuffledHash >>> 30) & 0x1f) * 2 + 1;
        newNode = (TrieNode) ((BitMappedNode) newNode).kvPairs[bitMappedNodeIndex];
        assertTrue(newNode instanceof CollisionNode);

        List<Object> newValues = Arrays.asList(((CollisionNode) newNode).kvPairs);
        assertTrue(newValues.contains("A"));
        assertTrue(newValues.contains("B"));
        assertTrue(newValues.contains("C"));
    }

    //=========================================================================
    // Construction methods

    @Test
    public void fromArraysGivenEmptyArraysReturnsEmptySingleton()
    {
        Changes changes = new Changes();

        TrieNode t = fromArrays(new Object[0], new Object[0], changes);

        assertSame(empty(), t);
        assertEquals(0, changes.keyCountDelta());
    }

    @Test
    public void fromArraysRequiresEqualLengthArrays()
    {
        Changes changes = new Changes();

        assertThrows(IllegalArgumentException.class,
                     () -> fromArrays(new Object[]{ 1 },
                                      new Object[]{ 1, 2 },
                                      changes));
    }

    @Test
    public void fromArraysAssociatesProperly()
    {
        Changes changes = new Changes();

        TrieNode t = fromArrays(new Object[] { 1, 2 },
                                new Object[] { 3, 4 },
                                changes);

        assertEquals(2, changes.keyCountDelta());
        assertEquals(3, t.get(1));
        assertEquals(4, t.get(2));
    }

    @Test
    public void fromArraysInsertsInGivenOrder()
    {
        Changes changes = new Changes();

        TrieNode t = fromArrays(new Object[] { 1, 1 },
                                new Object[] { 3, 4 },
                                changes);

        assertEquals(1, changes.keyCountDelta());
        assertEquals(4, t.get(1));
    }


    public static <K, V> TrieNode<K, V> trieFromPairs(Object... kvPairs)
    {
        Changes changes = new Changes();

        TrieNode<K, V> root = empty();
        for (int i = 0; i < kvPairs.length; i += 2)
        {
            root = root.mWith((K) kvPairs[i], (V) kvPairs[i+1], changes);
        }
        return root;
    }

    @Test
    public void fromSelectedKeysReturnsSubset()
    {
        TrieNode origin = trieFromPairs(1, 1, 2, 2, 3, 3);

        Changes changes = new Changes();
        TrieNode t = fromSelectedKeys(origin, new Object[]{ 1, 3, 5 }, changes);

        assertEquals(2,    changes.keyCountDelta());
        assertEquals(1,    t.get(1));
        assertEquals(null, t.get(2));
        assertEquals(3,    t.get(3));
    }

    @Test
    public void fromSelectedKeysReturnsEmptySingleton()
    {
        TrieNode origin = trieFromPairs(1, 1, 2, 2);

        Changes changes = new Changes();
        TrieNode t = fromSelectedKeys(origin, new Object[]{}, changes);
        assertSame(empty(), t);
        assertEquals(0, changes.keyCountDelta());

        t = fromSelectedKeys(origin, new Object[]{ 3, 4 }, changes);
        assertSame(empty(), t);
        assertEquals(0, changes.keyCountDelta());
    }


    // TrieNode.withoutKeys() is implemented atop without() and not handled by
    // individual node classes, so just testing it using the most simple
    // subclass (as well as EmptyNode above, since it might have a shortcut
    // override).

    @Test
    public void withoutKeysRequiresNonNullArray()
    {
        assertThrows(NullPointerException.class,
                     () -> new FlatNode().withoutKeys(null, new Changes()));
    }

    @Test
    public void withoutKeysGivenNoKeysReturnsSame()
    {
        TrieNode trie = flatNodeForPairs(1, 1, 2, 2, 3, 3);
        assertSame(trie, trie.withoutKeys(new Object[0], new Changes()));
    }

    @Test
    public void withoutKeysGivenKeysReturnsAnswer()
    {
        TrieNode trie = flatNodeForPairs(1, 1, 2, 2, 3, 3);
        TrieNode answer = trie.withoutKeys(new Object[]{1, 3, 5}, new Changes());

        assertEquals(1, answer.countKeys());
        assertValueEquals(2, answer, 2);
    }

    @Test
    public void withoutKeysGivenAllKeysReturnsSingleton()
    {
        TrieNode trie = flatNodeForPairs(1, 1, 2, 2);
        TrieNode answer = trie.withoutKeys(new Object[]{1, 3, 2}, new Changes());
        assertEmpty(answer);
    }
}
