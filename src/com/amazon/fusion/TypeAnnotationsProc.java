// Copyright (c) 2012-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionList.immutableList;
import static com.amazon.fusion.FusionValue.annotations;

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
