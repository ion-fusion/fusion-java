// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;
import static dev.ionfusion.fusion.SyntaxException.makeSyntaxError;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.FusionException;

final class SyntaxKeyword
    extends SyntaxText<SyntaxKeyword>
{

    /**
     * @param datum must not be null.
     */
    private SyntaxKeyword(SyntaxWraps      wraps,
                          ResourcePosition pos,
                          Object[]         properties,
                          BaseSymbol       datum)
    {
        super(wraps, pos, properties, datum);
    }

    /**
     * @param datum must not be null.
     */
    private SyntaxKeyword(ResourcePosition pos,
                          Object[]         properties,
                          BaseSymbol       datum)
    {
        super(null, pos, properties, datum);
    }


    static SyntaxKeyword makeOriginal(Evaluator        eval,
                                      ResourcePosition pos,
                                      BaseSymbol       symbol)
    {
        return new SyntaxKeyword(pos, ORIGINAL_STX_PROPS, symbol);
    }


    static SyntaxKeyword make(Evaluator eval, ResourcePosition pos, BaseSymbol symbol)
    {
        return new SyntaxKeyword(pos, EMPTY_OBJECT_ARRAY, symbol);
    }


    //========================================================================


    @Override
    SyntaxKeyword copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxKeyword(wraps,
                                 getPosition(),
                                 properties,
                                 (BaseSymbol) myDatum);
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws SyntaxException
    {
        throw makeSyntaxError(expander.getEvaluator(), null, "Keywords are not expressions", this);
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        // TODO I have no idea if this is correct long-term.
        // Should we allow it at the moment?
        return super.unwrap(eval);
    }
}
