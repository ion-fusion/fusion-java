// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * A procedure that accepts no arguments.
 * This class performs arity checking before invoking the subclass
 * implementation of {@link #doApply(Evaluator)}.
 */
abstract class Procedure0
    extends Procedure
{
    Procedure0()
    {
    }

    @Deprecated
    Procedure0(String doc)
    {
        super(doc);
    }

    abstract Object doApply(Evaluator eval)
        throws FusionException;

    @Override
    final Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(0, args);
        return doApply(eval);
    }
}
