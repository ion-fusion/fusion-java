// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of {@link Attribute}s and their values.
 * <p>
 * This class is thread-safe.
 */
public class AttributeSet
    implements Attributed
{
    private final Map<Attribute<?>, Object> myFacets = new IdentityHashMap<>();

    public synchronized <T> void addAttribute(Attribute<T> facet, T value)
    {
        Objects.requireNonNull(value);

        if (null != myFacets.putIfAbsent(facet, facet.typecheck(value)))
        {
            throw new IllegalStateException("Facet already exists for " + facet);
        }
    }

    public synchronized <T> T getAttribute(Attribute<T> facet)
    {
        return facet.typecheck(myFacets.get(facet));
    }
}
