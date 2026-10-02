// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_STRING_ARRAY;
import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.isImmutableList;
import static dev.ionfusion.fusion.FusionList.nullList;
import static dev.ionfusion.fusion.FusionList.unsafeListElement;
import static java.lang.System.arraycopy;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionList.BaseList;
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


    @Override
    SyntaxValue copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxList(getPosition(), properties, wraps, getContent());
    }


    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    static SyntaxList makeOriginal(Evaluator eval, ResourcePosition pos, BaseList datum)
    {
        return new SyntaxList(pos, ORIGINAL_STX_PROPS, SyntaxWraps.EMPTY, datum);
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
    BaseList propagateLexicalContent(Evaluator eval,
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


    @Override
    final int size(Evaluator eval)
    {
        return getContent().size();
    }


    @Override
    SyntaxValue[] extract(Evaluator eval)
        throws FusionException
    {
        var thisList = unwrap(eval);
        if (thisList.isAnyNull()) return null;

        int len = thisList.size();
        SyntaxValue[] extracted = new SyntaxValue[len];
        thisList.unsafeCopy(eval, 0, extracted, 0, len);
        return extracted;
    }


    @Override
    SyntaxValue get(Evaluator eval, int index)
        throws FusionException
    {
        var thisList = unwrap(eval);
        return (SyntaxValue) thisList.elt(eval, index);
    }


    @Override
    SyntaxList makeAppended(Evaluator eval, SyntaxSequence<?> that)
        throws FusionException
    {
        int thisLength = this.size(eval);
        int thatLength = that.size(eval);
        int newLength  = thisLength + thatLength;

        if (newLength == 0) return this;

        var thisList = unwrap(eval);
        Object[] children = new Object[newLength];
        if (thisLength != 0)
        {
            thisList.unsafeCopy(eval, 0, children, 0, thisLength);
        }
        if (thatLength != 0)
        {
            // that could be a sexp so we cant copy directly
            // TODO avoid intermediate array copy
            SyntaxValue[] c = that.extract(eval);
            arraycopy(c, 0, children, thisLength, thatLength);
        }

        BaseSymbol[] anns = thisList.getAnnotations();
        BaseList list = immutableList(eval, anns, children);
        return new SyntaxList(eval, null, list);
    }


    @Override
    SyntaxList makeSubseq(Evaluator eval, int from)
        throws FusionException
    {
        var thisList = unwrap(eval);

        if ((thisList.size() == 0 || from == 0)
            && ! thisList.isAnnotated())
        {
            return this;
        }

        BaseList list;
        if (thisList.isAnyNull())
        {
            list = FusionList.NULL_LIST;
        }
        else
        {
            // TODO will crash if `from` is beyond the end of the list
            int len = thisList.size();
            Object[] children = new Object[len - from];
            thisList.unsafeCopy(eval, from, children, 0, children.length);
            list = immutableList(eval, EMPTY_STRING_ARRAY, children);
        }

        return new SyntaxList(eval, null, list);
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();
        int len = size(eval);
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
