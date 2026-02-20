// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli.cover;

import dev.ionfusion.runtime._private.cover.CoverageDatabase;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a module and associated coverage metrics.
 * <p>
 * One subtle challenge is that a module may have coverage data using two different
 * source names: one test session might load the module via a concrete path, while
 * another might load it from a jar.  This class normalizes such duplicates to a
 * preferred form, which is the first concrete path encountered, else the first jar.
 * <p>
 * This normalization should really be happening in the {@link CoverageDatabase}.
 */
public class CoveredModule
    extends CoveredEntity
{
    private final ModuleIdentity myId;

    /**
     * Tracks the source code we want to present for this module.
     * We prefer a file-based source if one exists.
     */
    private SourceName myPreferredSource;

    /**
     * Remembers that we've processed a particular source to avoid the clunky
     * normalization for every location.
     */
    private final Set<SourceName> myNames;


    CoveredModule(ModuleIdentity id)
    {
        myId = id;
        myNames = new HashSet<>();
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
     */
    @Override
    public URI getUri()
    {
        return (myPreferredSource == null ? null : myPreferredSource.getUri());
    }

    /**
     * @return the path of the preferred source.
     */
    @Override
    public Path getPath()
    {
        return (myPreferredSource == null ? null : myPreferredSource.getPath());
    }


    void noteSourceName(SourceName sourceName)
    {
        if (myNames.add(sourceName))
        {
            if (myPreferredSource == null)
            {
                myPreferredSource = sourceName;
            }
            else
            {
                Path preferredPath = myPreferredSource.getPath();
                Path givenPath = sourceName.getPath();
                if (preferredPath == null)
                {
                    // Prefer a Path-based source over a URL-based one.
                    if (givenPath != null) myPreferredSource = sourceName;
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


    /**
     * Returns an equivalent location using our preferred source name.
     */
    public SourceLocation normalizeLocation(SourceLocation loc)
    {
        assert myPreferredSource != null;
        if (loc.getSourceName().equals(myPreferredSource))
        {
            return loc;
        }

        return SourceLocation.forLineColumn(loc.getLine(),
                                            loc.getColumn(), myPreferredSource);
    }


    /**
     * @param loc must have been {@link #normalizeLocation normalized}.
     */
    public void noteLocationCoverage(SourceLocation loc, Boolean covered)
    {
        assert myId == loc.getSourceName().getModuleIdentity();

        // Assume the caller has normalized the location, otherwise we'll have
        // two different keys for the same effective location.
        super.noteLocationCoverage(loc, covered);
    }
}
