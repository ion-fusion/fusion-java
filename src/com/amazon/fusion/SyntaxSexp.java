// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionList.unsafeListToSexp;
import static com.amazon.fusion.FusionSexp.immutableSexp;
import static com.amazon.fusion.FusionSexp.isEmptySexp;
import static com.amazon.fusion.FusionSexp.isNullSexp;
import static com.amazon.fusion.FusionSexp.isPair;
import static com.amazon.fusion.FusionSexp.isSexp;
import static com.amazon.fusion.FusionSexp.nullSexp;
import static com.amazon.fusion.FusionSexp.pair;
import static com.amazon.fusion.FusionSexp.unsafePairDot;
import static com.amazon.fusion.FusionSexp.unsafePairHead;
import static com.amazon.fusion.FusionSexp.unsafePairTail;
import static com.amazon.fusion.FusionSexp.unsafeSexpSize;
import com.amazon.fusion.FusionSexp.BaseSexp;
import com.amazon.fusion.FusionSexp.ImmutablePair;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonWriter;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;

final class SyntaxSexp
    extends SyntaxSequence
{
    private BaseSexp mySexp;


    /**
     * @param sexp must not be null.
     */
    private SyntaxSexp(SourceLocation loc,
                       Object[]       properties,
                       SyntaxWraps    wraps,
                       BaseSexp       sexp)
    {
        super(loc, properties, wraps);
        assert sexp != null;
        mySexp = sexp;
    }


    /**
     * @param sexp must not be null.
     */
    private SyntaxSexp(SourceLocation loc, BaseSexp sexp)
    {
        super(loc);
        assert sexp != null;
        mySexp = sexp;
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    static SyntaxSexp makeOriginal(Evaluator      eval,
                                   SourceLocation loc,
                                   BaseSexp       sexp)
    {
        return new SyntaxSexp(loc, ORIGINAL_STX_PROPS, null, sexp);
    }

    static SyntaxSexp make(Evaluator eval, SourceLocation loc, BaseSexp sexp)
    {
        return new SyntaxSexp(loc, sexp);
    }

    /**
     * Instance will be {@link #isAnyNull()} if children is null.
     *
     * @param anns must not be null.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterwards!
     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterwards!
     */
    static SyntaxSexp make(Evaluator eval,
                           SourceLocation loc,
                           BaseSymbol[] anns,
                           SyntaxValue[] children)
    {
        BaseSexp datum = (children == null
                              ? nullSexp(eval, anns)
                              : immutableSexp(eval, anns, children));
        return new SyntaxSexp(loc, datum);
    }

    /**
     * Instance will be {@link #isAnyNull()} if children is null.

     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterwards!
     */
    static SyntaxSexp make(Evaluator eval, SyntaxValue... children)
    {
        return make(eval, null, BaseSymbol.EMPTY_ARRAY, children);
    }

    /**
     * Instance will be {@link #isAnyNull()} if children is null.

     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterwards!
     */
    static SyntaxSexp make(Evaluator eval, SourceLocation loc,
                           SyntaxValue... children)
    {
        return make(eval, loc, BaseSymbol.EMPTY_ARRAY, children);
    }


    //========================================================================


    @Override
    SyntaxSexp copyReplacingChildren(Evaluator      eval,
                                     SyntaxValue... children)
        throws FusionException
    {
        BaseSymbol[] annotations = mySexp.getAnnotations();
        BaseSexp datum = (children == null
                              ? nullSexp(eval, annotations)
                              : immutableSexp(eval, annotations, children));
        return new SyntaxSexp(getLocation(), getProperties(), myWraps, datum);
    }


    @Override
    SyntaxSexp copyReplacingProperties(Object[] properties)
    {
        return new SyntaxSexp(getLocation(), properties, myWraps, mySexp);
    }


    @Override
    SyntaxSexp copyReplacingWraps(SyntaxWraps wraps)
        throws FusionException
    {
        assert ! hasNoChildren() && wraps != null;
        return new SyntaxSexp(getLocation(), getProperties(), wraps, mySexp);
    }


    private static ImmutablePair pushWraps(Evaluator eval,
                                           ImmutablePair pair,
                                           SyntaxWraps wraps)
        throws FusionException
    {
        Object head = unsafePairHead(eval, pair);
        Object tail = unsafePairTail(eval, pair);

        Object newHead = head;
        if (head instanceof SyntaxValue)
        {
            newHead = ((SyntaxValue) head).addWraps(wraps);
        }

        Object newTail = tail;
        if (isPair(eval, tail))
        {
            newTail = pushWraps(eval, (ImmutablePair) tail, wraps);
        }
        else if (tail instanceof SyntaxValue)
        {
            newTail = ((SyntaxValue) tail).addWraps(wraps);
        }

        if (head != newHead || tail != newTail)
        {
            pair = pair(eval, pair.myAnnotations, newHead, newTail);
        }

        return pair;
    }


    /**
     * If we have wraps cached here, push them down into fresh copies of all
     * children. This must be called before exposing any children outside of
     * this instance, so that it appears as if the wraps were pushed when they
     * were created.
     */
    private void pushWraps(Evaluator eval)
        throws FusionException
    {
        if (myWraps != null)  // We only have wraps when we have children.
        {
            mySexp = pushWraps(eval, (ImmutablePair) mySexp, myWraps);
            myWraps = null;
        }
    }


    @Override
    SyntaxValue[] extract(Evaluator eval)
        throws FusionException
    {
        pushWraps(eval);

        if (isNullSexp(eval, mySexp)) return null;

        int len = unsafeSexpSize(eval, mySexp);
        SyntaxValue[] extracted = new SyntaxValue[len];

        int i = 0;
        for (Object p = mySexp; isPair(eval, p); p = unsafePairTail(eval, p))
        {
            extracted[i] = (SyntaxValue) unsafePairHead(eval, p);
            i++;
        }

        return extracted;
    }


    void extract(Evaluator eval, List<SyntaxValue> list, int from)
        throws FusionException
    {
        pushWraps(eval);

        int i = 0;
        for (Object p = mySexp; isPair(eval, p); p = unsafePairTail(eval, p))
        {
            if (from <= i)
            {
                SyntaxValue child = (SyntaxValue) unsafePairHead(eval, p);
                list.add(child);
            }
            i++;
        }
    }


    private static ImmutablePair stripWraps(Evaluator eval,
                                            ImmutablePair pair)
        throws FusionException
    {
        Object head = unsafePairHead(eval, pair);
        Object tail = unsafePairTail(eval, pair);

        Object newHead = head;
        if (head instanceof SyntaxValue)
        {
            newHead = ((SyntaxValue) head).stripWraps(eval);
        }

        Object newTail = tail;
        if (isPair(eval, tail))
        {
            newTail = stripWraps(eval, (ImmutablePair) tail);
        }
        else if (tail instanceof SyntaxValue)
        {
            newTail = ((SyntaxValue) tail).stripWraps(eval);
        }

        if (head != newHead || tail != newTail)
        {
            pair = pair(eval, pair.myAnnotations, newHead, newTail);
        }

        return pair;
    }

    @Override
    SyntaxSequence stripWraps(Evaluator eval)
        throws FusionException
    {
        if (hasNoChildren()) return this;  // No children, no marks, all okay!

        BaseSexp newSexp = stripWraps(eval, (ImmutablePair) mySexp);
        return new SyntaxSexp(getLocation(), getProperties(), null, newSexp);
    }


    @Override
    boolean hasNoChildren()
        throws FusionException
    {
        return ! (mySexp instanceof ImmutablePair);
    }


    @Override
    int size()
        throws FusionException
    {
        return mySexp.size();
    }


    @Override
    SyntaxValue get(Evaluator eval, int index)
        throws FusionException
    {
        pushWraps(eval);
        return (SyntaxValue) unsafePairDot(eval, mySexp, index);
    }


    @Override
    void ionize(Evaluator eval, IonWriter out)
        throws IOException, FusionException
    {
        // Ionization doesn't require wraps to be pushed.
        mySexp.ionize(eval, out);
    }

    @Override
    final void write(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        mySexp.write(eval, out);
    }


    //========================================================================
    // Helpers for quote, syntax_to_datum, and syntax_unwrap


    private static Object syntaxToDatum(Evaluator eval, BaseSexp sexp)
        throws FusionException
    {
        if (isPair(eval, sexp))
        {
            Object head = unsafePairHead(eval, sexp);
            head = ((SyntaxValue) head).syntaxToDatum(eval);

            Object tail = unsafePairTail(eval, sexp);
            if (isSexp(eval, tail))
            {
                tail = syntaxToDatum(eval, (BaseSexp) tail);
            }
            else
            {
                // TODO this is different from Racket, where only the first
                //  pair of a (proper) sexp is wrapped in a syntax object.
                tail = ((SyntaxValue) tail).syntaxToDatum(eval);
            }

            sexp = pair(eval, sexp.myAnnotations, head, tail);
        }

        return sexp;
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        pushWraps(eval);
        return mySexp;
    }


    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        // Don't bother to push wraps; we'll just discard them anyway.
        return syntaxToDatum(eval, mySexp);
    }


    //========================================================================
    // Helpers for syntax_append and syntax_subseq


    @Override
    SyntaxSequence makeAppended(Evaluator eval, SyntaxSequence that)
        throws FusionException
    {
        Object back = that.unwrap(eval);

        BaseSexp backSexp;
        if (that instanceof SyntaxSexp)
        {
            backSexp = (BaseSexp) back;
        }
        else
        {
            backSexp = unsafeListToSexp(eval, back);
        }

        pushWraps(eval);

        BaseSexp appended = mySexp.sexpAppend(eval, backSexp);
        if (appended == null)
        {
            return null;
        }

        return SyntaxSexp.make(eval, null, appended);
    }


    /**
     * @return null if not a proper sexp and we go beyond the end.
     */
    private static Object subseq(Evaluator eval, Object sexp, int from)
        throws FusionException
    {
        if (from == 0) return sexp;

        if (isEmptySexp(eval, sexp))
        {
            return sexp;
        }

        if (isPair(eval, sexp))
        {
            Object tail = unsafePairTail(eval, sexp);
            return subseq(eval, tail, from - 1);
        }

        return null;
    }

    @Override
    SyntaxSequence makeSubseq(Evaluator eval, int from)
        throws FusionException
    {
        pushWraps(eval);

        BaseSexp sub = (BaseSexp) subseq(eval, mySexp, from);

        return make(eval, null, sub);
    }



    //========================================================================

    /**
     * Finds the leading identifier in this sexp.
     *
     * @return null if this sexp doesn't start with an identifier.
     */
    SyntaxSymbol firstIdentifier(Evaluator eval)
        throws FusionException
    {
        if (isPair(eval, mySexp))
        {
            pushWraps(eval);

            Object first = unsafePairHead(eval, mySexp);
            if (first instanceof SyntaxSymbol)
            {
                return (SyntaxSymbol) first;
            }
        }
        return null;
    }

    /**
     * Finds the {@linkplain Binding#target() target binding} for the leading
     * identifier in this sexp.
     *
     * @return null if this sexp doesn't start with an identifier.
     * Null is also equivalent to a {@link FreeBinding} on a lead identifier.
     */
    Binding firstTargetBinding(Evaluator eval)
        throws FusionException
    {
        SyntaxSymbol first = firstIdentifier(eval);
        if (first != null)
        {
            Binding binding = first.uncachedResolveMaybe();
            if (binding != null) return binding.target();
        }
        return null;
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();

        int len = size();
        if (len == 0)
        {
            String message =
                "not a valid syntactic form. You probably want to quote this.";
            throw new SyntaxException(null, message, this);
        }

        SyntaxValue[] children = extract(eval);

        SyntaxValue first = children[0];
        if (first instanceof SyntaxSymbol)
        {
            // Do not assume that the symbol will be treated as a variable,
            // since this scope may override #%app or #%variable-reference.
            // Thus we cannot call expand on the symbol and must not cache the
            // results of the binding resolution unless it resolves to syntax.
            SyntacticForm form =
                ((SyntaxSymbol) first).resolveSyntaxMaybe(env);
            if (form != null)
            {
                // We found a static top-level binding to a built-in form or a
                // macro. Continue the expansion process.

                // TODO FUSION-31 identifier macros entail extra work here.
                assert expander.expand(env, first) == first;
                // else the next stmt must change

                // We use the same expansion context as we already have.
                // Don't need to replace the sexp since we haven't changed it.
                SyntaxValue expandedExpr = expander.expand(env, form, this);
                return expandedExpr;
                // TODO FUSION-207 Needs tail-call optimization.
            }
        }

        // else we have a procedure application, expand each subform as an
        // expression
        for (int i = 0; i < len; i++)
        {
            SyntaxValue subform = children[i];
            children[i] = expander.expandExpression(env, subform);
        }

        return this.copyReplacingChildren(eval, children);
    }


    /**
     * This is actually an incomplete implementation of something like Racket's
     * {@code local-expand}. Partial expansion is defined based on core syntax
     * forms, not on a given stop-list.
     *
     * @see Expander#partialExpand(Environment, SyntaxValue)
     *
     * @deprecated No longer in use but I'm not ready to delete it.
     */
    @Deprecated
    final SyntaxValue partialExpand(Expander expander, Environment env,
                                    IdentityHashMap<Binding, Object> stops)
        throws FusionException
    {
        int len = size();
        if (len == 0)
        {
            String message =
                "not a valid syntactic form. You probably want to quote this.";
            throw new SyntaxException(null, message, this);
        }

        Evaluator eval = expander.getEvaluator();
        SyntaxValue first = get(eval, 0); // calls pushAnyWraps()
        if (first instanceof SyntaxSymbol)
        {
            SyntaxSymbol maybeMacro = (SyntaxSymbol) first;
            SyntaxValue prepared = expander.expand(env, maybeMacro);

            // Identifier has been expanded to #%top, we can stop.
            if (prepared != maybeMacro) return this;

            Binding binding = maybeMacro.getBinding();
            if (stops.get(binding) != null)
            {
                return this;
            }

            Object resolved = maybeMacro.resolveSyntaxMaybe(env);
            if (resolved instanceof MacroForm)
            {
                // We found a static top-level macro binding!
                SyntaxValue expanded =
                    ((MacroForm)resolved).expandOnce(expander, this);
                if (expanded instanceof SyntaxSexp)
                {
                    // TODO replace recursion with iteration
                    return ((SyntaxSexp)expanded).partialExpand(expander, env,
                                                                stops);
                }
                return expanded;
            }
            // else not a macro, so just stop here.
        }

        return this;
    }
}
