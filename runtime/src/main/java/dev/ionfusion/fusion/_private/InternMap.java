// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import static java.util.Objects.requireNonNull;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A map that stores unique interned values for given keys.
 * Values are weakly referenced, so they may be garbage collected when they
 * become unreachable from other parts of the application.
 */
public class InternMap <K, V>
{
    private final Map<V, WeakReference<V>> myMap;
    private final Function<K, V>           myValueFactory;


    public InternMap(Function<K, V> valueFactory)
    {
        this(valueFactory, 16);
    }

    public InternMap(Function<K, V> valueFactory, int initialCapacity)
    {
        myValueFactory = valueFactory;
        myMap = new WeakHashMap<>(initialCapacity);
    }


    /**
     * Produces an interned value for the given key.
     * <p>
     * If the key has no associated value, one is produced using the factory
     * provided to this map's constructor.
     */
    public V intern(K key)
    {
        // We do not want to assume that the key is held strongly by the value.
        // We therefore cannot use the key directly with a WeakHashMap: it
        // could be collected at any time, and then the corresponding map entry
        // and our reference to a still-needed interned value.

        V val = myValueFactory.apply(requireNonNull(key));

        // Prevent other threads from touching the intern table.
        // This doesn't prevent the GC from removing entries!
        synchronized (myMap)
        {
            WeakReference<V> ref = myMap.get(val);
            if (ref != null)
            {
                // There's a chance that the entry for a string will exist but
                // the weak reference has been cleared.
                V interned = ref.get();
                if (interned != null) return interned;
            }

            ref = new WeakReference<>(val);
            myMap.put(val, ref);

            return val;
        }
    }


    /**
     * Returns the number of entries in the map.
     * This may not reflect unreachable values.
     */
    public int size()
    {
        return myMap.size();
    }
}
