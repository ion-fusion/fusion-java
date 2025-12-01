// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import static dev.ionfusion.testing.Permute.generateSubsetPermutations;
import static java.util.Collections.unmodifiableList;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.BitMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.Changes;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class BitMappedNodeTransformTest
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
                new CustomKey(1, 11),
                new CustomKey(2, 12),
                new CustomKey(3, 15),
        };

        RECIPES =  unmodifiableList(generateSubsetPermutations(elements));
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
    BitMappedNode makeNode(Object[] elements)
    {
        assert elements.length <= BitMappedNode.MAX_CHILDREN;

        Changes changes = new Changes();
        BitMappedNode node = new BitMappedNode();
        for (int i = 0; i < elements.length; i++)
        {
            Object elt = elements[i];
            CustomKey key = (elt instanceof CustomKey ? (CustomKey) elt : new CustomKey(i, elt));
            node = (BitMappedNode) node.mWith(key, key.key(), changes);
        }
        return node;
    }
}
