// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.runtime._private.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.fusion.FusionString.BaseString;
import dev.ionfusion.runtime.base.ResourcePosition;

final class SyntaxString
    extends SyntaxText<SyntaxString>
{
    /**
     * @param datum must not be null.
     */
    private SyntaxString(SyntaxWraps      wraps,
                         ResourcePosition pos,
                         Object[]         properties,
                         BaseString       datum)
    {
        super(wraps, pos, properties, datum);
    }

    static SyntaxString makeOriginal(Evaluator        eval,
                                     ResourcePosition pos,
                                     BaseString       datum)
    {
        return new SyntaxString(null, pos, ORIGINAL_STX_PROPS, datum);
    }

    static SyntaxString make(Evaluator        eval,
                             ResourcePosition pos,
                             BaseString       datum)
    {
        return new SyntaxString(null, pos, EMPTY_OBJECT_ARRAY, datum);
    }


    @Override
    SyntaxString copyReplacingWraps(SyntaxWraps wraps)
    {
        return new SyntaxString(wraps,
                                getPosition(),
                                getProperties(),
                                (BaseString) myDatum);
    }

    @Override
    SyntaxString copyReplacingProperties(Object[] properties)
    {
        return new SyntaxString(myWraps,
                                getPosition(),
                                properties,
                                (BaseString) myDatum);
    }
}
