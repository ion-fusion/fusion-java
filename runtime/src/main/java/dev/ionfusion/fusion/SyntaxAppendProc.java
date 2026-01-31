// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


final class SyntaxAppendProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(eval, 1, args);
        SyntaxSequence seq = checkSyntaxSequenceArg(eval, 0, args);
        for (int i = 1; i < args.length; i++)
        {
            SyntaxSequence next = checkSyntaxSequenceArg(eval, i, args);
            seq = seq.makeAppended(eval, next);
            if (seq == null)
            {
                throw argError(eval, "proper sequence", i-1, args);
            }
        }
        return seq;
    }
}
