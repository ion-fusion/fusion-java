// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.NOTHING;
import static dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.Changes;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TrieNode#transform(BiFunction, Changes)} through many permutations of
 * node contents and operations that validate the full range of changes.
 *
 * Each subclass of {@link TrieNode} is tested individually using sets of entries
 * appropriate to it.  These aren't generally "deep" tests or tries; we trust the
 * recursive nature of the data structure.
 *
 * We use integers as keys and values, and transforms that alter, remove, or ignore
 * entries based on modulo arithmetic. This makes it easy to understand what each
 * transformation should do to each entry and to ensure the permutations include
 * the right variety of entries to get full coverage.
 *
 * @param <Recipe> is a type that can be used to construct a single test trie,
 *                which is run through a variety of transforms.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class TransformTestCase<Recipe>
{
    static <K, V> void assertTrieEquals(TrieNode<K, V> expected, TrieNode<K, V> actual)
    {
        if (expected.countKeys() == 0)
        {
            assertSame(empty(), actual);
            return;
        }

        Changes changes = new Changes();
        for (Entry<K, V> entry : actual)
        {
            K key = entry.getKey();
            assertEquals(expected.get(key), entry.getValue(), "value for key " + key);
            expected = expected.without(key, changes);
        }

        if (expected != empty())
        {
            fail("Expected entries were not present: " + expected + "\nActual trie was: " + actual);
        }
    }


    private static class XformChanges<K, V>
        extends Changes
    {
        final Set<V> actuallyRemovedValues = new HashSet<>();
        final Set<K> actuallyReplacedKeys  = new HashSet<>();

        @Override
        protected Object inserting(Object givenValue)
        {
            throw new UnsupportedOperationException("not for transform");
        }

        @Override
        protected Object replacing(Object storedValue, Object givenValue)
        {
            throw new UnsupportedOperationException("not for transform");
        }

        @Override
        protected void keyAdded(Object key, Object newValue)
        {
            throw new UnsupportedOperationException("not for transform");
        }

        @Override
        protected void keyRemoved(Object key, Object oldValue)
        {
            assertThat(oldValue, not(instanceOf(TrieNode.class)));
            actuallyRemovedValues.add((V) oldValue);
            super.keyRemoved(key, oldValue);
        }

        @Override
        protected void keyReplaced(Object key, Object oldValue, Object newValue)
        {
            assertNotSame(oldValue, newValue);
            assertThat(newValue, not(instanceOf(TrieNode.class)));
            assertThat(oldValue, not(instanceOf(TrieNode.class)));
            actuallyReplacedKeys.add((K) key);
            super.keyReplaced(key, oldValue, newValue);
        }
    }


    <K, V> TrieNode<K, V> checkTransform(TrieNode<K, V> orig, BiFunction xform)
    {
        XformChanges<K, V> changes = new XformChanges<>();
        TrieNode<K, V> actual = orig.transform(xform, changes);


        // Run a "manual" transformation over the same original trie.
        int expectedRemovedKeys = 0;
        Set<K> expectedReplacedKeys  = new HashSet<>();
        Set<V> expectedRemovedValues = new HashSet<>();

        TrieNode<K, V> expected = empty();
        for (Entry<K, V> entry : orig)
        {
            K key = entry.getKey();
            V storedValue = entry.getValue();
            Object newValue = xform.apply(key, storedValue);

            if (newValue == NOTHING)
            {
                expectedRemovedKeys++;
                expectedRemovedValues.add(storedValue);
            }
            else
            {
                if (newValue != storedValue)
                {
                    expectedReplacedKeys.add(key);
                }

                expected = expected.mWith(key, (V) newValue, new Changes());
            }
        }

        assertTrieEquals(expected, actual);

        assertThat("removed values",
                   changes.actuallyRemovedValues, is(expectedRemovedValues));
        assertThat("replaced keys",
                   changes.actuallyReplacedKeys, is(expectedReplacedKeys));

        assertEquals(-expectedRemovedKeys,
                     changes.keyCountDelta(),
                     "keyCountDelta");
        assertEquals(expectedReplacedKeys.size() + expectedRemovedKeys,
                     changes.changeCount(),
                     "changeCount");

        if (changes.changeCount() == 0)
        {
            assertSame(orig, actual, "no changes, trie should be the same");
        }

        return actual;
    }


    //=========================================================================


    /**
     * HAMT transform that does nothing.
     */
    static class NoopXform
        implements BiFunction
    {
        @Override
        public Object apply(Object key, Object storedValue)
        {
            return storedValue;
        }
    }


    /**
     * HAMT transform that adds one to each value.
     */
    static class IncrementXform
        implements BiFunction
    {
        @Override
        public Object apply(Object key, Object storedValue)
        {
            return 1 + (int) storedValue;
        }
    }


    /**
     * HAMT transform that removes values that are multiples of 3.
     */
    static class Remove3sXform
        implements BiFunction
    {
        @Override
        public Object apply(Object key, Object storedValue)
        {
            if ((int) storedValue % 3 == 0)
            {
                return NOTHING;
            }
            return storedValue;
        }
    }


    /**
     * HAMT transform that removes values that are multiples of 3, and
     * increments (other) multiples of 2.
     */
    static class Remove3sIncrement2sXform
        implements BiFunction
    {
        @Override
        public Object apply(Object key, Object storedValue)
        {
            int i = (int) storedValue;
            if (i % 3 == 0)
            {
                return NOTHING;
            }
            if (i % 2 == 0)
            {
                return i + 1;
            }
            return storedValue;
        }
    }

    abstract List<Recipe> recipes();

    abstract TrieNode makeNode(Recipe recipe);

    private void checkTransformOnCombinations(BiFunction xform)
    {
        List<Recipe> recipes = recipes();
        for (int i = 0; i < recipes.size(); i++)
        {
            TrieNode node = makeNode(recipes.get(i));
            try
            {
                checkTransform(node, xform);
            }
            catch (AssertionError e)
            {
                String message =
                    "Failure running " + xform.getClass().getSimpleName() +
                    " on combination #" + i + ": " + node;
                throw new AssertionError(message, e);
            }
        }
    }

    @Test
    public void testNoop()
    {
        checkTransformOnCombinations(new NoopXform());
    }

    @Test
    public void testModification()
    {
        checkTransformOnCombinations(new IncrementXform());
    }

    @Test
    public void testRemoval()
    {
        checkTransformOnCombinations(new Remove3sXform());
    }

    @Test
    public void testMixedChanges()
    {
        checkTransformOnCombinations(new Remove3sIncrement2sXform());
    }
}
