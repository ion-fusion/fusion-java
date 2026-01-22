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

        if (value instanceof Throwable)
        {
            String message =
                "Java Throwables cannot be raised from Fusion code";
            throw new IllegalArgumentException(message, (Throwable) value);
        }

        throw new FusionUserException(value);
    }
}
