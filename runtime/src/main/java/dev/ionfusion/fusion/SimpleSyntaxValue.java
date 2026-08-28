// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.runtime._private.util.Empties.EMPTY_OBJECT_ARRAY;

import com.amazon.ion.IonException;
import com.amazon.ion.IonWriter;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ResourcePosition;
import java.io.IOException;

/**
 * Implementation of most {@link SyntaxValue}s, which consist of a simple
 * wrapped datum.
 */
class SimpleSyntaxValue
    extends SyntaxValue
{
    final BaseValue myDatum;


    /**
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(ResourcePosition pos, Object[] properties, BaseValue datum)
    {
        super(pos, properties);
        assert ! (datum instanceof SyntaxValue);
        myDatum = datum;
    }

    /**
     * @param pos may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(ResourcePosition pos, BaseValue datum)
    {
        this(pos, EMPTY_OBJECT_ARRAY, datum);
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
    SyntaxValue copyReplacingProperties(Object[] properties)
    {
        return new SimpleSyntaxValue(getPosition(), properties, myDatum);
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

    @Override
    void ionize(Evaluator eval, IonWriter writer)
        throws IOException, IonException, FusionException
    {
        myDatum.ionize(eval, writer);
    }

    @Override
    final void write(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        myDatum.write(eval, out);
    }
}
