// Copyright (c) 2013-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionText.checkNonEmptyTextArg;


final class AnnotateProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(1, args);
        int arity = args.length;

        // We don't try to optimize this by looking for symbol args and
        // avoiding the symbol->String->symbol conversion. This is intentional:
        // it ensures that the annotations are not themselves annotated!

        String[] annotations = new String[arity - 1];
        for (int i = 0; i < arity - 1; i++)
        {
            String a = checkNonEmptyTextArg(eval, this, i+1, args);
            annotations[i] = a;
        }

        Object target = args[0];

        if (FusionValue.isAnnotatable(eval, target))
        {
            return FusionValue.annotate(eval, target, annotations);
        }

        throw argFailure("annotatable type", 0, args);
    }
}
