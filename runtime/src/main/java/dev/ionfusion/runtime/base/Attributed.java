// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

/**
 * Provides strongly typed {@link Attribute}s attached to an object.
 * <p>
 * Attributes are matched by their instance identity.
 */
public interface Attributed
{
    /**
     * Adds a new attribute to this object.
     *
     * @param attribute The attribute to add. Must not be null and must not be present
     * on this object.
     * @param value The value of the attribute. Must not be null.
     * @param <T> The type of the attribute's value.
     *
     * @throws IllegalStateException if the attribute is present on this object.
     */
    <T> void addAttribute(Attribute<T> attribute, T value);

    /**
     * Retrieves the value of an attribute of this object.
     *
     * @param attribute The attribute to retrieve. Must not be null.
     * @param <T> The type of the attribute's value.
     *
     * @return The value of the attribute, or null if the attribute is absent.
     */
    <T> T getAttribute(Attribute<T> attribute);
}
