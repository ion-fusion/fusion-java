// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli.cover;

import dev.ionfusion.runtime.base.SourceLocation;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Collects location coverage data for some module or file.
 */
abstract class CoveredEntity
{
    private final Map<SourceLocation, Boolean> myCoverage;
    private final CoverageInfoPair             mySummary;

    CoveredEntity()
    {
        myCoverage = new HashMap<>();
        mySummary = new CoverageInfoPair();
    }


    /**
     * Describes this entity for use by the index page.
     */
    abstract String describe();

    /**
     * Our source URI.
     *
     * @return an absolute URI with either "file" or "jar" scheme; not null.
     */
    abstract URI getUri();

    /**
     * Our concrete source file, if any.
     */
    abstract Path getPath();


    void noteLocationCoverage(SourceLocation loc, Boolean covered)
    {
        assert loc.getSourceName().getUri().equals(getUri());
        myCoverage.merge(loc, covered, (a, b) -> a || b);
    }


    Set<SourceLocation> locations()
    {
        return myCoverage.keySet();
    }

    boolean isLocationCovered(SourceLocation loc)
    {
        return myCoverage.get(loc);
    }


    void summarizeInto(CoverageInfoPair summary)
    {
        myCoverage.values().forEach(summary::foundExpression);
    }

    /**
     * Computes summary metrics from the noted locations.
     */
    void summarize()
    {
        summarizeInto(mySummary);
    }


    /**
     * Aggregated coverage metrics for this entity.
     * The result is only valid after {@link #summarize()} has been called.
     *
     * @return not null.
     */
    CoverageInfoPair getSummary()
    {
        return mySummary;
    }
}
