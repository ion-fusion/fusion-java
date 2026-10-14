// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import static dev.ionfusion.commons.util.Empties.EMPTY_STRING_ARRAY;
import static dev.ionfusion.fusion.GlobalState.MODULE;

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
    static SyntaxValue datumToSyntax(Evaluator        eval,
                                     Object           datum,
                                     SyntaxValue      context,
                                     ResourcePosition pos)
        throws FusionException
    {
        if (datum instanceof BaseValue)
        {
            return ((BaseValue) datum).datumToSyntax(eval, context, pos);
        }

        return SimpleSyntaxValue.makeSyntax(eval, pos, datum);
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
