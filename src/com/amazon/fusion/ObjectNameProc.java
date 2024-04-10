// Copyright (c) 2023 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * Implements {@code object_name}.
 *
 * @see NamedObject
 * @see <a href="http://tinyurl.com/object-name">Racket Reference for
 *        object-name</a>
 */
class ObjectNameProc
    extends Procedure1
{
    @Override
    Object doApply(Evaluator eval, Object arg)
        throws FusionException
    {
        if (arg instanceof NamedObject)
        {
            return ((NamedObject) arg).objectName(eval);
        }

        return FusionVoid.voidValue(eval);
    }
}
