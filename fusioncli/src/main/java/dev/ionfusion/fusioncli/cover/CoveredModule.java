// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import dev.ionfusion.runtime._private.cover.CoverageDatabase;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.ResourceIdentifier;
import java.net.URI;
import java.nio.file.Path;

/**
 * Represents a module and associated coverage metrics.
 * <p>
 * One subtle challenge is that a module may have coverage data using two different
 * resources: one test session might load the module via a concrete path, while
 * another might load it from a jar.  This class normalizes such duplicates to a
 * preferred form, which is the first concrete path encountered, else the first jar.
 * <p>
 * This normalization should perhaps be happening in the {@link CoverageDatabase}.
 */
public class CoveredModule
    extends CoveredEntity
{
    private final ModuleIdentity myId;

    /**
     * Tracks the resource we want to present for this module, preferring a Path-based
     * resource if one exists.
     */
    private ResourceIdentifier myPreferredSource;


    CoveredModule(ModuleIdentity id)
    {
        myId = id;
    }


    public ModuleIdentity getId()
    {
        return myId;
    }

    @Override
    public String describe()
    {
        return myId.absolutePath();
    }

    /**
     * @return the URI of the preferred source.
     * Can be null for synthetic parent modules.
     */
    @Override
    public URI getUri()
    {
        return (myPreferredSource == null ? null : myPreferredSource.getUri());
    }

    /**
     * @return the path of the preferred source.
     * Can be null for synthetic parent modules.
     */
    @Override
    public Path getPath()
    {
        return (myPreferredSource == null ? null : myPreferredSource.getPath());
    }


    void noteResource(ResourceIdentifier rsrc)
    {
        if (myPreferredSource == null)
        {
            myPreferredSource = rsrc;
        }
        else
        {
            Path preferredPath = myPreferredSource.getPath();
            Path givenPath = rsrc.getPath();
            if (preferredPath == null)
            {
                // Prefer a Path-based source over a URL-based one.
                if (givenPath != null) myPreferredSource = rsrc;
            }
            else
            {
                // We don't expect the same module to come from two different
                // concrete files.
                assert givenPath == null || preferredPath.equals(givenPath);
            }
        }
    }
}
