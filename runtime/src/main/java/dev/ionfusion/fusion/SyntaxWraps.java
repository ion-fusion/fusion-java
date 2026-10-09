// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Records the lexical information associated with a {@link SyntaxValue} during
 * expansion and compilation.  An instance may be associated with more than one
 * syntax object, so the underlying object is not referenced here.
 */
class SyntaxWraps
{
    final static SyntaxWraps EMPTY = new SyntaxWraps.Empty();


    /** Not null. */
    private final SyntaxWrap[] myWraps;

    private SyntaxWraps(SyntaxWrap initialWrap)
    {
        myWraps = new SyntaxWrap[] { initialWrap };
    }

    private SyntaxWraps(SyntaxWrap[] wraps)
    {
        myWraps = wraps;
    }


    //==================================================================================


    SyntaxWraps addWrap(SyntaxWrap wrap)
    {
        int suffixLen =  myWraps.length;
        int len = 1 + suffixLen;

        SyntaxWrap[] combined = new SyntaxWrap[len];
        combined[0] = wrap;
        System.arraycopy(myWraps, 0, combined, 1, suffixLen);

        return new SyntaxWraps(combined);
    }


    /**
     * Prepends a sequence of wraps onto our existing ones.
     * It is assumed that the given list will not be modified later and can
     * therefore be shared.
     */
    SyntaxWraps addWraps(SyntaxWraps wraps)
    {
        // TODO this should use a linked-list to avoid copies
        int prefixLen = wraps.myWraps.length;
        int suffixLen =  this.myWraps.length;

        if (suffixLen == 0) return this; // Nothing to add

        int len = prefixLen + suffixLen;

        SyntaxWrap[] combined = new SyntaxWrap[len];
        System.arraycopy(wraps.myWraps, 0, combined, 0, prefixLen);
        System.arraycopy(myWraps, 0, combined, prefixLen, suffixLen);

        return new SyntaxWraps(combined);
    }


    /**
     * @return not null.
     */
    public Set<MarkWrap> computeMarks()
    {
        Set<MarkWrap> marks = null;

        for (SyntaxWrap wrap : myWraps)
        {
            if (wrap instanceof MarkWrap)
            {
                MarkWrap mark = (MarkWrap) wrap;

                if (marks == null)
                {
                    marks = new HashSet<>();
                    marks.add(mark);
                }
                else if (! marks.add(mark))
                {
                    marks.remove(mark);
                }
            }
        }

        if (marks == null) marks = Collections.emptySet();
        return marks;
    }


    final boolean hasMarks(Evaluator eval)
    {
        // We have to walk all wraps to match up cancelling pairs of marks.
        return ! computeMarks().isEmpty();
    }


    /**
     * Resolves to a top-level binding if one exists.
     *
     * @return null is equivalent to a {@link FreeBinding}, and either may be
     * returned.
     */
    Binding resolveTopMaybe(BaseSymbol name)
    {
        assert myWraps.length > 0;

        Iterator<SyntaxWrap> i = Arrays.asList(myWraps).iterator();

        SyntaxWrap wrap = i.next();
        Set<MarkWrap> marks = new HashSet<>();
        return wrap.resolveTopMaybe(name, i, marks);
    }

    /**
     * @return not null.
     */
    BoundIdentifier resolveBoundIdentifier(BaseSymbol name)
    {
        assert myWraps.length > 0;

        Set<MarkWrap> marks = new HashSet<>();

        Iterator<SyntaxWrap> i = Arrays.asList(myWraps).iterator();
        SyntaxWrap wrap = i.next();

        Binding binding = wrap.resolveMaybe(name, i, marks);
        if (binding == null)
        {
            binding = new FreeBinding(name);
        }

        return new BoundIdentifier(binding, marks);
    }


    //==================================================================================


    private static final class Empty
        extends SyntaxWraps
    {
        private Empty()
        {
            super(new SyntaxWrap[0]);
        }

        @Override
        SyntaxWraps addWrap(SyntaxWrap wrap)
        {
            return new SyntaxWraps(wrap);
        }

        @Override
        SyntaxWraps addWraps(SyntaxWraps wraps)
        {
            return wraps;
        }

        @Override
        public Set<MarkWrap> computeMarks()
        {
            return Collections.emptySet();
        }

        @Override
        Binding resolveTopMaybe(BaseSymbol name)
        {
            return null;
        }

        @Override
        BoundIdentifier resolveBoundIdentifier(BaseSymbol name)
        {
            return new BoundIdentifier(new FreeBinding(name), Collections.emptySet());
        }
    }
}
