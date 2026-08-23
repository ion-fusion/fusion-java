// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

/**
 * Provides access to metadata about a resource and optionally access to its content.
 * <p>
 * Long term, this is intended to surface things like the deployment unit containing the
 * resource, perhaps checksums, etc. This enables passing context from the component
 * providing the resource.
 */
public interface ResourceDescriptor
{
    /**
     * Returns a human-readable description of the resource, for display in messages.
     * This could be a file path, a URL, or an arbitrary string.
     *
     * @return the displayable name of this source, not null or empty.
     */
    String display();

    /**
     * Returns an identifier of the resource content, if possible.
     *
     * @return can be null.
     */
    ResourceIdentifier getResourceId();


    /**
     * Returns true if this descriptor came from {@link #unknown()}.
     *
     * @return true for instances returned by {@link #unknown()}.
     */
    default boolean isUnknown()
    {
        return false;
    }


    /**
     * Returns a descriptor that displays as "unknown resource" and has no
     * {@link ResourceIdentifier}.
     *
     * @return not null.
     */
    static ResourceDescriptor unknown()
    {
        return new ResourceDescriptor()
        {
            @Override
            public String display()
            {
                return "unknown resource";
            }

            @Override
            public ResourceIdentifier getResourceId()
            {
                return null;
            }

            @Override
            public boolean isUnknown()
            {
                return true;
            }

            // Default equals/hashCode are correct
        };
    }
}
