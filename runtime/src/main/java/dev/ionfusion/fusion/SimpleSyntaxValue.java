// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionCollection.BaseCollection;
import dev.ionfusion.runtime.base.FusionException;

/**
 * Implementation of {@link SyntaxValue}s for content that does not contain
 * other syntax objects.
 */
class SimpleSyntaxValue
    extends SyntaxValue<Object>
{
    /**
     * @param wraps can be null.
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    SimpleSyntaxValue(SyntaxWraps wraps,
                      ResourcePosition pos,
                      Object[] properties,
                      Object datum)
    {
        super(datum, wraps, pos, properties);
        assert !(datum instanceof SyntaxValue);
        assert !(datum instanceof BaseCollection);
        // TODO we could contain empty or null collections
    }

    /**
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null and must not be a {@link SyntaxValue}.
     */
    private SimpleSyntaxValue(ResourcePosition pos, Object[] properties, Object datum)
    {
        this(SyntaxWraps.EMPTY, pos, properties, datum);
    }


    @Override
    SimpleSyntaxValue copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SimpleSyntaxValue(wraps, getPosition(), properties, getContent());
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
     * @param pos can be null.
     * @param datum must not be a {@link SyntaxValue}.
     */
    static SyntaxValue makeSyntax(Evaluator      eval,
                                  ResourcePosition pos,
                                  Object         datum)
    {
        return new SimpleSyntaxValue(pos, EMPTY_OBJECT_ARRAY, datum);
    }


    //========================================================================

    @Override
    void propagateLexicalContext(Evaluator eval)
    {
        // We have no children, so we don't need to propagate.
        // More importantly, this override exists to prevent the super-method from
        // clearing any context we have.
    }

    final Object propagateLexicalContent(Evaluator eval,
                                         Object content,
                                         SyntaxWraps propagate)
    {
        throw new IllegalStateException();
    }

    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        return getContent();
    }
}
