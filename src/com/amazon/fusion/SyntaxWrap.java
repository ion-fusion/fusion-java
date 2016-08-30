// Copyright (c) 2012-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.util.Iterator;
import java.util.Set;

/**
 * A unit of lexical information wrapped around a {@link SyntaxValue}.
 */
abstract class SyntaxWrap
{
    /**
     * @param returnMarks <em>returns</em> the marks from this wrap and those
     * deeper. Must be mutable and not null.
     *
     * @return null indicates a free variable.
     */
    abstract Binding resolve(BaseSymbol name,
                             Iterator<SyntaxWrap> moreWraps,
                             Set<MarkWrap> returnMarks);


    Binding resolveTop(BaseSymbol name,
                       Iterator<SyntaxWrap> moreWraps,
                       Set<MarkWrap> returnMarks)
    {
        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            return nextWrap.resolveTop(name, moreWraps, returnMarks);
        }
        return null;
    }
}
