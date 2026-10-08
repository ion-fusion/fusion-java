// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import static dev.ionfusion.commons.util.Empties.EMPTY_STRING_ARRAY;

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
        return eval.getGlobalState().coreModuleIdentifier(eval);
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
}
