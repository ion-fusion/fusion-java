// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import static dev.ionfusion.fusion.FusionBool.makeBool;

final class NotProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        boolean truthy = FusionValue.isTruthy(eval, arg);
        return makeBool(eval, !truthy);
    }
}
