// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionList.immutableList;
import static dev.ion_fusion.fusion.FusionValue.annotations;

final class TypeAnnotationsProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        Object[] anns = annotations(eval, arg);

        // Returning immutable list allows us to return a shared structure
        // when possible, avoiding copies.
        return immutableList(eval, anns);
    }
}
