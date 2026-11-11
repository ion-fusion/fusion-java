// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import static dev.ionfusion.commons.util.Empties.EMPTY_STRING_ARRAY;
import static dev.ionfusion.fusion.FusionSexp.pair;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpMap;
import static dev.ionfusion.fusion.FusionValue.unsafeCopyAnnotations;
import static dev.ionfusion.fusion.GlobalState.MODULE;
import static dev.ionfusion.fusion.SyntaxValue.NO_PROPS;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.runtime.base.FusionException;

/**
 * Internal utilities for working with {@link SyntaxValue}s.
 */
final class Syntax
{
    private Syntax() {}

    @Deprecated
    static boolean isSyntax(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxValue);
    }

    @Deprecated
    static boolean isIdentifier(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxSymbol);
    }

    static boolean isBindableIdentifier(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof SyntaxSymbol) &&
               ((SyntaxSymbol) value).unwrap(eval).isNonEmpty();
    }


    /**
     * @param context can be null, in which case no lexical information is
     * applied to converted objects.
     *
     * @return not null.
     */
    static SyntaxValue<?> datumToSyntax(Evaluator        eval,
                                        Object           datum,
                                        SyntaxValue<?>   context,
                                        ResourcePosition pos)
        throws FusionException
    {
        return datumToSyntax(eval, datum, context, pos, NO_PROPS);
    }

    /**
     * Converts data into a syntax object.
     *
     * @param datum the object to convert. If it's already a syntax object, it is
     * returned as is.
     * @param context can be null, in which case no lexical information is
     * applied to converted objects.
     * @param pos can be null.
     * @param props must not be null.
     *
     * @return not null.
     */
    static SyntaxValue<?> datumToSyntax(Evaluator        eval,
                                        Object           datum,
                                        SyntaxValue<?>   context,
                                        ResourcePosition pos,
                                        Object[]         props)
        throws FusionException
    {
        if (datum instanceof BaseValue)
        {
            return ((BaseValue) datum).datumToSyntax(eval, context, pos, props);
        }

        return SimpleSyntaxValue.makeSyntax(eval, datum, pos, props)
                                .initLexicalContext(context);
    }


    /**
     * Like {@link #datumToSyntax} but copying syntax metadata from the original.
     */
    static SyntaxValue<?> rebuild(Evaluator eval,
                                  Object content,
                                  SyntaxValue<?> original)
        throws FusionException
    {
        return datumToSyntax(eval,
                             content,
                             original, // Lexical context
                             original.getPosition(),
                             original.getProperties());
    }


    static SyntaxSexp replaceHead(Evaluator eval,
                                  SyntaxSexp stx,
                                  SyntaxValue<?> newHead)
        throws FusionException
    {
        var sexp    = stx.unwrap(eval);
        var tail    = unsafePairTail(eval, sexp);
        var newSexp = unsafeCopyAnnotations(eval, pair(eval, newHead, tail), sexp);

        return (SyntaxSexp) rebuild(eval, newSexp, stx);
    }


    /**
     * Maps a function over elements of a sexp, except for the head.
     */
    static SyntaxSexp mapOverSubforms(Evaluator eval,
                                      SyntaxSexp stx,
                                      Evaluator.Function<Object,?> fn)
        throws FusionException
    {
        var sexp  = stx.unwrap(eval);
        var ifId  = unsafePairHead(eval, sexp);
        var exprs = unsafePairTail(eval, sexp);

        var expanded = unsafeSexpMap(eval, exprs, fn);

        var newSexp = unsafeCopyAnnotations(eval, pair(eval, ifId, expanded), sexp);
        return (SyntaxSexp) rebuild(eval, newSexp, stx);
    }


    /**
     * Returns an identifier whose binding is the core {@code module} form.
     */
    static SyntaxSymbol coreModuleIdentifier(Evaluator eval)
        throws FusionException
    {
        return eval.getGlobalState().coreIdentifier(eval, MODULE);
    }


    /**
     * Extract the names from an array of identifiers.
     *
     * @param symbols can be null.
     */
    static String[] identifiersToNames(SyntaxSymbol[] symbols)
    {
        if (symbols == null || symbols.length == 0)
        {
            return EMPTY_STRING_ARRAY;
        }

        String[] names = new String[symbols.length];
        for (int i = 0; i < symbols.length; i++)
        {
            names[i] = symbols[i].stringValue();
        }
        return names;
    }


    static boolean boundIdentifierEqual(SyntaxSymbol a, SyntaxSymbol b)
    {
        BoundIdentifier idA = a.resolveBoundIdentifier();
        BoundIdentifier idB = b.resolveBoundIdentifier();
        return idA.equals(idB);
    }

    static boolean freeIdentifierEqual(SyntaxSymbol a, SyntaxSymbol b)
    {
        Binding bindingA = a.resolve();
        Binding bindingB = b.resolve();
        return bindingA.sameTarget(bindingB);
    }


    /**
     * @param id can be null
     */
    static boolean hasCoreBinding(Evaluator eval, SyntaxSymbol id, String form)
        throws FusionException
    {
        var core = eval.getGlobalState().coreIdentifier(eval, form);
        return id != null && freeIdentifierEqual(id, core);
    }


    /**
     * If the given syntax object is a sexp starting with a core form of the given name,
     * return it, otherwise return null.
     */
    static SyntaxSexp asCoreForm(Evaluator eval, SyntaxValue<?> stx, String form)
        throws FusionException
    {
        if (stx instanceof SyntaxSexp)
        {
            var sexp = (SyntaxSexp) stx;
            var first = sexp.firstIdentifier(eval);
            if (hasCoreBinding(eval, first, form))
            {
                return sexp;
            }
        }
        return null;
    }

    static boolean isCoreForm(Evaluator eval, SyntaxValue<?> stx, String form)
        throws FusionException
    {
        return asCoreForm(eval, stx, form) != null;
    }
}
