// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Ensures that cleanup actions happen when an object is GCd or when the JVM shuts down.
 * <p>
 * Basically a simiplified version of the Java 11 {@code java.lang.ref.Cleaner}, with
 * the addition of a shutdown hook for stronger cleanup guarantees.
 */
public final class Flusher
{
    /**
     * Refs that still need to be flushed. To ensure the flush action is invoked only
     * once, a ref must only be flushed immediately after removal from this queue!
     */
    private final ConcurrentLinkedQueue<FlushableRef> myUnflushedRefs =
        new ConcurrentLinkedQueue<>();

    private final Thread myShutdownHook;

    private boolean myRegistrationOpen = true;

    /**
     * Refs that have become unreachable via GC. Polled by {@link #flushUnreachables()}.
     */
    private final ReferenceQueue<Object> myUnreachableRefs = new ReferenceQueue<>();


    public Flusher(String nameOfThread)
    {
        Thread ourFlusherThread = new Thread(this::flushUnreachables, nameOfThread);
        ourFlusherThread.setDaemon(true);
        ourFlusherThread.start();
        // The thread will stop itself when all refs are flushed.

        myShutdownHook = new Thread(this::shutdown);
        Runtime.getRuntime().addShutdownHook(myShutdownHook);
        // Deregistered by flushUnreachables() if we are stopped.
    }


    /**
     * Register an action to run when the object becomes phantom reachable.
     *
     * @param obj the object to monitor.
     * @param action must not retain a reference to {@code toClean}!
     */
    public synchronized void register(Object obj, Runnable action)
    {
        if (myRegistrationOpen)
        {
            myUnflushedRefs.add(new FlushableRef(obj, action));
        }
        else
        {
            throw new IllegalStateException("Flusher is closed to registration.");
        }
    }


    /**
     * Disable all further registrations. Cleanup threads will keep running until all
     * actions have been completed.
     */
    public synchronized void stop()
    {
        myRegistrationOpen = false;
    }

    public synchronized boolean stillRunning()
    {
        return myRegistrationOpen || !myUnflushedRefs.isEmpty();
    }



    private final class FlushableRef
        extends PhantomReference<Object>
    {
        private final Runnable myAction;

        private FlushableRef(Object obj, Runnable action)
        {
            super(obj, myUnreachableRefs);
            myAction = action;
        }

        /**
         * Deregisters the ref and runs the action.
         */
        private void flush()
        {
            if (myUnflushedRefs.remove(this))
            {
                myAction.run();
            }
        }
    }


    /**
     * {@link Runnable} for the main flushing thread. Polls {@link #myUnreachableRefs}
     * to flush items that have been garbage collected.
     */
    private void flushUnreachables()
    {
        while (stillRunning())
        {
            try
            {
                FlushableRef ref = (FlushableRef) myUnreachableRefs.remove(60 * 1000);
                ref.flush();
            }
            catch (InterruptedException e)
            {
                // Thread is interrupted by stop() below.
                break;
            }
            catch (Throwable e)
            {
                // Ignore and keep polling.
            }
        }

        // All registered objects have been flushed, so we can remove the shutdown hook.
        try
        {
            Runtime.getRuntime().removeShutdownHook(myShutdownHook);
        }
        catch (Throwable e)
        {
            // We don't care, just exit the thread.
        }
    }


    /**
     * JVM shutdown hook to ensure everything gets flushed.
     * <p>
     * Per {@link Runtime#addShutdownHook}, "daemon threads will continue to
     * run during the shutdown sequence, as will non-daemon threads if shutdown
     * was initiated by invoking the exit method."
     * <p>
     * This means that {@link #flushUnreachables()} may be simultaneously flushing
     * entries.
     */
    private void shutdown()
    {
        // Ensure that no more objects are registered.
        stop();

        while (!myUnflushedRefs.isEmpty())
        {
            // DO NOT remove the ref from the set!
            // That will prevent the action from firing.
            FlushableRef ref = myUnflushedRefs.peek();
            if (ref != null)
            {
                ref.clear();
                try
                {
                    ref.flush(); // Removes itself from the queue
                }
                catch (Throwable e)
                {
                    // Ignore and keep polling.
                }
            }
        }
    }
}
