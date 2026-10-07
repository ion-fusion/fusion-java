// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionString.BaseString;

final class SyntaxString
    extends SyntaxText<BaseString>
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
        return new SyntaxString(SyntaxWraps.EMPTY, pos, ORIGINAL_STX_PROPS, datum);
    }

    static SyntaxString make(Evaluator        eval,
                             ResourcePosition pos,
                             BaseString       datum)
    {
        return new SyntaxString(SyntaxWraps.EMPTY, pos, EMPTY_OBJECT_ARRAY, datum);
    }


    @Override
    SyntaxString copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxString(wraps, getPosition(), properties, getContent());
    }
}
