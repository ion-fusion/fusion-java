// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;

abstract class SyntaxContainer
    extends SyntaxValue
{
    /**
     * The sequence of wraps around this value.
     * Semantically, wraps only affect symbols and they should act as if they
     * are always pushed down immediately.  However, we cache them at
     * containers as an optimization. Any wraps are just being held here
     * lazily, waiting to be pushed down to all children once one is requested.
     */
    SyntaxWraps myWraps;

    SyntaxContainer(ResourcePosition pos, Object[] properties, SyntaxWraps wraps)
    {
        super(pos, properties);
        myWraps = wraps;
    }

    SyntaxContainer(ResourcePosition pos)
    {
        super(pos, EMPTY_OBJECT_ARRAY);
        myWraps = null;
    }


    /**
     * Only called when this container has children.
     * @param wraps is not null.
     */
    abstract SyntaxValue copyReplacingWraps(SyntaxWraps wraps)
        throws FusionException;


    @Override
    final SyntaxValue addWrap(SyntaxWrap wrap)
        throws FusionException
    {
        assert wrap != null;

        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = SyntaxWraps.make(wrap);
        }
        else
        {
            newWraps = myWraps.addWrap(wrap);
        }
        return copyReplacingWraps(newWraps);
    }

    /**
     * Prepends a sequence of wraps onto our existing wraps.
     */
    @Override
    final SyntaxValue addWraps(SyntaxWraps wraps)
        throws FusionException
    {
        assert wraps != null;

        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = wraps;
        }
        else
        {
            newWraps = myWraps.addWraps(wraps);
        }
        return copyReplacingWraps(newWraps);
    }


    @Override
    boolean hasMarks(Evaluator eval)
    {
        return (myWraps == null ? false : myWraps.hasMarks(eval));
    }
}
