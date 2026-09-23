// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;

/**
 * Implementation of most {@link SyntaxValue}s, which consist of a simple
 * wrapped datum.
 */
class SimpleSyntaxValue
    extends SyntaxValue
{
    final BaseValue myDatum;


    /**
     * @param wraps can be null.
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(SyntaxWraps wraps,
                      ResourcePosition pos,
                      Object[] properties,
                      BaseValue datum)
    {
        super(wraps, pos, properties);
        assert !(datum instanceof SyntaxValue);
        myDatum = datum;
    }

    /**
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(ResourcePosition pos, Object[] properties, BaseValue datum)
    {
        this(null, pos, properties, datum);
    }

    /**
     * @param pos may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(ResourcePosition pos, BaseValue datum)
    {
        this(null, pos, EMPTY_OBJECT_ARRAY, datum);
    }


    @Override
    SimpleSyntaxValue copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SimpleSyntaxValue(wraps, getPosition(), properties, myDatum);
    }


    /**
     * @param pos may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    static SyntaxValue makeOriginalSyntax(Evaluator      eval,
                                          ResourcePosition pos,
                                          BaseValue      datum)
    {
        return new SimpleSyntaxValue(pos, ORIGINAL_STX_PROPS, datum);
    }

    /**
     * @param pos may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    static SyntaxValue makeSyntax(Evaluator      eval,
                                  ResourcePosition pos,
                                  BaseValue      datum)
    {
        return new SimpleSyntaxValue(pos, datum);
    }

    /**
     * @param pos may be null.
     * @param datum must be a Fusion value but not a {@link SyntaxValue}.
     */
    static SyntaxValue makeSyntax(Evaluator      eval,
                                  ResourcePosition pos,
                                  Object         datum)
    {
        return new SimpleSyntaxValue(pos, (BaseValue) datum);
    }


    //========================================================================


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        return myDatum;
    }

    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        return myDatum;
    }
}
