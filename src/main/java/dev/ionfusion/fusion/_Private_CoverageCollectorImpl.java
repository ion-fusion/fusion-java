// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.io.File;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements code-coverage metrics collection.
 * <p>
 * The collector is given a data directory from which it reads configuration
 * and where it persists its metrics database.  This allows multiple runtime
 * launches to contribute to the same set of metrics.  That's common during
 * unit testing where each test case uses a fresh {@link FusionRuntime}.
 * <p>
 * At present, only file-based sources are instrumented. This includes sources
 * loaded from a file-based {@link ModuleRepository} as well as scripts from
 * other locations.
 * <p>
 * Instances of this class are interned in a weak-reference cache, keyed by
 * data directory.  This allows them to be shared by {@code FusionRuntime}
 * instances for as long as possible, and flushed to disk when they become
 * unreachable or the JVM exits.  This also ensures proper deduplication of the
 * {@link CoverageDatabase} contained within.
 * <p>
 * <em>Flushing and reloading of a database multiple times within a test run is
 * expected and common, particularly within this project.</em>
 * <p>
 * TODO: The caching/flushing mechanism is distinct from this class's main
 * responsibility of coverage filtering, and brings significant complexity to
 * something that should be simple.  The Single Responsibility Principle
 * suggests it should be refactored into a dedicated class.
 *
 * @see CoverageConfiguration
 */
public final class _Private_CoverageCollectorImpl
    implements _Private_CoverageCollector
{
    // TODO Remove _Private_ prefix from this class; it's not used outside of
    //      this package.

    // TODO JAVA8 Replace with a Predicate<SourceLocation>
    private final CoverageConfiguration myConfig;

    /** Where we store our metrics. */
    private final CoverageDatabase myDatabase;


    private _Private_CoverageCollectorImpl(File dataDir)
        throws FusionException, IOException
    {
        myConfig   = new CoverageConfiguration(dataDir);
        myDatabase = new CoverageDatabase(dataDir.toPath());
    }


    /**
     * Polled by {@link Flusher#run()}.
     * <p>
     * TODO: Can we streamline this to {@code ReferenceQueue<Closeable>}?
     */
    private static final
    ReferenceQueue<_Private_CoverageCollectorImpl> ourReferenceQueue =
        new ReferenceQueue<>();


    // TODO A soft reference might be even better, to retain the collector as
    //      long as possible.
    private static final class CollectorRef
        extends WeakReference<_Private_CoverageCollectorImpl>
    {
        private File             myFile;
        private CoverageDatabase myDatabase;

        CollectorRef(_Private_CoverageCollectorImpl referent)
        {
            super(referent, ourReferenceQueue);

            myDatabase = referent.myDatabase;
        }

        /**
         * Carefully synchronized since this can be called in a race between
         * {@link Flusher} and {@link #fromDirectory(File)}, depending on which
         * detects collection first.
         */
        void flushDatabase()
            throws IOException
        {
            try
            {
                synchronized (this)
                {
                    if (myDatabase != null)
                    {
                        // If we fail to write the database, don't try it again.
                        CoverageDatabase db = myDatabase;
                        myDatabase = null;
                        db.write();
                    }
                }
            }
            finally
            {
                // FIXME The `myFile` is never initialized and is always null!
                removeFromCache(myFile, this);
            }
        }
    }


    /**
     * Polls {@link #ourReferenceQueue} to write coverage data to disk when a
     * collector is garbage collected.
     * <p>
     * TODO: It could be simpler to handle {@link java.io.Closeable}.
     */
    private static class Flusher
        implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    try
                    {
                        CollectorRef ref =
                            (CollectorRef) ourReferenceQueue.remove();
                        ref.flushDatabase();
                    }
                    catch (InterruptedException e)  // TODO Why is this needed?
                    {
                        break;
                    }
                    catch (IOException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            finally
            {
                synchronized (Flusher.class)
                {
                    ourFlusherThread = null;
                }
            }
        }


        private static Thread ourFlusherThread = null;

        static synchronized void start()
        {
            if (ourFlusherThread == null)
            {
                ourFlusherThread =
                    new Thread(new Flusher(),
                               "Fusion coverage metrics flusher");
                ourFlusherThread.setDaemon(true);
                ourFlusherThread.start();
            }
        }

        /** Called when there are no more references to flush. */
        static synchronized void stop()
        {
            if (ourFlusherThread != null)
            {
                ourFlusherThread.interrupt();
                ourFlusherThread = null;
            }
        }
    }


    // TODO Consider moving the cache into its own class, to better
    //   organize/encapsulate the complicated caching/threading logic.
    private static Map<File,CollectorRef> ourCollectorCache;

    private static Thread ourShutdownHook;
    private static boolean ourShutdownHasStarted;


    /**
     * Called before each access to the {@link #ourCollectorCache}.
     */
    private static synchronized void initCache()
        throws FusionException
    {
        if (ourCollectorCache == null)
        {
            ourCollectorCache = new HashMap<>();
        }

        if (ourShutdownHook == null)
        {
            // TODO JAVA8 Use a lambda
            ourShutdownHook = new Thread()
            {
                @Override
                public void run()
                {
                    emptyCache();
                }
            };

            try
            {
                Runtime.getRuntime().addShutdownHook(ourShutdownHook);
            }
            catch (IllegalStateException e)
            {
                throw new FusionException("The JMV is shutting down.");
            }
        }
        else if (ourShutdownHasStarted)
        {
            throw new FusionException("The JMV is shutting down.");
        }
    }


    private static synchronized void addToCache(File file,  CollectorRef ref)
    {
        ourCollectorCache.put(file, ref);

        Flusher.start();
    }


    /**
     * Remove an entry from the cache AFTER it has been flushed.
     */
    private static synchronized void removeFromCache(File file,
                                                     CollectorRef ref)
    {
        assert ref.get() == null;

        CollectorRef current = ourCollectorCache.get(file);
        if (current == ref)
        {
            ourCollectorCache.remove(file);
        }

        if (ourCollectorCache.isEmpty())
        {
            // TODO How do we know this?
            assert ourShutdownHook != null;
            try
            {
                Runtime.getRuntime().removeShutdownHook(ourShutdownHook);
            }
            catch (IllegalStateException e)
            {
                // The JMV is shutting down. Nothing to do.
            }

            Flusher.stop();
        }
    }

    /**
     * Called by the JVM shutdown hook, to ensure everything gets flushed.
     * <p>
     * Per {@link Runtime#addShutdownHook}, "daemon threads will continue to
     * run during the shutdown sequence, as will non-daemon threads if shutdown
     * was initiated by invoking the exit method."
     * <p>
     * TODO This is a risky: the database could be large and flushing may thus
     *      take too long for a shutdown hook.  We should instead have an active
     *      {@code close()} protocol, perhaps on {@link FusionRuntime}, so that
     *      the application flushes coverage data in a controlled manner.
     */
    private static synchronized void emptyCache()
    {
        ourShutdownHasStarted = true;

        // Snapshot the values in the cache, to avoid concurrent
        // modification problems. If the cache isn't empty, Flusher thread is
        // still running.
        Set<CollectorRef> files = new HashSet<>(ourCollectorCache.values());
        for (CollectorRef ref : files)
        {
            // TODO Why would ref be null?
            if (ref != null)
            {
                ref.clear();
                try
                {
                    ref.flushDatabase();
                }
                catch (IOException e)
                {
                    String message = "Error writing coverage data";
                    throw new RuntimeException(message, e);
                }
            }
        }
    }

    public static synchronized
    _Private_CoverageCollectorImpl fromDirectory(File dataDir)
        throws FusionException
    {
        initCache();

        // Canonicalize the dataDir, so different paths to the same directory
        // don't lead to collectors overwriting each other's database.
        try
        {
            dataDir = dataDir.getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new FusionException("Error getting canonical path", e);
        }

        _Private_CoverageCollectorImpl collector;

        CollectorRef ref = ourCollectorCache.get(dataDir);
        if (ref != null)
        {
            collector = ref.get();
            if (collector != null) return collector;

            // The prior collector has been GCed, so write any remaining state.
            // We want this to happen BEFORE creating a new collector using
            // the same directory.
            try
            {
                // TODO Could we instead extract the database and reuse it with
                //   a new collector?  That would avoid the write/read cycle.
                ref.flushDatabase();
            }
            catch (IOException e)
            {
                throw new FusionException("Error writing coverage data", e);
            }
            // ref is now useless. Fall through and create a new one.
        }

        try
        {
            collector = new _Private_CoverageCollectorImpl(dataDir);
        }
        catch (IOException e)
        {
            throw new FusionException("Error reading coverage data", e);
        }

        addToCache(dataDir, new CollectorRef(collector));

        return collector;
    }

    public static _Private_CoverageCollectorImpl fromDirectory(String dataDir)
        throws FusionException
    {
        return fromDirectory(new File(dataDir));
    }


    //=========================================================================
    // Managing the coverage data


    void noteRepository(File repoDir)
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
        throws FusionException
    {
        try
        {
            myDatabase.write();
        }
        catch (IOException e)
        {
            throw new FusionException("Error writing coverage data", e);
        }
    }
}
