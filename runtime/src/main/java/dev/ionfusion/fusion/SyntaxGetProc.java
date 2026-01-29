// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.safeWrite;
import static dev.ionfusion.fusion.FusionNumber.checkIntArgToJavaInt;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion._private.FusionUtils.writeFriendlyIndex;

final class SyntaxGetProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(1, args);
        SyntaxContainer stx = checkSyntaxContainerArg(eval, 0, args);
        SyntaxValue value = stx;

        final int lastArg = args.length - 1;
        for (int i = 1; i <= lastArg; i++)
        {
            if (stx instanceof SyntaxSequence)
            {
                int index = checkIntArgToJavaInt(eval, this, i, args);
                SyntaxSequence s = (SyntaxSequence) stx;
                if (s.size(eval) <= index)
                {
                    return voidValue(eval);
                }
                value = s.get(eval, index);
            }
            else
            {
                String field = checkRequiredTextArg(eval, this, i, args);
                SyntaxStruct s = (SyntaxStruct) stx;
                value = s.get(eval, field);
            }

            if (value == null) return voidValue(eval);

            if (i < lastArg)
            {
                try
                {
                    stx = (SyntaxContainer) value;
                }
                catch (ClassCastException cce)
                {
                    StringBuilder out = new StringBuilder();
                    out.append("expected syntax container before traversing ");
                    writeFriendlyIndex(out, i + 1);
                    out.append(" argument, had: ");
                    safeWrite(eval, out, value);
                    out.append("\nArguments were:");
                    for (Object arg : args)
                    {
                        out.append("\n  ");
                        safeWrite(eval, out, arg);
                    }
                    String message = out.toString();
                    throw contractFailure(message);
                }
            }
        }

        return value;
    }
}
