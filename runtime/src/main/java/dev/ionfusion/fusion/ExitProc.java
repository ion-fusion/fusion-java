// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


/**
 * Throws an {@link ExitException}, which will generally cause the runtime
 * to exit its evaluation.
 */
final class ExitProc
    extends Procedure0
{
    @Override
    Object doApply(Evaluator eval)
        throws ExitException
    {
        throw new ExitException();
    }
}
