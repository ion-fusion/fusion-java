// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionSequence.BaseSequence;
import dev.ionfusion.runtime.base.FusionException;

abstract class SyntaxSequence<Content extends BaseSequence>
    extends SyntaxValue<Content>
{
    SyntaxSequence(Content content, ResourcePosition pos, Object[] properties, SyntaxWraps wraps)
    {
        super(content, wraps, pos, properties);
    }

    SyntaxSequence(Content content, ResourcePosition pos)
    {
        super(content, pos);
    }


    abstract int size(Evaluator eval)
        throws FusionException;


    abstract SyntaxValue get(Evaluator eval, int index)
        throws FusionException;


    /**
     * Creates a new syntax sequence, using our location and properties but
     * the given children.
     */
    abstract SyntaxSequence<Content> copyReplacingChildren(Evaluator eval,
                                                           SyntaxValue... children)
        throws FusionException;
}
