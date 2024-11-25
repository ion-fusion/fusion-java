// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import com.amazon.fusion.FusionString.BaseString;

final class SyntaxString
    extends SyntaxText<SyntaxString>
{
    /**
     * @param datum must not be null.
     */
    private SyntaxString(SyntaxWraps    wraps,
                         SourceLocation loc,
                         Object[]       properties,
                         BaseString     datum)
    {
        super(wraps, loc, properties, datum);
    }

    static SyntaxString makeOriginal(Evaluator      eval,
                                     SourceLocation loc,
                                     BaseString     datum)
    {
        return new SyntaxString(null, loc, ORIGINAL_STX_PROPS, datum);
    }

    static SyntaxString make(Evaluator      eval,
                             SourceLocation loc,
                             BaseString     datum)
    {
        return new SyntaxString(null, loc, EMPTY_OBJECT_ARRAY, datum);
    }


    @Override
    SyntaxString copyReplacingWraps(SyntaxWraps wraps)
    {
        return new SyntaxString(wraps,
                                getLocation(),
                                getProperties(),
                                (BaseString) myDatum);
    }

    @Override
    SyntaxString copyReplacingProperties(Object[] properties)
    {
        return new SyntaxString(myWraps,
                                getLocation(),
                                properties,
                                (BaseString) myDatum);
    }
}
