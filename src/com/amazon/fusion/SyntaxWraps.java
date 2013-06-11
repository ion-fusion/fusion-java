// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class SyntaxWraps
{
    private final SyntaxWrap[] myWraps;

    static SyntaxWraps make(SyntaxWrap initialWrap)
    {
        return new SyntaxWraps(initialWrap);
    }

    static SyntaxWraps make(SyntaxWrap... initialWraps)
    {
        return new SyntaxWraps(initialWraps);
    }

    private SyntaxWraps(SyntaxWrap initialWrap)
    {
        myWraps = new SyntaxWrap[] { initialWrap };
    }

    private SyntaxWraps(SyntaxWrap[] wraps)
    {
        myWraps = wraps;
    }


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
        int len = prefixLen + suffixLen;

        SyntaxWrap[] combined = new SyntaxWrap[len];
        System.arraycopy(wraps.myWraps, 0, combined, 0, prefixLen);
        System.arraycopy(myWraps, 0, combined, prefixLen, suffixLen);

        return new SyntaxWraps(combined);
    }


    /**
     * @return not null.
     */
    public Set<Integer> computeMarks()
    {
        Set<Integer> marks = null;

        for (SyntaxWrap wrap : myWraps)
        {
            if (wrap instanceof MarkWrap)
            {
                int mark = ((MarkWrap)wrap).getMark();

                if (marks == null)
                {
                    marks = new HashSet<Integer>();
                    marks.add(mark);
                }
                else if (! marks.remove(mark))
                {
                    marks.add(mark);
                }
            }
        }

        if (marks == null) marks = Collections.emptySet();
        return marks;
    }


    /**
     * @return null is equivalent to a {@link FreeBinding}.
     */
    Binding resolve(String name)
    {
        Iterator<SyntaxWrap> i =
            new SplicingIterator(Arrays.asList(myWraps).iterator());
        Set<Integer> marks = new HashSet<Integer>();

        if (i.hasNext())
        {
            SyntaxWrap wrap = i.next();
            return wrap.resolve(name, i, marks);
        }

        return null;
    }

    /**
     * @return null is equivalent to a {@link FreeBinding}.
     */
    Binding resolveTop(String name)
    {
        Iterator<SyntaxWrap> i =
            new SplicingIterator(Arrays.asList(myWraps).iterator());
        Set<Integer> marks = new HashSet<Integer>();

        if (i.hasNext())
        {
            SyntaxWrap wrap = i.next();
            return wrap.resolveTop(name, i, marks);
        }

        return null;
    }

    private static final class SplicingIterator
        implements Iterator<SyntaxWrap>
    {
        private final Iterator<SyntaxWrap> myOuterWraps;
        private Iterator<SyntaxWrap> mySplicedWraps;

        private SplicingIterator(Iterator<SyntaxWrap> outerWraps)
        {
            myOuterWraps = outerWraps;
        }

        @Override
        public boolean hasNext()
        {
            if (mySplicedWraps != null)
            {
                boolean hasNext = mySplicedWraps.hasNext();
                if (hasNext) return true;
                mySplicedWraps = null;
            }

            // This assumes the final outer-wrap isn't an empty composite!
            // See below for more.
            return myOuterWraps.hasNext();
        }

        @Override
        public SyntaxWrap next()
        {
            SyntaxWrap next;
            if (mySplicedWraps == null)
            {
                next = myOuterWraps.next();
                Iterator<SyntaxWrap> spliced = next.iterator();
                if (spliced == null)
                {
                    return next;
                }

                // If this isn't true, then hasNext above is wrong!
                assert spliced.hasNext();
                mySplicedWraps = spliced;
            }

            next = mySplicedWraps.next();
            assert next.iterator() == null;

            return next;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
