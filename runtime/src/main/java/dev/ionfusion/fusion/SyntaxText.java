// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionText.BaseText;
import dev.ionfusion.runtime.base.FusionException;

abstract class SyntaxText<Sub extends SyntaxText>
    extends SimpleSyntaxValue
{
    /**
     * @param wraps can be null.
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null.
     */
    SyntaxText(SyntaxWraps    wraps,
               ResourcePosition pos,
               Object[]       properties,
               BaseText       datum)
    {
        super(wraps, pos, properties, datum);
    }


    final String stringValue()
    {
        return ((BaseText) getContent()).stringValue();
    }


    /**
     * Adds the wraps on this value onto those already on another value.
     * @return syntax matching the source after adding the wraps from this
     * symbol.
     */
    final SyntaxValue copyWrapsTo(SyntaxValue source)
        throws FusionException
    {
        if (myWraps == null) return source;
        return source.addWraps(myWraps);
    }
}
