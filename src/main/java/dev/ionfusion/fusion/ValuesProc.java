// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


final class ValuesProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args) throws FusionException
    {
        int arity = args.length;

        switch (arity)
        {
            case 0:
            {
                return FusionUtils.EMPTY_OBJECT_ARRAY;
            }
            case 1:
            {
                return args[0];
            }
            default:
            {
                return args;
            }
        }
    }
}
