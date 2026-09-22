// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionText.BaseText;

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
}
