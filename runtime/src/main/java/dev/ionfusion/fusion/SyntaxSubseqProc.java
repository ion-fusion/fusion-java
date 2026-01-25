// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;

final class SyntaxSubseqProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityExact(2, args);
        SyntaxSequence sequence = checkSyntaxSequenceArg(eval, 0, args);
        int from = checkIntArgToJavaInt(eval, this, 1, args);
        int size = sequence.size();

        if (size < from) from = size;

        sequence = sequence.makeSubseq(eval, from);
        if (sequence == null)
        {
            throw argError(eval, "proper sequence", 0, args);
        }
        return sequence;
    }
}
