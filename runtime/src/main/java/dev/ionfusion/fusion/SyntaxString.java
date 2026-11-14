// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionString.BaseString;

final class SyntaxString
    extends SimpleSyntaxValue<BaseString>
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


    static SyntaxString make(Evaluator        eval,
                             BaseString       content,
                             ResourcePosition pos,
                             Object[]         props)
    {
        return new SyntaxString(SyntaxWraps.EMPTY, pos, props, content);
    }


    @Override
    SyntaxString copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxString(wraps, getPosition(), properties, getContent());
    }
}
