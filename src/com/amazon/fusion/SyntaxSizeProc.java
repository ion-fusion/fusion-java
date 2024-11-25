// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionNumber.makeInt;

final class SyntaxSizeProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        SyntaxSequence c = checkSyntaxSequenceArg(0, arg);
        return makeInt(eval, c.size());
    }
}
