// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.net.URI;
import java.nio.file.Path;

/**
 * Provides access to metadata about a resource and optionally access to its content.
 * <p>
 * There are three varieties of resource descriptors:
 * <ul>
 *   <li><em>Identified</em> descriptors have a {@link ResourceIdentifier}, so their
 *   content can be accessed from the descriptor.  Their {@link #display} form is that
 *   of their identifier's {@link Path} or {@link URI}.</li>
 *   <li><em>Named</em> descriptors have no identifier, only a custom {@link #display}
 *   string.</li>
 *   <li><em>Unknown</em> descriptors have no identifier, only a generic {@link #display}
 *   string.</li>
 * </ul>
 * <p>
 * Two descriptors are considered equal if they are the same instance or if they
 * have equal non-null {@link ResourceIdentifier}s.
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


    //==================================================================================
    // Factories

    /**
     * Returns a descriptor that displays as the given name and has no
     * {@link ResourceIdentifier}.
     * <p>
     * Named descriptors are only {@link Object#equals} to themselves.
     *
     * @param name must not be null or empty.
     *
     * @return a new descriptor.
     */
    static ResourceDescriptor named(String name)
    {
        return new ResourceDescriptorImpl.NamedResourceDescriptor(name);
    }


    /**
     * Returns a descriptor that displays as "unknown resource" and has no
     * {@link ResourceIdentifier}.
     * <p>
     * Unknown descriptors are only {@link Object#equals} to themselves.
     *
     * @return not null.
     */
    static ResourceDescriptor unknown()
    {
        return new ResourceDescriptorImpl.UnknownResourceDescriptor();
    }
}
