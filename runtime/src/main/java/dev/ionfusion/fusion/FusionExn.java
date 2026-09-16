// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime.base.FusionException;


final class FusionExn
{
    private FusionExn() {}

    static final class UnsafeExnMessageProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            String message = ((FusionException) arg).getBaseMessage();
            return FusionString.makeString(eval, message);
        }
    }
}
