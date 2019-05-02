// Copyright (c) 2012-2018 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionNumber.checkIntArgToJavaInt;

final class SyntaxSubseqProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(2, args);
        SyntaxSequence sequence = checkSyntaxSequenceArg(0, args);
        int from = checkIntArgToJavaInt(eval, this, 1, args);
        int size = sequence.size();

        if (size < from) from = size;

        sequence = sequence.makeSubseq(eval, from);
        if (sequence == null)
        {
            throw new ArgumentException(this, "proper sequence", 0, args);
        }
        return sequence;
    }
}
