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
    private final Map<K, WeakReference<V>> myWeakMap;
    private final Function<K, V>           myValueFactory;


    public InternMap(Function<K, V> valueFactory)
    {
        myValueFactory = valueFactory;
        myWeakMap = new WeakHashMap<>();
    }

    public InternMap(Function<K, V> valueFactory, int initialCapacity)
    {
        myValueFactory = valueFactory;
        myWeakMap = new WeakHashMap<>(initialCapacity);
    }


    /**
     * Produces an interned value for the given key.
     * <p>
     * If the key has no associated value, one is produced using the factory
     * provided to this map's constructor.
     */
    public V intern(K key)
    {
        V sym = myValueFactory.apply(requireNonNull(key));

        // Prevent other threads from touching the intern table.
        // This doesn't prevent the GC from removing entries!
        synchronized (myWeakMap)
        {
            WeakReference<V> ref = myWeakMap.get(key);
            if (ref != null)
            {
                // There's a chance that the entry for a string will exist but
                // the weak reference has been cleared.
                V interned = ref.get();
                if (interned != null) return interned;
            }

            ref = new WeakReference<>(sym);
            myWeakMap.put(key, ref);

            return sym;
        }
    }


    /**
     * Returns the number of entries in the map.
     * This may not reflect unreachable values.
     */
    public int size()
    {
        return myWeakMap.size();
    }
}
