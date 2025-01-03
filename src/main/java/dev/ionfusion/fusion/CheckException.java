// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructElt;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructHasKey;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructKeys;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructMerge;
import static dev.ionfusion.fusion.FusionStruct.unsafeStructRemoveKeys;
import java.io.IOException;

@SuppressWarnings("serial")
final class CheckException
    extends FusionErrorException
{
    private final Object myStack;

    CheckException(Object stack)
    {
        super("check failure");
        myStack = stack;
    }

    private void displayField(Evaluator eval, Appendable out,
                              String fieldName, Object value)
        throws IOException, FusionException
    {
        out.append(fieldName);
        out.append(": ");
        FusionIo.write(eval, out, value);
        out.append('\n');
    }

    private void displayField(Evaluator eval, Appendable out,
                              Object frame, String fieldName)
        throws IOException, FusionException
    {
        if (unsafeStructHasKey(eval, frame, fieldName))
        {
            Object v = unsafeStructElt(eval, frame, fieldName);
            displayField(eval, out, fieldName, v);
        }
    }

    private void displayFrame(final Evaluator  eval,
                              final Appendable out,
                              Object frame)
        throws IOException, FusionException
    {
        displayField(eval, out, frame, "name");
        displayField(eval, out, frame, "expression");

        if (unsafeStructHasKey(eval, frame, "actual") ||
            unsafeStructHasKey(eval, frame, "expected"))
        {
            displayField(eval, out, frame, "actual");
            displayField(eval, out, frame, "expected");
        }
        else
        {
            displayField(eval, out, frame, "args");
        }

        frame = unsafeStructRemoveKeys(eval, frame,
                                       new String[]{ "name",
                                                     "expression",
                                                     "actual",
                                                     "expected",
                                                     "args" });

        for (String fieldName : unsafeStructKeys(eval, frame))
        {
            Object value = unsafeStructElt(eval, frame, fieldName);
            displayField(eval, out, fieldName, value);
        }
    }


    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        out.append("Check failure:");

        Object frame = null;
        for (Object stack = myStack;
             FusionSexp.isPair(eval, stack);
             stack = unsafePairTail(eval, stack))
        {
            Object entry = unsafePairHead(eval, stack);

            if (frame == null)
            {
                frame = entry;
            }
            else
            {
                frame = unsafeStructMerge(eval, frame, entry);
            }

            if (unsafeStructHasKey(eval, frame, "name"))
            {
                out.append('\n');
                displayFrame(eval, out, frame);
                frame = null;
            }
        }

        if (frame != null)
        {
            out.append('\n');
            displayFrame(eval, out, frame);
        }
    }
}
