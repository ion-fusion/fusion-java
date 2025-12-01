// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.testing.Permute.generateSubsets;
import static java.util.Collections.unmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.BitMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.Changes;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.HashArrayMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class HamNodeTransformTest
    extends TransformTestCase<Object[]>
{
    private static List<Object[]> RECIPES;

    @BeforeAll
    public static void setUpClass()
    {
        Object[] elements = {
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
                12,
        };

        // We want this to be large enough that some subsets have more than the
        // minimum number of children (and thus don't shrink after any change).
        assert HashArrayMappedNode.MIN_CHILDREN + 1 < elements.length;

        // Elements will be shuffled by the trie, so we don't need to permute.
        // This also allows us to use larger number of elements while running
        // in a reasonable amount of time.
        RECIPES = unmodifiableList(generateSubsets(elements));
    }

    @AfterAll
    public static void tearDownClass()
    {
        RECIPES = null;
    }

    @Override
    List<Object[]> recipes()
    {
        return RECIPES;
    }

    @Override
    HashArrayMappedNode makeNode(Object[] elements)
    {
        assert elements.length <= HashArrayMappedNode.MAX_CHILDREN;

        Changes changes = new Changes();
        HashArrayMappedNode node = new HashArrayMappedNode();
        for (int i = 0; i < elements.length; i++)
        {
            Object value = elements[i];
            CustomKey key = new CustomKey(i, value);
            node = (HashArrayMappedNode) node.mWith(key, value, changes);
        }
        return node;
    }

    @Override
    <K, V> TrieNode<K, V> checkTransform(TrieNode<K, V> node, BiFunction xform)
    {
        TrieNode<K, V> actual = super.checkTransform(node, xform);

        if (actual != node)
        {
            int size = actual.countKeys();
            if (size == 0)
            {
                assertSame(HashArrayMappedTrie.empty(), actual);
            }
            else if (size < HashArrayMappedNode.MIN_CHILDREN)
            {
                assertThat("Should shrink to BitMappedNode",
                           actual, is(instanceOf(BitMappedNode.class)));
            }
        }
        return actual;
    }
}
