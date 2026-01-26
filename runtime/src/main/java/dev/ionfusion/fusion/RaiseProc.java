// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

final class RaiseProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object value)
        throws FusionException
    {
        if (value instanceof FusionException)
        {
            throw (FusionException) value;
        }

        throw FusionUserException.make(eval, value);
    }
}
