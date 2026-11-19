// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.isImmutableList;
import static dev.ionfusion.fusion.FusionList.nullList;
import static dev.ionfusion.fusion.FusionList.unsafeListElement;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionList.ImmutableList;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.FusionException;

final class SyntaxList
    extends SyntaxSequence<BaseList>
{
    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    private SyntaxList(ResourcePosition pos,
                       Object[]         properties,
                       SyntaxWraps      wraps,
                       BaseList         datum)
    {
        super(datum, pos, properties, wraps);
    }

    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    private SyntaxList(Evaluator eval,
                       ResourcePosition pos,
                       BaseList datum)
    {
        super(datum, pos);
        assert isImmutableList(eval, datum);
    }


    /**
     * @param content must contain {@link SyntaxValue}s.
     * @param props must not be null.
     */
    static SyntaxList make(Evaluator        eval,
                           ImmutableList    content,
                           ResourcePosition pos,
                           Object[]         props)
    {
        return new SyntaxList(pos, props, SyntaxWraps.EMPTY, content);
    }


    @Override
    SyntaxList copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxList(getPosition(), properties, wraps, getContent());
    }


    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    static SyntaxList make(Evaluator eval, ResourcePosition pos, Object datum)
    {
        return new SyntaxList(eval, pos, (BaseList) datum);
    }


    @Override
    SyntaxList copyReplacingChildren(Evaluator      eval,
                                     SyntaxValue... children)
        throws FusionException
    {
        BaseSymbol[] annotations = getContent().getAnnotations();
        BaseList datum = (children == null
                              ? nullList(eval, annotations)
                              : immutableList(eval, annotations, children));
        return new SyntaxList(getPosition(), getProperties(), getWraps(), datum);
    }


    @Override
    BaseList propagateLexicalContext(Evaluator eval,
                                     BaseList content,
                                     SyntaxWraps propagate)
        throws FusionException
    {
        int len = content.size();
        if (len > 0)
        {
            boolean changed = false;
            SyntaxValue[] children = new SyntaxValue[len];
            for (int i = 0; i < len; i++)
            {
                SyntaxValue child = (SyntaxValue) content.elt(eval, i);
                SyntaxValue wrapped = child.addWraps(propagate);
                children[i] = wrapped;
                changed |= wrapped != child;
            }

            if (changed) // Keep sharing when we can
            {
                BaseSymbol[] annotations = content.getAnnotations();
                return immutableList(eval, annotations, children);
            }
        }
        return content;
    }


    SyntaxValue get(Evaluator eval, int index)
        throws FusionException
    {
        return (SyntaxValue) unsafeListElement(eval, unwrap(eval), index);
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();
        int len = getContent().size(eval);
        if (len == 0) return this;

        Object list = unwrap(eval);

        boolean same = true;
        SyntaxValue[] children = new SyntaxValue[len];
        for (int i = 0; i < len; i++)
        {
            SyntaxValue subform = (SyntaxValue) unsafeListElement(eval, list, i);
            SyntaxValue expanded = expander.expandExpression(env, subform);
            same &= (subform == expanded);
            children[i] = expanded;
        }

        if (same) return this;

        return this.copyReplacingChildren(eval, children);
    }


    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        // No need to propagate, we're discarding the context.
        var thisList = getContent();
        int size = thisList.size();
        if (size == 0)
        {
            return thisList;
        }

        Object[] children = new Object[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue child = (SyntaxValue) thisList.elt(eval, i);
            children[i] = child.syntaxToDatum(eval);
        }

        BaseSymbol[] annotations = thisList.getAnnotations();
        return immutableList(eval, annotations, children);
    }
}
