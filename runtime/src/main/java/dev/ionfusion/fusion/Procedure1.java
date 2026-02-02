// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * A procedure of exactly one argument.
 * This class performs arity checking before invoking the subclass
 * implementation of {@link #doApply(Evaluator, Object)}.
 */
abstract class Procedure1
    extends Procedure
{
    abstract Object doApply(Evaluator eval, Object arg)
        throws FusionException;

    @Override
    final Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(eval, 1, args);
        return doApply(eval, args[0]);
    }
}
