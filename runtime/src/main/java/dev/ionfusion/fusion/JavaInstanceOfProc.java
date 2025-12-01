// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;

final class JavaInstanceOfProc
    extends Procedure2
{
    @Override
    Object doApply(Evaluator eval, Object arg0, Object arg1)
        throws FusionException
    {
        Class<?> klass = checkArg(Class.class, "arg", 0, arg0);

        return makeBool(eval, klass.isInstance(arg1));
    }
}
