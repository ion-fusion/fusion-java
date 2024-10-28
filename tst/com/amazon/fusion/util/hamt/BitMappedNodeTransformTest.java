// Copyright (c) 2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.util.hamt;

import static com.amazon.fusion.util.Permute.generateSubsetPermutations;
import static java.util.Collections.unmodifiableList;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.BitMappedNode;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.Changes;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class BitMappedNodeTransformTest
    extends TransformTestCase<Object[]>
{
    private static List<Object[]> RECIPES;

    @BeforeClass
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

    @AfterClass
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
