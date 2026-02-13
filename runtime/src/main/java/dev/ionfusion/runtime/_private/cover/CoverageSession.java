// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempFile;

import dev.ionfusion.fusion._Private_CoverageCollector;
import dev.ionfusion.fusion._private.InternMap;
import dev.ionfusion.runtime._private.util.Flusher;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;


/**
 * Associates a {@link CoverageDatabase} with a storage file. Instances automatically
 * flush their database to disk when they are garbage-collected or when the JVM exits.
 */
public class CoverageSession
    implements _Private_CoverageCollector
{
    private static final Flusher ourFlusher = new Flusher("Fusion coverage session flusher");


    /**
     * TODO remove when {@link InternMap} supports direct key comparison.
     *
     * @see CoverageCollectorFactory#createSession(Path)
     */
    private final Path             mySessionsDir;
    private final Path             myStorageFile;
    private final CoverageDatabase myDatabase;


    private CoverageSession(Path sessions, Path storage, CoverageDatabase database)
    {
        mySessionsDir = sessions;
        myStorageFile = storage;
        myDatabase = database;
    }


    public static CoverageSession createSession(Path sessionsDir)
        throws IOException
    {
        createDirectories(sessionsDir);

        Path sessionFile = createTempFile(sessionsDir, "", ".ion");

        CoverageDatabase db      = new CoverageDatabase();
        CoverageSession  session = new CoverageSession(sessionsDir, sessionFile, db);

        // WARNING: The flush action must not retain a reference to the session!
        ourFlusher.register(session, () -> db.uncheckedWrite(sessionFile));

        return session;
    }


    public Path getStorageFile()
    {
        return myStorageFile;
    }

    public CoverageDatabase getDatabase()
    {
        return myDatabase;
    }


    void noteRepository(File repoDir)
    {
        myDatabase.noteRepository(repoDir);
    }

    @Override
    public boolean locationIsRecordable(SourceLocation loc)
    {
        return myDatabase.locationIsRecordable(loc);
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
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) { return false; }
        CoverageSession that = (CoverageSession) o;
        return Objects.equals(this.mySessionsDir, that.mySessionsDir);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(mySessionsDir);
    }
}
