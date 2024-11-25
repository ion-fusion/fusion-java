// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionList.isList;
import static com.amazon.fusion.FusionList.unsafeListAdd;
import static com.amazon.fusion.FusionSequence.checkNullableSequenceArg;
import static com.amazon.fusion.FusionSexp.unsafeSexpAdd;


final class AddProc
    extends Procedure2
{
    @Override
    Object doApply(Evaluator eval, Object sequence, Object element)
        throws FusionException
    {
        checkNullableSequenceArg(eval, this, 0, sequence, element);

        if (isList(eval, sequence))
        {
            return unsafeListAdd(eval, sequence, element);
        }

        return unsafeSexpAdd(eval, sequence, element);
    }
}
