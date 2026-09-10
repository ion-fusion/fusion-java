// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.runtime.base._Private_Attributes.MODULE_IDENTITY_ATTRIBUTE;
import static java.util.Objects.requireNonNull;

import dev.ionfusion.runtime.base.SourceNameImpl.ResourceSourceName;
import java.io.File;

/**
 * Identifies a source of Fusion code or other data: a file, URL, <em>etc.</em>
 * <p>
 * The primary purpose of this class is to display a suitable message fragment
 * for error reporting to users.
 */
public interface SourceName
    extends ResourceDescriptor
{
    //=========================================================================
    // Factory methods

    /**
     * Creates a {@link SourceName} that will simply display the given text.
     *
     * @param display must not be null.
     *
     * @return a new {@link SourceName} instance
     */
    static SourceName forDisplay(String display)
    {
        if (display.isEmpty()) {
            throw new IllegalArgumentException("display must not be empty");
        }
        return new SourceNameImpl(display);
    }


    /**
     * Creates a {@link SourceName} representing a file at the given path.
     *
     * @param path must not be null or empty, and is converted to an absolute path.
     *
     * @return a new descriptor.
     *
     * @see #forFile(File)
     */
    static ResourceDescriptor forFile(String path)
    {
        ResourceIdentifier rsrc = ResourceIdentifier.forFile(path);
        return new ResourceSourceName(rsrc);
    }

    /**
     * Creates a {@link SourceName} representing a file. The {@link File}'s absolute
     * path will be displayed.
     *
     * @param path is converted to an absolute path.
     *
     * @return a new descriptor.
     *
     * @see #forFile(String)
     */
    static ResourceDescriptor forFile(File path)
    {
        ResourceIdentifier rsrc = ResourceIdentifier.forFile(path);
        return new ResourceSourceName(rsrc);
    }


    /**
     * Convert a resource identifier to a source name.
     *
     * @param resource must not be null.
     * @param id can be null.
     *
     * @return a new {@link SourceName}.
     */
    static ResourceDescriptor forResource(ResourceIdentifier resource, ModuleIdentity id)
    {
        requireNonNull(resource, "resource must not be null");
        if (id == null) return new ResourceSourceName(resource);
        SourceName name = new ResourceSourceName(resource);
        name.addAttribute(MODULE_IDENTITY_ATTRIBUTE, id);
        return name;
    }
}
