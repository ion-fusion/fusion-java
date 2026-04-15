// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

/**
 * A very simple, single-use Fusion breakpoint.
 */
class BreakpointProc
    extends Procedure0
{
    /**
     * Latch that drops to zero when this breakpoint is hit.
     */
    final CountDownLatch myPauseLatch = new CountDownLatch(1);

    /**
     * Latch that drops to zero when this breakpoint should resume.
     */
    final CountDownLatch myResumeLatch = new CountDownLatch(1);


    /**
     * Wait for the Fusion breakpoint to be hit, pausing that thread.
     */
    void await()
        throws InterruptedException
    {
        myPauseLatch.await();
    }

    /**
     * Signal the Fusion thread to continue.
     */
    void resume()
    {
        assertTrue(myResumeLatch.getCount() > 0, "Breakpoint not hit");
        myResumeLatch.countDown();
    }


    @Override
    Object doApply(Evaluator eval)
    {
        try
        {
            myPauseLatch.countDown();     // Signal that we're paused.
            myResumeLatch.await();        // Wait for the resume signal.
        }
        catch (InterruptedException e)
        {
            throw new FusionInterrupt();
        }

        return voidValue(eval);
    }
}
