// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import dev.ionfusion.fusion._private.InternMap;
import dev.ionfusion.runtime._private.util.Flusher;
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
    private static final Flusher ourFlusher = new Flusher("Fusion coverage flusher");

    private static final InternMap<Path, CoverageCollectorImpl> ourSessions =
        new InternMap<>(CoverageCollectorFactory::createSession);


    public static CoverageCollectorImpl fromDirectory(Path dataDir)
        throws IOException
    {
        // Canonicalize the path for more reliable session sharing.
        dataDir = dataDir.toRealPath();

        try
        {
            return ourSessions.intern(dataDir);
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
    static CoverageCollectorImpl createSession(Path dataDir)
        throws UncheckedIOException
    {
        try
        {
            CoverageConfiguration config =
                CoverageConfiguration.forDataDir(dataDir);
            CoverageDatabase database =
                CoverageDatabase.openSession(dataDir);
            CoverageCollectorImpl collector =
                new CoverageCollectorImpl(dataDir, config, database);

            ourFlusher.register(collector, database::uncheckedWrite);

            return collector;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
