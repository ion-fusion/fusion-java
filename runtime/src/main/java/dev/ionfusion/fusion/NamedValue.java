// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Fusion objects that support {@code object_name}.
 * <p>
 * Object names are intended for {@linkplain FusionIo#display display} in
 * error messages and debugging.
 *
 * @see ObjectNameProc
 */
interface NamedValue <T>
{
    /**
     * Suggests a name for the given value, if it can accept it.
     */
    @SuppressWarnings("unchecked")
    static void inferObjectName(Object value, Object name)
    {
        if (value instanceof NamedValue)
        {
            ((NamedValue<Object>)value).inferObjectName(name);
        }
    }

    /**
     * Suggests a name for this object, inferred from its syntactic context.
     * <p>
     * If this object already has a name, it must not be changed.
     */
    default void inferObjectName(T name)
    {
    }

    /**
     * Produces a Fusion value that's the object's name for the purpose of the
     * {@code object_name} function.
     * The result must not change between invocations.
     *
     * @return the object name. If null, {@code object_name} returns void.
     */
    T objectName();
}
