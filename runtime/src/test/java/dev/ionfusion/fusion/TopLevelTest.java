// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.TopLevel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TopLevelTest
    extends CoreTestCase
{
    @Test
    public void testLookupFromImport()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("pair");
        assertTrue(FusionProcedure.isProcedure(top, fv));
    }

    @Test
    public void testLookupNoBinding()
        throws Exception
    {
        TopLevel top = topLevel();
        Object fv = top.lookup("no binding!");
        assertNull(fv);
    }

    @Test
    public void testDefineAndLookup()
        throws Exception
    {
        TopLevel top = topLevel();

        top.define("v", 12);
        Object fv = top.lookup("v");
        assertTrue(FusionNumber.isInt(top, fv));

        top.define("A questionable name", null);
        fv = top.lookup("A questionable name");
        assertTrue(FusionVoid.isVoid(top, fv));
    }

    @Test
    public void testCallProcByValue()
        throws Exception
    {
        TopLevel top = topLevel();
        Object plus = top.lookup("+");
        assertTrue(FusionProcedure.isProcedure(top, plus));
        checkLong(3, top.call(plus, 1, 2));
    }


    /**
     * Tests behavior of top-level forward references evaluated simultaneously to the
     * corresponding definition.
     */
    @Test
    public void testRacingFreeVariableReference()
        throws Exception
    {
        TopLevel top = topLevel();

        BreakpointProc defnBreak = new BreakpointProc();
        top.define("defn_break", defnBreak);

        BreakpointProc initBreak = new BreakpointProc();
        top.define("init_break", initBreak);

        // Run a script in a worker thread. We'll use the above latches to step
        // it through the various stages of initialization.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
        {
            Future<Object> worker = executor.submit(() -> {
                try
                {
                    String src = "(begin                      \n" +
                                 "  (define (getter) delayed) \n" + // forward reference
                                 "  (defn_break)              \n" +
                                 "  (define delayed           \n" +
                                 "    (begin                  \n" +
                                 "      (init_break)          \n" +
                                 "      '''initialized'''))   \n" +
                                 "  '''defined''')";
                    return top.eval(src);
                }
                catch (FusionException e)
                {
                    throw new RuntimeException(e);
                }
            });

            // Wait for the worker to define the forward-referencing getter proc.
            defnBreak.await();
            expectUnboundIdentifierExn("(getter)", "delayed");

            // Let the worker start evaluating the definition.
            defnBreak.resume();
            initBreak.await();

            // The worker is evaluating the definition but has not initialized the
            // new slot.
            expectUnboundIdentifierExn("(getter)", "delayed");

            // Let the worker complete the delayed initialization.
            initBreak.resume();

            // Wait for the worker to complete.
            Object workerResult = worker.get();  // Throws exn if the thread threw one.
            checkString("defined", workerResult);

            Object getterResult = eval("(getter)");
            checkString("initialized", getterResult);
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}
