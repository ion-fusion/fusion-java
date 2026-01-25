// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.TestSetup.testDataFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.embed.ModuleBuilder;
import dev.ionfusion.runtime.embed.TopLevel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class InterruptionTest
    extends CoreTestCase
{
    private abstract static class EvaluationTask
        implements Runnable
    {
        volatile Throwable   myCaughtThrowable;
        volatile boolean     myEvaluationFinished;

        abstract void eval() throws Exception;

        @Override
        public final void run()
        {
            try
            {
                eval();
                myEvaluationFinished = true;
            }
            catch (Throwable t)
            {
                myCaughtThrowable = t;
            }
        }
    }


    private static final String INTERRUPT_MODULE = "/interrupt";

    private static class RequestInterruptProc
        extends Procedure0
    {
        final CountDownLatch latch = new CountDownLatch(1);

        @Override
        Object doApply(Evaluator eval)
        {
            latch.countDown();
            return voidValue(eval);
        }
    }


    private void expectInterruption(EvaluationTask task)
        throws Exception
    {
        useTstRepo();

        RequestInterruptProc latchProc = new RequestInterruptProc();
        ModuleBuilder mb = runtime().makeModuleBuilder(INTERRUPT_MODULE);
        mb.define("ready_for_interrupt", latchProc);
        mb.instantiate();

        Thread thread = new Thread(task);
        thread.start();

        if (! latchProc.latch.await(5, TimeUnit.SECONDS))
        {
            if (task.myEvaluationFinished)
            {
                throw new AssertionError("Task finished before latch");
            }

            if (task.myCaughtThrowable != null)
            {
                throw new AssertionError("Task threw exception before latch",
                                         task.myCaughtThrowable);
            }
            fail("latch timed out");
        }

        thread.interrupt();
        thread.join();

        assertFalse(task.myEvaluationFinished);

        if (! (task.myCaughtThrowable instanceof FusionInterruptedException))
        {
            throw new AssertionError("Unexpected exception",
                                     task.myCaughtThrowable);
        }
    }


    //========================================================================


    @Test
    public void interruptMakeTopLevel()
        throws Exception
    {
        // Bootstrap our FusionRuntime with the bare minimum.
        runtimeBuilder().setDefaultLanguage("/fusion/private/kernel");

        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                runtime().makeTopLevel("/interrupt_on_instantiation");
            }
        };

        expectInterruption(task);

        // The code above aborts compiling and loading of /fusion.
        // Now check that we can can successfully load the rest of /fusion.

        runtime().makeTopLevel("/fusion");
    }


    @Test
    public void interruptRequireModule()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.requireModule("/interrupt_on_instantiation");
            }
        };

        expectInterruption(task);
    }


    private static final String INTERRUPTABLE_EXPRESSION =
        "(begin (ready_for_interrupt) (let loop [] (loop)))";

    @Test
    public void interruptEvalReader()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.requireModule(INTERRUPT_MODULE);

                IonReader reader =
                    system().newReader(INTERRUPTABLE_EXPRESSION);
                top.eval(reader);
            }
        };

        expectInterruption(task);
    }


    @Test
    public void interruptEvalString()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.requireModule(INTERRUPT_MODULE);
                top.eval(INTERRUPTABLE_EXPRESSION);
            }
        };

        expectInterruption(task);
    }


    @Test
    public void interruptLoad()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.load(testDataFile("interrupt.fusion").toFile());
            }
        };

        expectInterruption(task);
    }


    @Test
    public void interruptCallByName()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.requireModule(INTERRUPT_MODULE);
                top.eval("(define (proc) " + INTERRUPTABLE_EXPRESSION + ")");
                top.call("proc");
            }
        };

        expectInterruption(task);
    }


    @Test
    public void interruptCallByValue()
        throws Exception
    {
        EvaluationTask task = new EvaluationTask()
        {
            @Override
            void eval() throws Exception
            {
                TopLevel top = topLevel();
                top.requireModule(INTERRUPT_MODULE);
                top.eval("(define (proc) " + INTERRUPTABLE_EXPRESSION + ")");
                Object proc = top.lookup("proc");
                top.call(proc);
            }
        };

        expectInterruption(task);
    }
}
