// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.isList;
import static dev.ionfusion.fusion.FusionList.unsafeListCopy;
import static dev.ionfusion.fusion.FusionList.unsafeListSize;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpSize;


final class ApplyProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(2, args);

        int arity = args.length;

        Procedure proc = checkProcArg(0, args);

        Object rest = args[arity - 1];
        boolean restIsList = isList(eval, rest);
        boolean restIsSexp = isSexp(eval, rest);

        int restLen;
        if (restIsList)
        {
            restLen = unsafeListSize(eval, rest);
        }
        else if (restIsSexp)
        {
            restLen = unsafeSexpSize(eval, rest);
        }
        else
        {
            throw argFailure("list or sexp", arity - 1, args);
        }

        // TODO if proc accepts a rest argument, optimize this copying.
        //   As it stands we'll copy to an array, then to a sexp.

        int procArity = restLen + arity - 2;
        Object[] procArgs = new Object[procArity];

        int arg = 0;
        for (int i = 1; i < arity - 1; i++)
        {
            procArgs[arg++] = args[i];
        }

        if (restIsList)
        {
            unsafeListCopy(eval, rest, 0, procArgs, arg, restLen);
        }
        else
        {
            while (isPair(eval, rest))
            {
                procArgs[arg++] = unsafePairHead(eval, rest);
                rest = unsafePairTail(eval, rest);
            }
        }

        return eval.bounceTailCall(proc, procArgs);
    }
}
