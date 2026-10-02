// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;
import static dev.ionfusion.fusion.FusionCompare.isSame;
import static dev.ionfusion.fusion.FusionSexp.emptySexp;
import static dev.ionfusion.fusion.FusionSexp.pair;
import static java.lang.Boolean.TRUE;

import com.amazon.ion.IonValue;
import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Models Fusion source code, using a custom DOM implementation of Ion.
 * Unlike the {@link IonValue} model, this one allows sharing of nodes in a
 * DAG structure.
 */
abstract class SyntaxValue<Content>
    extends BaseValue
{
    /** A zero-length array. */
    static final SyntaxValue[] EMPTY_ARRAY = new SyntaxValue[0];

    /**
     * Private key used to identify syntax objects constructed by the reader.
     * We don't use a normal symbol here because the property key must be
     * kept private: a symbol would be interned and therefore reproducible.
     */
    static final Object STX_PROPERTY_ORIGINAL = new String("is_original");

    /**
     * Syntax properties list used when creating "original" syntax via the
     * {@link StandardReader}.
     */
    final static Object[] ORIGINAL_STX_PROPS =
        new Object[] { STX_PROPERTY_ORIGINAL, Boolean.TRUE };

    /**
     * The wrapped content; not null.
     * Can be mutated by {@link #propagateLexicalContext(Evaluator)}
     */
    private Content myContent;

    /**
     * The lexical context collected during expansion; not null.
     * It is not final because we use mutation to lazily propagate context to children.
     * <p>
     * TODO make private to control mutation.
     */
    SyntaxWraps myWraps;

    private final ResourcePosition myPosition;

    /** Not null, to streamline things. */
    private final Object[] myProperties;


    /**
     * @param content must not be null and must not be a {@link SyntaxValue}.
     * @param pos can be null.
     * @param properties must not be null.
     */
    SyntaxValue(Content content,
                SyntaxWraps wraps,
                ResourcePosition pos,
                Object[] properties)
    {
        assert !(content instanceof SyntaxValue);
        assert wraps != null;
        assert properties != null;
        myContent = content;
        myWraps = wraps;
        myPosition = pos;
        myProperties = properties;
    }

    /**
     * @param pos can be null.
     */
    SyntaxValue(Content content, SyntaxWraps wraps, ResourcePosition pos)
    {
        this(content, wraps, pos, EMPTY_OBJECT_ARRAY);
    }

    /**
     * @param pos can be null.
     */
    SyntaxValue(Content content, ResourcePosition pos)
    {
        this(content, SyntaxWraps.EMPTY, pos, EMPTY_OBJECT_ARRAY);
    }


    /** Does not propagate context! */
    abstract SyntaxValue<Content> copyReplacing(SyntaxWraps wraps, Object[] properties);

    /** Does not propagate context! */
    private SyntaxValue<Content> copyReplacingWraps(SyntaxWraps wraps)
    {
        return copyReplacing(wraps, getProperties());
    }

    /** Does not propagate context! */
    private SyntaxValue<Content> copyReplacingProperties(Object[] properties)
    {
        return copyReplacing(getWraps(), properties);
    }


    // This final override isn't semantically necessary, but it exists to
    // ensure that a syntax object is never considered null.
    @Override
    final boolean isAnyNull()
    {
        return false;
    }


    private static final DynamicParameter WRITE_EXPLICIT_SYNTAX =
        new DynamicParameter(true);

    @Override
    final void write(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        Object content = unwrap(eval);

        // When possible, only write the explicit wrapper on the outside edge.
        if (eval != null && (boolean) WRITE_EXPLICIT_SYNTAX.currentValue(eval))
        {
            eval = eval.parameterize(WRITE_EXPLICIT_SYNTAX, false);
            out.append("stx::{{{");
            FusionIo.write(eval, out, content);
            out.append("}}}");
        }
        else
        {
            FusionIo.write(eval, out, content);
        }
    }


    /**
     * Gets the resource and position associated with this syntax node, if it exists.
     * @return can be null.
     */
    ResourcePosition getPosition()
    {
        return myPosition;
    }


    Object[] getProperties()
    {
        return myProperties;
    }

    /**
     * @param key must not be null.
     * @return void if no value is associated with the key.
     */
    Object findProperty(Evaluator eval, Object key)
        throws FusionException
    {
        for (int i = 0; i < myProperties.length; i += 2)
        {
            if (isSame(eval, key, myProperties[i]).isTrue())
            {
                return myProperties[i + 1];
            }
        }
        return FusionVoid.voidValue(eval);
    }


    SyntaxValue<Content> copyWithProperty(Evaluator eval, Object key, Object value)
        throws FusionException
    {
        // Determine whether the property already exists so we can replace it.
        int length = myProperties.length;
        for (int i = 0; i < length; i += 2)
        {
            if (isSame(eval, key, myProperties[i]).isTrue())
            {
                Object[] newProperties = Arrays.copyOf(myProperties, length);
                newProperties[i + 1] = value;
                return copyReplacingProperties(newProperties);
            }
        }

        Object[] newProperties = Arrays.copyOf(myProperties, length + 2);
        newProperties[length    ] = key;
        newProperties[length + 1] = value;
        return copyReplacingProperties(newProperties);
    }




    //========================================================================
    // Original syntax tracking

    @Override
    final SyntaxValue<Content> makeOriginalSyntax(Evaluator eval, ResourcePosition pos)
    {
        throw new IllegalStateException("Cannot wrap syntax as syntax");
    }


    final SyntaxValue<Content> trackOrigin(Evaluator      eval,
                                           SyntaxValue<?> origStx,
                                           SyntaxSymbol   origin)
        throws FusionException
    {
        Object stxPropOrigin = eval.getGlobalState().myStxPropOrigin;

        Object[] oProps = origStx.myProperties;
        if (oProps == ORIGINAL_STX_PROPS) oProps = EMPTY_OBJECT_ARRAY;

        // Reserve space for origin, in case either list has it yet.
        int maxLen = oProps.length + myProperties.length + 2;
        Object[] merged = new Object[maxLen];
        int m = 0;

        for (int i = 0; i < myProperties.length; i += 2)
        {
            Object k = myProperties[i];
            Object v = myProperties[i + 1];

            if (k != STX_PROPERTY_ORIGINAL)
            {
                // Look for the same property on the original object.
                // If found, combine the values.
                for (int j = 0; j < oProps.length; j += 2)
                {
                    if (isSame(eval, k, oProps[j]).isTrue())
                    {
                        Object o = oProps[j + 1];
                        if (k == stxPropOrigin)
                        {
                            assert origin != null;
                            o = pair(eval, origin, o);
                            origin = null;
                        }
                        v = pair(eval, v, o);
                        break;
                    }
                }

                if (origin != null && k == stxPropOrigin)
                {
                    Object o = emptySexp(eval);
                    o = pair(eval, origin, o);
                    v = pair(eval, v, o);
                    origin = null;
                }
            }

            merged[m++] = k;
            merged[m++] = v;
        }

        // Copy what remains from the original properties.
        pass2:
        for (int i = 0; i < oProps.length; i += 2)
        {
            Object k = oProps[i];

            if (k != STX_PROPERTY_ORIGINAL)
            {
                Object v = oProps[i + 1];

                for (int j = 0; j < myProperties.length; j += 2)
                {
                    if (isSame(eval, k, myProperties[j]).isTrue())
                    {
                        // We already merged this property in pass 1 above.
                        continue pass2;
                    }
                }

                if (origin != null && k == stxPropOrigin)
                {
                    v = pair(eval, origin, v);
                    origin = null;
                }

                merged[m++] = k;
                merged[m++] = v;
            }
        }

        // We haven't found origin in either list, so add it.
        if (origin != null)
        {
            Object v = emptySexp(eval);
            v = pair(eval, origin, v);

            merged[m++] = stxPropOrigin;
            merged[m++] = v;
        }

        // Remove empty space at the end.
        if (merged.length != m)
        {
            merged = Arrays.copyOf(merged, m);
        }

        return copyReplacingProperties(merged);
    }


    /**
     * Per Racket:
     *
     * "Returns #t if stx has the property that read-syntax attaches to the syntax
     * objects that they generate, and if stx’s lexical information does not include any
     * macro-introduction scopes (which indicate that the object was introduced by a
     * syntax transformer)."
     */
    final boolean isOriginal(Evaluator eval)
        throws FusionException
    {
        // Implementation in expander/syntax/scope.rkt:
        //
        // (define (syntax-any-macro-scopes? s)
        //   (for/or ([sc (in-set (syntax-scopes s))])
        //     (eq? (scope-kind sc) 'macro)))

        Object o = findProperty(eval, STX_PROPERTY_ORIGINAL);
        return o == TRUE && ! hasMarks(eval);
    }


    //========================================================================
    // Lexical Context

    /**
     * @param context can be null.
     */
    final SyntaxValue<Content> initLexicalContext(SyntaxValue<?> context)
    {
        assert !(context instanceof SyntaxStruct) &&
               !(context instanceof SyntaxSequence)
            : "Can't use container as lexical context; see #68";
        assert myWraps == SyntaxWraps.EMPTY : "Already initialized";

        if (context != null)
        {
            myWraps = context.myWraps;
        }
        return this;
    }


    /**
     * Push any cached context down into fresh copies of all contained syntax objects.
     * This must be called before exposing any children outside of this instance, so
     * it appears as if the context was pushed when it was added.
     * <p>
     * This mutates {@link #myWraps} and content field.
     */
    synchronized void propagateLexicalContext(Evaluator eval)
        throws FusionException
    {
        if (myWraps != SyntaxWraps.EMPTY)
        {
            myContent = propagateLexicalContent(eval, myContent, myWraps);
            myWraps = SyntaxWraps.EMPTY;
        }
    }

    /**
     * Type-specific content propagation.
     *
     * @param propagate is not null
     */
    abstract Content propagateLexicalContent(Evaluator eval,
                                             Content content,
                                             SyntaxWraps propagate)
        throws FusionException;


    final SyntaxValue<Content> stripLexicalContext(Evaluator eval)
        throws FusionException
    {
        // Make sure we don't lose cached context that should be on children.
        propagateLexicalContext(eval);
        return myWraps == SyntaxWraps.EMPTY
               ? this
               : copyReplacingWraps(SyntaxWraps.EMPTY);
    }


    final SyntaxWraps getWraps()
    {
        return myWraps;
    }

    /**
     * Prepends a wrap onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    final SyntaxValue<Content> addWrap(SyntaxWrap wrap)
        throws FusionException
    {
        assert wrap != null;

        SyntaxWraps newWraps = myWraps.addWrap(wrap);

        return copyReplacingWraps(newWraps);
    }

    /**
     * Prepends a sequence of wraps onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    final SyntaxValue<Content> addWraps(SyntaxWraps wraps)
        throws FusionException
    {
        assert wraps != null;

        SyntaxWraps newWraps = myWraps.addWraps(wraps);

        return copyReplacingWraps(newWraps);
    }


    final SyntaxValue addOrRemoveMark(MarkWrap mark)
        throws FusionException
    {
        // 2014-07-03 Only 32/906 (3.5%) of marks matched the first wrap.
        //            Eliminating those didn't increase that count.
        // 2016-08-17 I bet that's because the mark has usually been pushed.
        return addWrap(mark);
    }

    boolean hasMarks(Evaluator eval)
    {
        return myWraps.hasMarks(eval);
    }


    //========================================================================

    /** Don't call directly! Go through the evaluator. */
    SyntaxValue<Content> doExpand(Expander expander, Environment env)
        throws FusionException
    {
        return this;
    }


    /**
     * Returns our content like {@link #unwrap}, but does not propagate context.
     */
    final Content getContent()
    {
        return myContent;
    }

    /**
     * Unwraps syntax, propagating lexical context and returning a plain value.
     * Only one layer is unwrapped: if the content is a container, the result will
     * contain syntax objects.
     * <p>
     * This method is equivalent to Racket's `syntax-e`.
     */
    Content unwrap(Evaluator eval)
        throws FusionException
    {
        propagateLexicalContext(eval);
        return myContent;
    }


    /**
     * Unwraps syntax recursively, returning plain values.
     * Used by `quote` and `syntax_to_datum`.
     */
    abstract Object syntaxToDatum(Evaluator eval)
        throws FusionException;


    @Override
    final SyntaxValue datumToSyntax(Evaluator        eval,
                                    SyntaxValue      context,
                                    ResourcePosition pos)
        throws FusionException
    {
        return this;
    }
}
