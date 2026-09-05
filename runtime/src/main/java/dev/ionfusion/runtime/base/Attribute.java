// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

/**
 * A key for storing attributes in an {@link Attributed} object.
 * <p>
 * Attributes are matched by their instance identity.
 *
 * @param <T> The type of the attribute's value.
 */
public class Attribute<T>
{
    private final Class<T> myType;

    private Attribute(Class<T> type)
    {
        myType = type;
    }

    public T typecheck(Object value)
    {
        return myType.cast(value);
    }

    public static <T> Attribute<T> ofType(Class<T> type)
    {
        return new Attribute<>(type);
    }
}
