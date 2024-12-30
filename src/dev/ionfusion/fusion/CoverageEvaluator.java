// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 *
 */
final class CoverageEvaluator
    extends Evaluator
{
    private final _Private_CoverageCollector myCollector;

    CoverageEvaluator(GlobalState globalState,
                      _Private_CoverageCollector collector)
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

            SourceLocation loc = source.getLocation();
            if (loc != null)
            {
                if (myCollector.locationIsRecordable(loc))
                {
                    form = new CoverageCompiledForm(loc, form);

                    myCollector.locationInstrumented(loc);
                }
            }

            return form;
        }
    }


    /**
     * Decorator that notifies the {@link _Private_CoverageCollector} when a
     * form has been evaluated.
     */
    private static final class CoverageCompiledForm
        implements CompiledForm
    {
        private final SourceLocation myLocation;
        private final CompiledForm   myForm;

        CoverageCompiledForm(SourceLocation location,
                             CompiledForm   decorated)
        {
            myLocation  = location;
            myForm      = decorated;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            _Private_CoverageCollector collector =
                ((CoverageEvaluator) eval).myCollector;
            collector.locationEvaluated(myLocation);

            // TODO Eliminate tail-call?
            return myForm.doEval(eval, store);
        }
    }
}
