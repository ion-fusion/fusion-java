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
    Procedure1()
    {
    }

    @Deprecated
    Procedure1(String doc, String argName)
    {
        super(doc, argName);
    }

    abstract Object doApply(Evaluator eval, Object arg)
        throws FusionException;

    @Override
    final Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(1, args);
        return doApply(eval, args[0]);
    }
}
