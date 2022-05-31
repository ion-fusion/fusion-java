// Copyright (c) 2021 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.util.hamt;

import com.amazon.fusion.util.hamt.HashArrayMappedTrie.Changes;
import com.amazon.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomTrieTransformTest
    extends TransformTestCase<TrieNode>
{
    private TrieNode randomTrie(int size, int collisionChance)
    {
        assert 0 <= collisionChance && collisionChance <= 100;

        TrieNode trie = HashArrayMappedTrie.empty();
        Changes changes = new Changes();

        Random r = new Random();
        for (int i = 0; i < size; i++)
        {
            int v = r.nextInt();
            CustomKey key = new CustomKey(v);
            trie = trie.mWith(key, v, changes);

            while (r.nextInt(100) < collisionChance)
            {
                key = key.collide(v);
                trie = trie.mWith(key, v, changes);
                i++;
            }
        }

        return trie;
    }

    @Override
    List<TrieNode> recipes()
    {
        TrieNode trie = randomTrie(100_000, 20);
        return Arrays.asList(trie);
    }

    @Override
    TrieNode makeNode(TrieNode trie)
    {
        return trie;
    }
}
