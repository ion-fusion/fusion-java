// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
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
    abstract Binding resolveMaybe(BaseSymbol name,
                                  Iterator<SyntaxWrap> moreWraps,
                                  Set<MarkWrap> returnMarks);


    Binding resolveTopMaybe(BaseSymbol name,
                            Iterator<SyntaxWrap> moreWraps,
                            Set<MarkWrap> returnMarks)
    {
        if (moreWraps.hasNext())
        {
            SyntaxWrap nextWrap = moreWraps.next();
            return nextWrap.resolveTopMaybe(name, moreWraps, returnMarks);
        }
        return null;
    }
}
