// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * A procedure of exactly two arguments.
 * This class performs arity checking before invoking the subclass
 * implementation of {@link #doApply(Evaluator, Object, Object)}.
 */
abstract class Procedure2
    extends Procedure
{
    abstract Object doApply(Evaluator eval, Object arg0, Object arg1)
        throws FusionException;

    @Override
    final Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(2, args);
        return doApply(eval, args[0], args[1]);
    }
}
