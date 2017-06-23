// Copyright (c) 2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * This class is an internal procedure used by the with_handlers form.
 * It evaluates a thunk, catches FusionExceptions returned by the evaluation,
 *   and applies the exception value to the cond expression passed in.
 *
 * It is NOT an implementation of Racket's call-with-exception-handler which
 *   evaluates a thunk with a given exception handler.
 */
final class CallWithHandlerProc
    extends Procedure2
{
    @Override
    Object doApply(Evaluator eval, Object thunk, Object cond)
        throws FusionException
    {
        try
        {
            Procedure thunker = (Procedure) thunk;
            return eval.callNonTail(thunker);
        }
        catch (FusionException e)
        {
            Procedure conder = (Procedure) cond;

            Object exn = e instanceof FusionUserException
                         ? ((FusionUserException) e).getExceptionValue()
                         : e;

            Object rethrowSentinel = new String("sentinel value");

            Object r = eval.callNonTail(conder, exn, rethrowSentinel);

            if (r == rethrowSentinel)
            {
                throw e;
            }

            return r;
        }
    }
}
