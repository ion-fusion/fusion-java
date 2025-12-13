// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

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
            Object o = ((NamedObject) arg).objectName();
            if (o != null)
            {
                return o;
            }
        }

        return FusionVoid.voidValue(eval);
    }
}
