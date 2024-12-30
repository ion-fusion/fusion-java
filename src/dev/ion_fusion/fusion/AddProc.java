// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionList.isList;
import static dev.ion_fusion.fusion.FusionList.unsafeListAdd;
import static dev.ion_fusion.fusion.FusionSequence.checkNullableSequenceArg;
import static dev.ion_fusion.fusion.FusionSexp.unsafeSexpAdd;


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
