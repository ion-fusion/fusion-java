// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionStruct.ImmutableStruct;
import dev.ionfusion.fusion.FusionStruct.StructFieldVisitor;
import dev.ionfusion.runtime.base.FusionException;

final class SyntaxStruct
    extends SyntaxValue<ImmutableStruct>
{
    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(ResourcePosition pos,
                         Object[] properties,
                         SyntaxWraps wraps,
                         ImmutableStruct struct)
    {
        super(struct, wraps, pos, properties);
    }

    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(ResourcePosition pos, ImmutableStruct struct)
    {
        super(struct, null, pos);
    }



    @Override
    SyntaxStruct copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxStruct(getPosition(), properties, wraps, getContent());
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
    static SyntaxStruct make(Evaluator eval, ResourcePosition pos, ImmutableStruct datum)
    {
        return new SyntaxStruct(pos, datum);
    }


    //========================================================================


    @Override
    synchronized ImmutableStruct propagateLexicalContent(Evaluator eval,
                                                         ImmutableStruct content,
                                                         SyntaxWraps propagate)
        throws FusionException
    {
        StructFieldVisitor visitor =
            (name, value) -> ((SyntaxValue) value).addWraps(propagate);

        return content.transformFields(eval, visitor);
    }


    @Override
    Object syntaxToDatum(final Evaluator eval)
        throws FusionException
    {
        // No need to propagate, we're discarding the context.
        var thisStruct = getContent();
        if (thisStruct.size() == 0)
        {
            return thisStruct;
        }

        StructFieldVisitor visitor =
            (name, value) -> ((SyntaxValue) value).syntaxToDatum(eval);

        return thisStruct.transformFields(eval, visitor);
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
