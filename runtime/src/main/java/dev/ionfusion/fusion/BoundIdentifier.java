// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.util.Set;

/**
 * The minimal data needed to track a bound identifier.
 */
final class BoundIdentifier
{
    private final Binding       myBinding;
    private final Set<MarkWrap> myMarks;

    // TODO Compress the mark-sets, there's going to be lots of
    //  empty and small sets.

    BoundIdentifier(Binding binding, Set<MarkWrap> marks)
    {
        assert binding != null;
        assert marks != null;

        myBinding = binding;
        myMarks   = marks;
    }


    Binding getBinding()
    {
        return myBinding;
    }

    BoundIdentifier copyReplacingBinding(Binding b)
    {
        return new BoundIdentifier(b, myMarks);
    }


    public boolean hasMarks()
    {
        return ! myMarks.isEmpty();
    }


    /**
     * Semantics are of {@code bound-identifier=?}, so two instances can match
     * even when {@link #getBinding()} returns different objects.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        BoundIdentifier other = (BoundIdentifier) obj;
        return myBinding.sameTarget(other.myBinding)
            && myMarks.equals(other.myMarks);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result =
            prime * result
                + myBinding.target().hashCode();
        result =
            prime * result
                + myMarks.hashCode();
        return result;
    }
}
