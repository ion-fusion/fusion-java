// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

final class RaiseProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        return FusionException.raise(eval, arg);
    }
}
