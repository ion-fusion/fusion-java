// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.util.hamt;

/**
 * This class allows us to easily form hash collisions for different keys.
 */
class CustomKey
{
    private final int hash;
    private final Object key;

    CustomKey(Object key)
    {
        this.key  = key;
        this.hash = key.hashCode();
    }

    CustomKey(int hash, Object key)
    {
        this.hash = hash;
        this.key = key;
    }

    CustomKey collide(Object otherKey)
    {
        return new CustomKey(hash, otherKey);
    }

    CustomKey nonCollide(Object otherKey)
    {
        return new CustomKey(hash + 1, otherKey);
    }

    Object key()
    {
        return key;
    };

    @Override
    public String toString()
    {
        return "CustomKey::{hash:" + hash + ", key:" + key + "}";
    }

    @Override
    public int hashCode()
    {
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CustomKey)
        {
            return key.equals(((CustomKey) obj).key);
        }
        return false;
    }
}
