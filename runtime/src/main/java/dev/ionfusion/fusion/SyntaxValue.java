// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionCompare.isSame;
import static dev.ionfusion.fusion.FusionSexp.emptySexp;
import static dev.ionfusion.fusion.FusionSexp.pair;
import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_OBJECT_ARRAY;
import static java.lang.Boolean.TRUE;

import com.amazon.ion.IonValue;
import dev.ionfusion.runtime.base.SourceLocation;
import java.util.Arrays;

/**
 * Models Fusion source code, using a custom DOM implementation of Ion.
 * Unlike the {@link IonValue} model, this one allows sharing of nodes in a
 * DAG structure.
 */
abstract class SyntaxValue
    extends BaseValue
{
    /** A zero-length array. */
    static final SyntaxValue[] EMPTY_ARRAY = new SyntaxValue[0];

    /**
     * Private key used to identify syntax objects constructed by the reader.
     * We don't use a normal symbol here, because the property key must be
     * kept private: a symbol would be interned and therefore reproducible.
     */
    static final Object STX_PROPERTY_ORIGINAL = new String("is_original");

    /**
     * Syntax properties list used when creating "original" syntax via the
     * {@link StandardReader}.
     */
    final static Object[] ORIGINAL_STX_PROPS =
        new Object[] { STX_PROPERTY_ORIGINAL, Boolean.TRUE };


    private final SourceLocation mySrcLoc;

    /** Not null, to streamline things. */
    private final Object[] myProperties;


    /**
     * @param loc may be null.
     * @param properties must not be null.
     */
    SyntaxValue(SourceLocation loc, Object[] properties)
    {
        assert properties != null;
        mySrcLoc = loc;
        myProperties = properties;
    }


    // This final override isn't semantically necessary, but it exists to
    // ensure that a syntax object is never considered null.
    @Override
    final boolean isAnyNull()
    {
        return false;
    }


    /**
     * Gets the location associated with this syntax node, if it exists.
     * @return may be null.
     */
    SourceLocation getLocation()
    {
        return mySrcLoc;
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


    abstract SyntaxValue copyReplacingProperties(Object[] properties);


    SyntaxValue copyWithProperty(Evaluator eval, Object key, Object value)
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


    final SyntaxValue trackOrigin(Evaluator    eval,
                                  SyntaxValue  origStx,
                                  SyntaxSymbol origin)
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


    final boolean isOriginal(Evaluator eval)
        throws FusionException
    {
        Object o = findProperty(eval, STX_PROPERTY_ORIGINAL);
        return o == TRUE && ! hasMarks(eval);
    }


    /**
     * Prepends a wrap onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    SyntaxValue addWrap(SyntaxWrap wrap)
        throws FusionException
    {
        return this;
    }

    /**
     * Prepends a sequence of wraps onto our existing wraps.
     * This will return a new instance as necessary to preserve immutability.
     */
    SyntaxValue addWraps(SyntaxWraps wraps)
        throws FusionException
    {
        return this;
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
        return false;
    }


    /**
     * Removes any wraps from this value and any children.
     * @return an equivalent syntax value with no wraps.
     * May return this instance when that's already the case.
     */
    SyntaxValue stripWraps(Evaluator eval)
        throws FusionException
    {
        return this;
    }


    /** Don't call directly! Go through the evaluator. */
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        return this;
    }


    /**
     * Unwraps syntax, returning plain values. Only one layer is unwrapped, so
     * if this is a container, the result will contain syntax objects.
     */
    abstract Object unwrap(Evaluator eval)
        throws FusionException;


    /**
     * Unwraps syntax recursively, returning plain values.
     * Used by `quote` and `synatax_to_datum`.
     */
    abstract Object syntaxToDatum(Evaluator eval)
        throws FusionException;


    @Override
    final SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                         SyntaxSymbol   context,
                                         SourceLocation loc)
        throws FusionException
    {
        return this;
    }


    @Override
    SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
    {
        throw new IllegalStateException("Cannot wrap syntax as syntax");
    }


    //========================================================================
    // Visitation


    abstract Object visit(Visitor v) throws FusionException;


    static abstract class Visitor
    {
        Object accept(SyntaxValue stx) throws FusionException
        {
            String msg = "Visitor doesn't accept " + getClass();
            throw new IllegalStateException(msg);
        }

        Object accept(SimpleSyntaxValue stx) throws FusionException
        {
            return accept((SyntaxValue) stx);
        }

        Object accept(SyntaxSymbol stx) throws FusionException
        {
            return accept((SyntaxText) stx);
        }

        Object accept(SyntaxKeyword stx) throws FusionException
        {
            return accept((SyntaxText) stx);
        }

        Object accept(SyntaxContainer stx) throws FusionException
        {
            return accept((SyntaxValue) stx);
        }

        Object accept(SyntaxList stx) throws FusionException
        {
            return accept((SyntaxSequence) stx);
        }

        Object accept(SyntaxSexp stx) throws FusionException
        {
            return accept((SyntaxSequence) stx);
        }

        Object accept(SyntaxStruct stx) throws FusionException
        {
            return accept((SyntaxContainer) stx);
        }
    }
}
