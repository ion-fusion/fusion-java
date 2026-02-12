// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import dev.ionfusion.fusion._Private_CoverageCollector;
import dev.ionfusion.fusion._private.InternMap;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.embed.FusionRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Implements code-coverage metrics collection.
 * <p>
 * The collector is given a data directory from which it reads configuration
 * and where it persists its metrics database.  This allows multiple runtime
 * launches to contribute to the same set of metrics.  That's common during
 * unit testing where each test case uses a fresh {@link FusionRuntime}.
 * <p>
 * At present, only file-based sources are instrumented. This includes sources
 * loaded from a file-based {@code ModuleRepository} as well as scripts from
 * other locations.
 *
 * @see CoverageConfiguration
 */
public final class CoverageCollectorImpl
    implements _Private_CoverageCollector
{
    /**
     * TODO remove when {@link InternMap} supports direct key comparison.
     * @see CoverageCollectorFactory#createSession(Path)
     */
    private final Path                  myDataDir;
    private final CoverageConfiguration myConfig;

    /** Where we store our metrics. */
    private final CoverageDatabase myDatabase;


    CoverageCollectorImpl(Path                  dataDir,
                          CoverageConfiguration config,
                          CoverageDatabase      database)
    {
        myDataDir  = dataDir;
        myConfig   = config;
        myDatabase = database;
    }


    CoverageDatabase getDatabase()
    {
        return myDatabase;
    }


    public void noteRepository(File repoDir)
    {
        myDatabase.noteRepository(repoDir);
    }


    @Override
    public boolean locationIsRecordable(SourceLocation loc)
    {
       return (myDatabase.locationIsRecordable(loc) &&
               myConfig.locationIsSelected(loc));
    }


    @Override
    public void locationInstrumented(SourceLocation loc)
    {
        myDatabase.locationInstrumented(loc);
    }


    @Override
    public void locationEvaluated(SourceLocation loc)
    {
        myDatabase.locationEvaluated(loc);
    }


    @Override
    public void flushMetrics()
        throws IOException
    {
        try
        {
            myDatabase.write();
        }
        catch (IOException e)
        {
            throw new IOException("Error writing Fusion coverage data", e);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) { return false; }
        CoverageCollectorImpl that = (CoverageCollectorImpl) o;
        return Objects.equals(this.myDataDir, that.myDataDir);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(myDataDir);
    }
}
