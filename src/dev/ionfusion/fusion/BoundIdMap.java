// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map that uses bound identifiers as keys.
 * <p>
 * Two identifiers ({@link SyntaxSymbol}s) are {@code bound_identifier_equal}
 * if they have the same (original) binding and the same marks.
 * <p>
 * This class is not thread-safe!
 *
 * @param <V> the type of mapped values.
 */
final class BoundIdMap<V>
{
    private final Map<BoundIdentifier, V> myMap = new HashMap<>();


    /**
     * @return the previous value associated with the identifier, or null
     *   if there was no mapping for it.
     */
    final V put(SyntaxSymbol identifier, V binding)
    {
        BoundIdentifier key = identifier.resolveBoundIdentifier();
        return myMap.put(key, binding);
    }


    final V get(BoundIdentifier identifier)
    {
        return myMap.get(identifier);
    }

    final V get(SyntaxSymbol identifier)
    {
        return myMap.get(identifier.resolveBoundIdentifier());
    }

    final V get(Binding binding, Set<MarkWrap> marks)
    {
        return myMap.get(new BoundIdentifier(binding, marks));
    }

    final Collection<V> values()
    {
        return myMap.values();
    }
}
