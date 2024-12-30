// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.CollisionNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.HashArrayMappedNode;
import dev.ionfusion.fusion.util.hamt.HashArrayMappedTrie.TrieNode;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * Iterator for {@code KvNode}s.
 * <p>
 * A minor optimization could be made for {@link CollisionNode}s similar to that
 * done for {@link HashArrayMappedNode}s, since they never have child nodes.
 */
@SuppressWarnings("unchecked")
final class KvArrayIterator<K, V>
    implements Iterator<Entry<K, V>>
{
    private final Object[] array;
    private int            i = 0;

    // At most one of these is non-null because an array will contain
    // either a key then value pair or a null then a node pair.
    private Iterator<Entry<K, V>> childIterator;
    private Entry<K, V>           nextEntry;

    KvArrayIterator(Object[] array)
    {
        this.array = array;
    }

    private boolean getAndCacheNext()
    {
        while (i < array.length)
        {
            Object keyOrNull = array[i++];
            Object valOrNode = array[i++];
            if (keyOrNull != null)
            {
                nextEntry = new SimpleEntry<>((K) keyOrNull, (V) valOrNode);
                return true;
            }
            else if (valOrNode != null)
            {
                Iterator<Entry<K, V>> i = ((TrieNode<K, V>) valOrNode).iterator();
                if (i.hasNext())
                {
                    childIterator = i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasNext()
    {
        return childIterator != null || nextEntry != null || getAndCacheNext();
    }

    @Override
    public Entry<K, V> next()
    {
        if (childIterator != null)
        {
            Entry<K, V> ret = childIterator.next();
            if (!childIterator.hasNext())
            {
                childIterator = null;
            }
            return ret;
        }
        else if (nextEntry != null)
        {
            Entry<K, V> ret = nextEntry;
            nextEntry = null;
            return ret;
        }
        else if (getAndCacheNext())
        {
            return next();
        }
        else
        {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
