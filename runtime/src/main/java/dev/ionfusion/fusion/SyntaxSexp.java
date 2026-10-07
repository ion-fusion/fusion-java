// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.unsafeListToSexp;
import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionSexp.isEmptySexp;
import static dev.ionfusion.fusion.FusionSexp.isNullSexp;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSexp.nullSexp;
import static dev.ionfusion.fusion.FusionSexp.pair;
import static dev.ionfusion.fusion.FusionSexp.unsafePairDot;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpSize;
import static dev.ionfusion.fusion.SyntaxException.makeSyntaxError;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import dev.ionfusion.fusion.FusionSexp.ImmutablePair;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.FusionException;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.List;

final class SyntaxSexp
    extends SyntaxSequence<BaseSexp>
{
    /**
     * @param sexp must not be null.
     */
    private SyntaxSexp(ResourcePosition pos,
                       Object[]         properties,
                       SyntaxWraps      wraps,
                       BaseSexp         sexp)
    {
        super(sexp, pos, properties, wraps);
    }


    /**
     * @param sexp must not be null.
     */
    private SyntaxSexp(ResourcePosition pos, BaseSexp sexp)
    {
        super(sexp, pos);
    }


    @Override
    SyntaxSexp copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        return new SyntaxSexp(getPosition(), properties, wraps, getContent());
    }


    static SyntaxSexp makeOriginal(Evaluator eval, ResourcePosition pos, BaseSexp sexp)
    {
        return new SyntaxSexp(pos, ORIGINAL_STX_PROPS, SyntaxWraps.EMPTY, sexp);
    }

    static SyntaxSexp make(Evaluator eval, ResourcePosition pos, BaseSexp sexp)
    {
        return new SyntaxSexp(pos, sexp);
    }

    /**
     * Instance will be {@link #isAnyNull()} if {@code children} is null.
     *
     * @param anns must not be null.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterward!
     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterward!
     */
    static SyntaxSexp make(Evaluator eval,
                           ResourcePosition pos,
                           BaseSymbol[] anns,
                           SyntaxValue[] children)
    {
        BaseSexp datum = (children == null
                              ? nullSexp(eval, anns)
                              : immutableSexp(eval, anns, children));
        return new SyntaxSexp(pos, datum);
    }

    /**
     * Instance will be {@link #isAnyNull()} if {@code children} is null.

     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterward!
     */
    static SyntaxSexp make(Evaluator eval, SyntaxValue... children)
    {
        return make(eval, null, BaseSymbol.EMPTY_ARRAY, children);
    }

    /**
     * Instance will be {@link #isAnyNull()} if {@code children} is null.

     * @param children the children of the new sexp.
     * This method takes ownership of the array; the array and its elements
     * must not be changed by calling code afterward!
     */
    static SyntaxSexp make(Evaluator eval, ResourcePosition pos,
                           SyntaxValue... children)
    {
        return make(eval, pos, BaseSymbol.EMPTY_ARRAY, children);
    }


    //========================================================================


    @Override
    SyntaxSexp copyReplacingChildren(Evaluator      eval,
                                     SyntaxValue... children)
        throws FusionException
    {
        BaseSymbol[] annotations = getContent().getAnnotations();
        BaseSexp datum = (children == null
                              ? nullSexp(eval, annotations)
                              : immutableSexp(eval, annotations, children));
        return new SyntaxSexp(getPosition(), getProperties(), getWraps(), datum);
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


    @Override
    BaseSexp propagateLexicalContext(Evaluator eval,
                                     BaseSexp content,
                                     SyntaxWraps propagate)
        throws FusionException
    {
        if (content instanceof ImmutablePair)
        {
            return pushWraps(eval, (ImmutablePair) content, propagate);
        }
        // else our content is `null.sexp` or `()`

        return content;
    }


    @Override
    SyntaxValue[] extract(Evaluator eval)
        throws FusionException
    {
        return extract(eval, SyntaxValue.class);
    }

    <T> T[] extract(Evaluator eval, Class<T> klass)
        throws FusionException
    {
        var thisSexp = unwrap(eval);
        if (isNullSexp(eval, thisSexp)) return null;

        int len = unsafeSexpSize(eval, thisSexp);
        T[] extracted = (T[]) Array.newInstance(klass, len);

        int i = 0;
        for (Object p = thisSexp; isPair(eval, p); p = unsafePairTail(eval, p))
        {
            extracted[i] = klass.cast(unsafePairHead(eval, p));
            i++;
        }

        return extracted;
    }


    void extract(Evaluator eval, List<SyntaxValue> list, int from)
        throws FusionException
    {
        int i = 0;
        for (Object p = unwrap(eval); isPair(eval, p); p = unsafePairTail(eval, p))
        {
            if (from <= i)
            {
                SyntaxValue child = (SyntaxValue) unsafePairHead(eval, p);
                list.add(child);
            }
            i++;
        }
    }


    @Override
    int size(Evaluator eval)
        throws FusionException
    {
        return getContent().size(eval);
    }


    @Override
    SyntaxValue get(Evaluator eval, int index)
        throws FusionException
    {
        return (SyntaxValue) unsafePairDot(eval, unwrap(eval), index);
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

            sexp = pair(eval, sexp.getAnnotations(), head, tail);
        }

        return sexp;
    }


    @Override
    Object syntaxToDatum(Evaluator eval)
        throws FusionException
    {
        // Don't bother to push wraps; we'll just discard them anyway.
        return syntaxToDatum(eval, getContent());
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

        BaseSexp appended = unwrap(eval).sexpAppend(eval, backSexp);
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
        BaseSexp sub = (BaseSexp) subseq(eval, unwrap(eval), from);

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
        var thisSexp = unwrap(eval);
        if (isPair(eval, thisSexp))
        {
            Object first = unsafePairHead(eval, thisSexp);
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

    /**
     * If this expression starts with a syntax form identifier, return it.
     *
     * @return null if the first child isn't an identifier or isn't bound to a
     * syntax form.
     */
    SyntacticForm syntaxForm(Evaluator eval, Environment env)
        throws FusionException
    {
        SyntaxSymbol id = firstIdentifier(eval);
        return (id == null ? null : id.resolveSyntaxMaybe(env));
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();

        int len = size(eval);
        if (len == 0)
        {
            String message =
                "not a valid syntactic form. You probably want to quote this.";
            throw makeSyntaxError(eval, null, message, this);
        }

        SyntacticForm form = syntaxForm(eval, env);       // propagates lexical context
        if (form != null)
        {
            // We found a static top-level binding to a built-in form or a
            // macro. Continue the expansion process.

            // TODO #72 identifier macros entail extra work here; we should instead
            //  fall through to expanding each child.

            // We use the same expansion context as we already have.
            // Don't need to replace the sexp since we haven't changed it.
            return expander.expand(env, form, this);
            // TODO #71 Eliminate this tail-call.
        }

        // else we have a procedure application, expand each subform as an
        // expression
        SyntaxValue[] children = extract(eval);
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
        Evaluator eval = expander.getEvaluator();
        int len = size(eval);
        if (len == 0)
        {
            String message =
                "not a valid syntactic form. You probably want to quote this.";
            throw makeSyntaxError(eval, null, message, this);
        }

        SyntaxValue first = get(eval, 0); // calls pushAnyWraps()
        if (first instanceof SyntaxSymbol)
        {
            SyntaxSymbol maybeMacro = (SyntaxSymbol) first;
            SyntaxValue prepared = expander.expand(env, maybeMacro);

            // Identifier has been expanded to #%top, we can stop.
            if (prepared != maybeMacro) return this;

            Binding binding = maybeMacro.resolve();
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
