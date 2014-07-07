// Copyright (c) 2013-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import com.amazon.ion.IonException;
import com.amazon.ion.IonWriter;
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
     * @param loc may be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(SourceLocation loc, Object[] properties, BaseValue datum)
    {
        super(loc, properties);
        assert ! (datum instanceof SyntaxValue);
        myDatum = datum;
    }

    /**
     * @param loc may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(SourceLocation loc, BaseValue datum)
    {
        this(loc, EMPTY_OBJECT_ARRAY, datum);
    }


    /**
     * @param loc may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    static SyntaxValue makeOriginalSyntax(Evaluator      eval,
                                          SourceLocation loc,
                                          BaseValue      datum)
    {
        return new SimpleSyntaxValue(loc, ORIGINAL_STX_PROPS, datum);
    }

    /**
     * @param loc may be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    static SyntaxValue makeSyntax(Evaluator      eval,
                                  SourceLocation loc,
                                  BaseValue      datum)
    {
        return new SimpleSyntaxValue(loc, datum);
    }

    /**
     * @param loc may be null.
     * @param datum must be a Fusion value but not a {@link SyntaxValue}.
     */
    static SyntaxValue makeSyntax(Evaluator      eval,
                                  SourceLocation loc,
                                  Object         datum)
    {
        return new SimpleSyntaxValue(loc, (BaseValue) datum);
    }


    @Override
    String[] annotationsAsJavaStrings()
    {
        return myDatum.annotationsAsJavaStrings();
    }

    @Override
    SyntaxValue copyReplacingProperties(Object[] properties)
    {
        return new SimpleSyntaxValue(getLocation(), properties, myDatum);
    }

    @Override
    CompiledForm doCompile(Evaluator eval, Environment env)
        throws FusionException
    {
        return new CompiledConstant(myDatum);
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
    boolean isAnyNull()
    {
        return myDatum.isAnyNull();
    }

    @Override
    void ionize(Evaluator eval, IonWriter writer)
        throws IOException, IonException, FusionException, IonizeFailure
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
