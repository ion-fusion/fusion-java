// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionStruct.ImmutableStruct;
import dev.ionfusion.fusion.FusionStruct.StructFieldVisitor;
import dev.ionfusion.runtime.base.FusionException;

final class SyntaxStruct
    extends SyntaxValue
{
    private ImmutableStruct myStruct;


    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(ResourcePosition pos,
                         Object[] properties,
                         SyntaxWraps wraps,
                         ImmutableStruct struct)
    {
        super(wraps, pos, properties);
        myStruct = struct;
    }

    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(ResourcePosition pos, ImmutableStruct struct)
    {
        super(null, pos);
        myStruct = struct;
    }



    @Override
    SyntaxStruct copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxStruct(getPosition(), properties, wraps, myStruct);
    }


    static SyntaxStruct makeOriginal(Evaluator eval,
                                     ResourcePosition pos,
                                     ImmutableStruct struct)
    {
        return new SyntaxStruct(pos, ORIGINAL_STX_PROPS, null, struct);
    }


    /**
     * @param datum must be an immutable struct
     */
    static SyntaxStruct make(Evaluator eval, ResourcePosition pos, Object datum)
    {
        return new SyntaxStruct(pos, (ImmutableStruct) datum);
    }


    //========================================================================


    @Override
    synchronized void propagateLexicalContext(Evaluator eval, SyntaxWraps propagate)
        throws FusionException
    {
        StructFieldVisitor visitor =
            (name, value) -> ((SyntaxValue) value).addWraps(propagate);

        myStruct = myStruct.transformFields(eval, visitor);
    }


    @Override
    ImmutableStruct unwrap(Evaluator eval)
        throws FusionException
    {
        propagateLexicalContext(eval);
        return myStruct;
    }


    @Override
    Object syntaxToDatum(final Evaluator eval)
        throws FusionException
    {
        if (myStruct.size() == 0)
        {
            return myStruct;
        }

        StructFieldVisitor visitor =
            (name, value) -> ((SyntaxValue) value).syntaxToDatum(eval);

        return myStruct.transformFields(eval, visitor);
    }


    @Override
    SyntaxValue doExpand(final Expander expander, final Environment env)
        throws FusionException
    {
        var eval = expander.getEvaluator();
        var thisStruct = unwrap(eval);
        if (thisStruct.size() == 0)
        {
            return this;
        }

        StructFieldVisitor visitor =
            (name, value) -> expander.expandExpression(env, (SyntaxValue) value);

        ImmutableStruct s = thisStruct.transformFields(eval, visitor);

        // Wraps have been pushed down so the copy doesn't need them.
        return new SyntaxStruct(getPosition(), s);
    }
}
