// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionCollection.isCollection;
import static com.amazon.fusion.FusionCollection.unsafeCollectionAnnotationStrings;
import static com.amazon.fusion.FusionList.immutableList;
import static com.amazon.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import com.amazon.ion.IonValue;

final class IonAnnotationsProc
    extends Procedure1
{
    IonAnnotationsProc()
    {
        //    "                                                                               |
        super("Returns a non-null immutable list of strings containing the user type\n" +
              "annotations on the `value`.",
              "value");
    }

    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        String[] anns = EMPTY_STRING_ARRAY;

        if (isCollection(eval, arg))
        {
            anns = unsafeCollectionAnnotationStrings(eval, arg);
        }
        else
        {
            IonValue value = castToIonValueMaybe(arg);

            if (value != null)
            {
                anns = value.getTypeAnnotations();
            }
        }

        Object[] result = EMPTY_OBJECT_ARRAY;
        int length = anns.length;
        if (length != 0)
        {
            result = new Object[length];
            for (int i = 0; i < length; i++)
            {
                result[i] = eval.newString(anns[i]);
            }
        }

        // Returning immutable list allows us to return a shared structure
        // when possible, avoiding copies.
        return immutableList(eval, result);
    }
}
