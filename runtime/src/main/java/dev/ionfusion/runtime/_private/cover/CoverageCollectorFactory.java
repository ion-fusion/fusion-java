// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;

import dev.ionfusion.fusion._private.InternMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Collector instances are interned in a weak-reference cache, keyed by the
 * data directory.  This allows them to be shared by {@code FusionRuntime}
 * instances for as long as possible, and flushed to disk when they become
 * unreachable or the JVM exits.
 */
public final class CoverageCollectorFactory
{
    private static final InternMap<Path, CoverageSession> ourSessions =
        new InternMap<>(CoverageCollectorFactory::createSession);


    public static CoverageCollectorImpl fromDirectory(Path dataDir)
        throws IOException
    {
        CoverageConfiguration config = CoverageConfiguration.forDataDir(dataDir);
        return fromDirectory(config, dataDir);
    }


    public static CoverageCollectorImpl fromDirectory(CoverageConfiguration config,
                                                      Path dataDir)
        throws IOException
    {
        if (! isDirectory(dataDir))
        {
            // This fails if dataDir is a symlink!
            createDirectories(dataDir);
        }

        // Canonicalize the path for more reliable session sharing.
        Path sessionsDir = dataDir.toRealPath().resolve("sessions");

        try
        {
            CoverageSession session = ourSessions.intern(sessionsDir);
            return new CoverageCollectorImpl(config, session);
        }
        catch (UncheckedIOException e) // from createSession()
        {
            throw e.getCause();
        }
    }


    /**
     * Called by our {@link InternMap} to create an instance.
     * <p>
     * At the moment, every `intern` call invokes this method because it cannot use the
     * directory as the map's key. When it finds a previously interned instance
     * with the same directory, the fresh instance is discarded. This leads to lots of
     * empty session files, which are benign but annoying.
     * <p>
     * When {@link InternMap} properly supports key comparison, then the instance won't
     * need to hold the key.
     *
     * @throws UncheckedIOException so this method can be used as a {@link Runnable}.
     */
    static CoverageSession createSession(Path sessionsDir)
        throws UncheckedIOException
    {
        try
        {
            return CoverageSession.createSession(sessionsDir);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
