// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_OBJECT_ARRAY;

import com.amazon.ion.IonException;
import com.amazon.ion.IonWriter;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;

final class SyntaxKeyword
    extends SyntaxText<SyntaxKeyword>
{

    /**
     * @param datum must not be null.
     */
    private SyntaxKeyword(SyntaxWraps    wraps,
                          SourceLocation loc,
                          Object[]       properties,
                          BaseSymbol     datum)
    {
        super(wraps, loc, properties, datum);
    }

    /**
     * @param datum must not be null.
     */
    private SyntaxKeyword(SourceLocation loc,
                          Object[]       properties,
                          BaseSymbol     datum)
    {
        super(null, loc, properties, datum);
    }


    static SyntaxKeyword makeOriginal(Evaluator      eval,
                                      SourceLocation loc,
                                      BaseSymbol     symbol)
    {
        return new SyntaxKeyword(loc, ORIGINAL_STX_PROPS, symbol);
    }


    static SyntaxKeyword make(Evaluator      eval,
                              SourceLocation loc,
                              BaseSymbol     symbol)
    {
        return new SyntaxKeyword(loc, EMPTY_OBJECT_ARRAY, symbol);
    }


    //========================================================================


    @Override
    SyntaxKeyword copyReplacingWraps(SyntaxWraps wraps)
    {
        return new SyntaxKeyword(wraps,
                                 getLocation(),
                                 getProperties(),
                                 (BaseSymbol) myDatum);
    }

    @Override
    SyntaxKeyword copyReplacingProperties(Object[] properties)
    {
        return new SyntaxKeyword(myWraps,
                                 getLocation(),
                                 properties,
                                 (BaseSymbol) myDatum);
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    SyntaxValue doExpand(Expander eval, Environment env)
        throws SyntaxException
    {
        throw new SyntaxException(null, "Keywords are not expressions", this);
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        // TODO I have no idea if this is correct long-term.
        // Should we allow it at the moment?
        return super.unwrap(eval);
    }


    @Override
    void ionize(Evaluator eval, IonWriter writer)
        throws IOException, IonException, FusionException
    {
        // TODO __ ??
        super.ionize(eval, writer);
    }
}
