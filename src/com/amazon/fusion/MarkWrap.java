// Copyright (c) 2012-2015 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Syntax wrap holding marks applied by the macro expander.
 */
class MarkWrap
    extends SyntaxWrap
{
    private static final AtomicInteger ourMarkCounter = new AtomicInteger();

    private final int myMark;

    MarkWrap()
    {
        myMark = ourMarkCounter.incrementAndGet();
    }

    int getMark()
    {
        return myMark;
    }


    @Override
    Binding resolve(BaseSymbol name,
                    Iterator<SyntaxWrap> moreWraps,
                    Set<MarkWrap> returnMarks)
    {
        Binding b = null;
        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            b = nextWrap.resolve(name, moreWraps, returnMarks);
        }

        if (! returnMarks.add(this))
        {
            returnMarks.remove(this);
        }

        return b;
    }


    @Override
    public String toString()
    {
        return "{{{Mark " + myMark + "}}}";
    }
}
