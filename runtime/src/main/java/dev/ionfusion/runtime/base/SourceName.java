// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static java.util.Objects.requireNonNull;

import dev.ionfusion.runtime.base.SourceNameImpl.ModuleSourceName;
import dev.ionfusion.runtime.base.SourceNameImpl.ResourceSourceName;
import java.io.File;
import java.net.URL;

/**
 * Identifies a source of Fusion code or other data: a file, URL, <em>etc.</em>
 * <p>
 * The primary purpose of this class is to display a suitable message fragment
 * for error reporting to users.
 */
public interface SourceName
    extends ResourceDescriptor
{
    /**
     * The standard extension for Fusion source code files.
     */
    String FUSION_SOURCE_EXTENSION = ".fusion";


    /**
     * It is not guaranteed that the module declaration is the only content of
     * the file or URL.
     * The resource could be a script with several modules inside, and module
     * declarations will eventually nest.
     *
     * @return the module associated with this source, if any.
     */
    ModuleIdentity getModuleIdentity();


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
     * @return a new {@link SourceName} instance
     *
     * @see #forFile(File)
     */
    static SourceName forFile(String path)
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
     * @return a new {@link SourceName} instance
     *
     * @see #forFile(String)
     */
    static SourceName forFile(File path)
    {
        ResourceIdentifier rsrc = ResourceIdentifier.forFile(path);
        return new ResourceSourceName(rsrc);
    }


    /**
     * @param id must not be null.
     * @param sourceFile must not be null.
     * @return a new {@link SourceName}.
     */
    static SourceName forModule(ModuleIdentity id, File sourceFile)
    {
        requireNonNull(id, "id must not be null");
        ResourceIdentifier rsrc = ResourceIdentifier.forFile(sourceFile);
        return new ModuleSourceName(rsrc, id);
    }


    /**
     * @param id must not be null.
     * @param url must not be null.
     * @return a new {@link SourceName}.
     */
    static SourceName forUrl(ModuleIdentity id, URL url)
    {
        requireNonNull(id, "id must not be null");
        ResourceIdentifier rsrc = ResourceIdentifier.forUrl(url);
        return new ModuleSourceName(rsrc, id);
    }


    /**
     * Convert a resource identifier to a source name.
     *
     * @param resource must not be null.
     * @param id can be null.
     *
     * @return a new {@link SourceName}.
     */
    static SourceName forResource(ResourceIdentifier resource, ModuleIdentity id)
    {
        requireNonNull(resource, "resource must not be null");
        if (id == null) return new ResourceSourceName(resource);
        return new ModuleSourceName(resource, id);
    }
}
