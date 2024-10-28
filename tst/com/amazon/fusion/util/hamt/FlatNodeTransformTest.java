// Copyright (c) 2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.util.hamt;

import static com.amazon.fusion.util.Permute.generateSubsetPermutations;
import static java.util.Collections.unmodifiableList;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.CollisionNode;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.FlatNode;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Tests transformations of {@link FlatNode} and {@link CollisionNode}.
 *
 * The latter is a simplified variant of the former, so we test them together.
 * This is aided by the fact that FlatNode can only have CollisionNodes as
 * child-nodes.
 */
public class FlatNodeTransformTest
    extends TransformTestCase<Object[]>
{
    private static List<Object[]> RECIPES;


    /**
     * Create a CollisionNode with the given elements, forcing them to share
     * the same hash as the first.
     */
    static CollisionNode collide(Object... elements)
    {
        int hash = elements[0].hashCode();

        Object[] kvPairs = new Object[elements.length * 2];
        for (int i = 0; i < elements.length; i++)
        {
            kvPairs[i*2] = new CustomKey(hash, elements[i]);
            kvPairs[i*2 + 1] = elements[i];
        }

        return new CollisionNode<>(hash, kvPairs);
    }

    @BeforeClass
    public static void setUpClass()
    {
        Object[] elements = new Object[] {
                1,
                2,
                3,
                collide(9),       // Fully removed by Remove3sXform
                collide(11),
                collide(15, 30),  // Fully removed by Remove3sXform
                collide(10, 12),  // Partially removed by Remove3sXform
        };

        RECIPES = unmodifiableList(generateSubsetPermutations(elements));
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
    FlatNode makeNode(Object[] elements)
    {
        assert elements.length <= FlatNode.MAX_CHILDREN;

        Object[] kvPairs = new Object[elements.length * 2];
        for (int i = 0; i < elements.length; i++)
        {
            Object e = elements[i];
            if (! (e instanceof CollisionNode))
            {
                kvPairs[i * 2] = new CustomKey(e);
            }
            kvPairs[i*2 + 1] = e;
        }

        return new FlatNode<>(kvPairs);
    }
}
