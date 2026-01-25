// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * This class is an internal procedure used by the with_handlers form.
 * It evaluates a thunk, catches FusionExceptions returned by the evaluation,
 *   and applies the exception value to the cond expression passed in.
 *
 * It is NOT the same as Racket's call-with-exception-handler, since this
 * unwinds the stack before calling the handler.
 */
final class CallWithHandlerProc
    extends Procedure2
{
    @Override
    Object doApply(Evaluator eval, Object handler, Object thunk)
        throws FusionException
    {
        // TODO Check arity of both procs before installing the handler.
        checkArg(eval, Procedure.class, "1-arg procedure", 0, handler, thunk);
        checkArg(eval, Procedure.class, "0-arg procedure", 1, handler, thunk);

        try
        {
            Procedure thunker = (Procedure) thunk;
            return eval.callNonTail(thunker);
        }
        catch (FusionException e)
        {
            Procedure h = (Procedure) handler;

            Object raised = e.getRaisedValue();
            return eval.bounceTailCall(h, raised);
        }
    }
}
