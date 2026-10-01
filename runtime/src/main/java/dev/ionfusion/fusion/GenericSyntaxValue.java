// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;

abstract class GenericSyntaxValue<Content>
    extends SyntaxValue
{
    /**
     * Can be mutated by {@link #propagateLexicalContext}
     */
    private Content myContent;


    GenericSyntaxValue(Content content,
                       SyntaxWraps wraps,
                       ResourcePosition pos,
                       Object[] properties)
    {
        super(wraps, pos, properties);
        assert content != null;
        myContent = content;
    }

    GenericSyntaxValue(Content content,
                       SyntaxWraps wraps,
                       ResourcePosition pos)
    {
        super(wraps, pos);
        assert content != null;
        myContent = content;
    }

    @Override
    final synchronized void propagateLexicalContext(Evaluator eval,
                                                    SyntaxWraps propagate)
        throws FusionException
    {
        myContent = propagateLexicalContent(eval, myContent, propagate);
    }

    abstract Content propagateLexicalContent(Evaluator eval,
                                             Content content,
                                             SyntaxWraps propagate)
        throws FusionException;


    /**
     * Returns our content like {@link #unwrap}, but does not propagate context.
     */
    final Content getContent()
    {
        return myContent;
    }


    @Override
    Content unwrap(Evaluator eval)
        throws FusionException
    {
        propagateLexicalContext(eval);
        return getContent();
    }
}
