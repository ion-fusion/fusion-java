// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.isImmutableList;
import static dev.ionfusion.fusion.FusionList.nullList;
import static dev.ionfusion.fusion.FusionList.unsafeListElement;
import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_STRING_ARRAY;
import static java.lang.System.arraycopy;

import com.amazon.ion.IonWriter;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;

final class SyntaxList
    extends SyntaxSequence
{
    /**
     * Both the list and its elements may be shared with other instances.
     * When we push down wraps, we copy the list and the children as needed.
     * We push lazily to aggregate as many wraps here and only push once.
     * That avoids repeated cloning of the children.
     */
    private BaseList myImmutableList;


    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    private SyntaxList(SourceLocation loc,
                       Object[]       properties,
                       SyntaxWraps    wraps,
                       BaseList       datum)
    {
        super(loc, properties, wraps);
        myImmutableList = datum;
    }

    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    private SyntaxList(Evaluator eval,
                       SourceLocation loc,
                       BaseList datum)
    {
        super(loc);
        assert isImmutableList(eval, datum);
        myImmutableList = datum;
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    static SyntaxList makeOriginal(Evaluator      eval,
                                   SourceLocation loc,
                                   BaseList       datum)
    {
        return new SyntaxList(loc, ORIGINAL_STX_PROPS, null, datum);
    }

    /**
     * @param datum an immutable list of {@link SyntaxValue}s.
     */
    static SyntaxList make(Evaluator      eval,
                           SourceLocation loc,
                           Object         datum)
    {
        return new SyntaxList(eval, loc, (BaseList) datum);
    }


    @Override
    SyntaxList copyReplacingChildren(Evaluator      eval,
                                     SyntaxValue... children)
        throws FusionException
    {
        BaseSymbol[] annotations = myImmutableList.getAnnotations();
        BaseList datum = (children == null
                              ? nullList(eval, annotations)
                              : immutableList(eval, annotations, children));
        return new SyntaxList(getLocation(), getProperties(), myWraps, datum);
    }


    @Override
    SyntaxList copyReplacingProperties(Object[] properties)
    {
        return new SyntaxList(getLocation(), properties, myWraps,
                              myImmutableList);
    }

    @Override
    SyntaxList copyReplacingWraps(SyntaxWraps wraps)
    {
        return new SyntaxList(getLocation(), getProperties(), wraps,
                              myImmutableList);
    }


    /**
     * If we have wraps cached here, push them down into fresh copies of all
     * children. This must be called before exposing any children outside of
     * this instance, so that it appears as if the wraps were pushed when they
     * were created.
     */
    private synchronized void pushWraps(Evaluator eval)
        throws FusionException
    {
        if (myWraps != null)  // We only have wraps when we have children.
        {
            boolean changed = false;
            int len = myImmutableList.size();
            SyntaxValue[] children = new SyntaxValue[len];
            for (int i = 0; i < len; i++)
            {
                SyntaxValue child = (SyntaxValue) myImmutableList.elt(eval, i);
                SyntaxValue wrapped = child.addWraps(myWraps);
                children[i] = wrapped;
                changed |= wrapped != child;
            }

            if (changed) // Keep sharing when we can
            {
                BaseSymbol[] annotations = myImmutableList.getAnnotations();
                myImmutableList = immutableList(eval, annotations, children);
            }

            myWraps = null;
        }
    }


    @Override
    SyntaxSequence stripWraps(Evaluator eval)
        throws FusionException
    {
        int len = myImmutableList.size();

        if (len == 0) return this;  // No children, no marks, all okay!

        // Even if we have no marks, some children may have them.
        boolean mustCopy = (myWraps != null);

        Object[] children = new Object[len];
        for (int i = 0; i < len; i++)
        {
            SyntaxValue child = (SyntaxValue) myImmutableList.elt(eval, i);
            SyntaxValue stripped = child.stripWraps(eval);
            children[i] = stripped;
            mustCopy |= stripped != child;
        }

        if (! mustCopy) return this;

        BaseSymbol[] annotations = myImmutableList.getAnnotations();
        BaseList newList = immutableList(eval, annotations, children);
        return new SyntaxList(getLocation(), getProperties(), null, newList);
    }


    @Override
    final boolean hasNoChildren()
    {
        return myImmutableList.size() == 0;
    }


    @Override
    final int size(Evaluator eval)
    {
        return myImmutableList.size();
    }


    @Override
    SyntaxValue[] extract(Evaluator eval)
        throws FusionException
    {
        if (myImmutableList.isAnyNull()) return null;

        pushWraps(eval);

        int len = myImmutableList.size();
        SyntaxValue[] extracted = new SyntaxValue[len];
        myImmutableList.unsafeCopy(eval, 0, extracted, 0, len);
        return extracted;
    }


    @Override
    SyntaxValue get(Evaluator eval, int index)
        throws FusionException
    {
        pushWraps(eval);
        return (SyntaxValue) myImmutableList.elt(eval, index);
    }


    @Override
    SyntaxSequence makeAppended(Evaluator eval, SyntaxSequence that)
        throws FusionException
    {
        int thisLength = this.size(eval);
        int thatLength = that.size(eval);
        int newLength  = thisLength + thatLength;

        if (newLength == 0) return this;

        Object[] children = new Object[newLength];
        if (thisLength != 0)
        {
            pushWraps(eval);
            myImmutableList.unsafeCopy(eval, 0, children, 0, thisLength);
        }
        if (thatLength != 0)
        {
            // that could be a sexp so we cant copy directly
            // TODO avoid intermediate array copy
            SyntaxValue[] c = that.extract(eval);
            arraycopy(c, 0, children, thisLength, thatLength);
        }

        BaseSymbol[] anns = myImmutableList.getAnnotations();
        BaseList list = immutableList(eval, anns, children);
        return new SyntaxList(eval, null, list);
    }


    @Override
    SyntaxSequence makeSubseq(Evaluator eval, int from)
        throws FusionException
    {
        if ((myImmutableList.size() == 0 || from == 0)
            && ! myImmutableList.isAnnotated())
        {
            return this;
        }

        pushWraps(eval);

        BaseList list;
        if (myImmutableList.isAnyNull())
        {
            list = FusionList.NULL_LIST;
        }
        else
        {
            // TODO will crash if `from` is beyond the end of the list
            int len = myImmutableList.size();
            Object[] children = new Object[len - from];
            myImmutableList.unsafeCopy(eval, from, children, 0, children.length);
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
    final void ionize(Evaluator eval, IonWriter out)
        throws IOException, FusionException
    {
        myImmutableList.ionize(eval, out);
    }

    @Override
    final void write(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        myImmutableList.write(eval, out);
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        pushWraps(eval);
        return myImmutableList;
    }


    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        int size = myImmutableList.size();
        if (size == 0)
        {
            return myImmutableList;
        }

        // Don't bother to push wraps; we'll just discard them anyway.
        Object[] children = new Object[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue child = (SyntaxValue) myImmutableList.elt(eval, i);
            children[i] = child.syntaxToDatum(eval);
        }

        BaseSymbol[] annotations = myImmutableList.getAnnotations();
        return immutableList(eval, annotations, children);
    }
}
