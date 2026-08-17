// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime._private.cover.CoverageCollector;
import dev.ionfusion.runtime.base.CodePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
final class CoverageEvaluator
    extends Evaluator
{
    private final CoverageCollector myCollector;

    CoverageEvaluator(GlobalState globalState,
                      CoverageCollector collector)
    {
        super(globalState);
        myCollector = collector;
    }

    private CoverageEvaluator(CoverageEvaluator outer)
    {
        super(outer);
        myCollector = outer.myCollector;
    }


    @Override
    Evaluator addContinuationFrame()
    {
        return new CoverageEvaluator(this);
    }


    @Override
    Compiler makeCompiler()
    {
        return new CoverageCompiler();
    }


    /**
     * An extended {@link Compiler} that generates instrumented code.
     */
    private final class CoverageCompiler
        extends Compiler
    {
        CoverageCompiler()
        {
            super(CoverageEvaluator.this);
        }

        @Override
        CompiledForm compileExpression(Environment env, SyntaxValue source)
            throws FusionException
        {
            CompiledForm form = super.compileExpression(env, source);

            CodePosition loc = source.getLocation();
            if (loc != null)
            {
                if (myCollector.locationIsRecordable(loc))
                {
                    AtomicInteger counter = myCollector.locationInstrumented(loc);
                    form = new CoverageCompiledForm(counter, form);
                }
            }

            return form;
        }
    }


    /**
     * Decorator that notifies the {@link CoverageCollector} when a
     * form has been evaluated.
     */
    private static final class CoverageCompiledForm
        implements CompiledForm
    {
        private final AtomicInteger myCounter;
        private final CompiledForm  myForm;

        CoverageCompiledForm(AtomicInteger counter, CompiledForm decorated)
        {
            myCounter = counter;
            myForm    = decorated;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            myCounter.getAndIncrement();

            // TODO Eliminate tail-call?
            return myForm.doEval(eval, store);
        }
    }
}
