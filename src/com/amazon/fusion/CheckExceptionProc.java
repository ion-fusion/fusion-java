// Copyright (c) 2013-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionString.stringToJavaString;
import static com.amazon.fusion.FusionVoid.voidValue;
import com.amazon.fusion.CheckFailureProc.CheckFailure;


final class CheckExceptionProc
    extends Procedure2
{
    CheckExceptionProc()
    {
        //    "                                                                               |
        super("NOT FOR APPLICATION USE.",
              "tag", "thunk");
    }

    Class<? extends Exception> classFor(String tag)
    {
        switch (tag)
        {
            case "arg":      return ArgTypeFailure.class;
            case "arity":    return ArityFailure.class;
            case "check":    return CheckFailure.class;
            case "contract": return ContractException.class;
            case "result":   return ResultFailure.class;
            case "syntax":   return SyntaxException.class;
            default:         return null;
        }
    }

    @Override
    Object doApply(Evaluator eval, Object tag, Object thunk)
        throws FusionException
    {
        try
        {
            Procedure proc = (Procedure) thunk;
            eval.callNonTail(proc);
        }
        catch (Exception e)
        {
            Class<? extends Exception> expected =
                classFor(stringToJavaString(eval, tag));

            if (expected.isInstance(e))
            {
                // Successful check
                return voidValue(eval);
            }

            return makeString(eval, e.getClass().getName());
        }

        return makeString(eval, "");
    }
}
