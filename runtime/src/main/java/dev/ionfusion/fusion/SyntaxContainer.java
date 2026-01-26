// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.runtime.base.SourceLocation;

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

    SyntaxContainer(SourceLocation loc, Object[] properties, SyntaxWraps wraps)
    {
        super(loc, properties);
        myWraps = wraps;
    }

    SyntaxContainer(SourceLocation loc)
    {
        super(loc, EMPTY_OBJECT_ARRAY);
        myWraps = null;
    }


    /**
     * Equivalent to size() == 0.  Computing size of sexp is
     * expensive so we do this instead.
     */
    abstract boolean hasNoChildren()
        throws FusionException;

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
        // Don't bother if this is an empty container.
        if (hasNoChildren()) return this;

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
        // Don't bother if this is an empty container.
        if (hasNoChildren()) return this;

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
