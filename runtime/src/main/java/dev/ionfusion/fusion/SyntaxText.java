// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionText.BaseText;

abstract class SyntaxText<Sub extends SyntaxText>
    extends SimpleSyntaxValue
{
    final SyntaxWraps myWraps;   // TODO make non-null to streamline logic.

    /**
     * @param wraps may be null.
     * @param loc may be null.
     * @param properties must not be null.
     * @param datum must not be null.
     */
    SyntaxText(SyntaxWraps    wraps,
               SourceLocation loc,
               Object[]       properties,
               BaseText       datum)
    {
        super(loc, properties, datum);
        myWraps = wraps;
    }


    final String stringValue()
    {
        return ((BaseText) myDatum).stringValue();
    }


    @Override
    boolean hasMarks(Evaluator eval)
    {
        return (myWraps != null && myWraps.hasMarks(eval));
    }


    abstract Sub copyReplacingWraps(SyntaxWraps wraps);

    @Override
    abstract Sub copyReplacingProperties(Object[] properties);


    @Override
    final Sub addWrap(SyntaxWrap wrap)
    {
        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = SyntaxWraps.make(wrap);
        }
        else
        {
            newWraps = myWraps.addWrap(wrap);
        }
        return copyReplacingWraps(newWraps);
    }

    @Override
    final Sub addWraps(SyntaxWraps wraps)
    {
        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = wraps;
        }
        else
        {
            newWraps = myWraps.addWraps(wraps);
        }
        return copyReplacingWraps(newWraps);
    }


    @Override
    final Sub stripWraps(Evaluator eval)
    {
        if (myWraps == null) return (Sub) this;
        return copyReplacingWraps(null);
    }


    /**
     * Adds the wraps on this value onto those already on another value.
     * @return syntax matching the source, after adding the wraps from this
     * symbol.
     */
    final SyntaxValue copyWrapsTo(SyntaxValue source)
        throws FusionException
    {
        if (myWraps == null) return source;
        return source.addWraps(myWraps);
    }
}
