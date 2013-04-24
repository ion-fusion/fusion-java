// Copyright (c) 2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Syntax wrap thats an ordered, mutable sequence of other wraps.
 */
class SequenceWrap
    extends SyntaxWrap
{
    private SyntaxWrap[] myWraps;

    SequenceWrap(SyntaxWrap... wraps)
    {
        assert wraps.length != 0;
        myWraps = wraps;
    }


    void addWrap(SyntaxWrap wrap)
    {
        int suffixLen =  myWraps.length;
        int len = 1 + suffixLen;

        SyntaxWrap[] combined = new SyntaxWrap[len];
        combined[0] = wrap;
        System.arraycopy(myWraps, 0, combined, 1, suffixLen);

        myWraps = combined;
    }


    @Override
    Binding resolve(String name,
                    Iterator<SyntaxWrap> moreWraps,
                    Set<Integer> returnMarks)
    {
        throw new IllegalStateException();
    }


    @Override
    Iterator<SyntaxWrap> iterator()
    {
        return Arrays.asList(myWraps).iterator();
    }


    @Override
    public String toString()
    {
        return "{{{Sequence wrap: " + Arrays.toString(myWraps) + "}}}";
    }
}
