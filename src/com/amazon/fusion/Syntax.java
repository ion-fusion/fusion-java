// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;


/**
 * Utilities for working with {@link SyntaxValue}s.
 */
final class Syntax
{
    private Syntax() {}

    static boolean isSyntax(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxValue);
    }

    static boolean isIdentifier(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxSymbol);
    }



    /**
     * @param context may be null, in which case nothing happens.
     */
    static SyntaxValue applyContext(Evaluator    eval,
                                    SyntaxSymbol context,
                                    SyntaxValue  datum)
        throws FusionException
    {
        if (context != null)
        {
            datum = context.copyWrapsTo(datum);
        }
        return datum;
    }


    /**
     * @param context may be null, in which case no lexical information is
     * applied to converted objects.
     *
     * @return null if something in the datum can't be converted into syntax.
     */
    static SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                          Object         datum,
                                          SyntaxSymbol   context,
                                          SourceLocation location)
        throws FusionException
    {
        if (datum instanceof BaseValue)
        {
            return ((BaseValue) datum).datumToSyntaxMaybe(eval, context,
                                                          location);
        }

        return null;
    }


    /**
     * @param context may be null, in which case no lexical information is
     * applied to converted objects.
     * @param whosCalling The form to name for error messages; may be null.
     *
     * @return not null.
     */
    static SyntaxValue datumToSyntax(Evaluator      eval,
                                     Object         datum,
                                     SyntaxSymbol   context,
                                     SourceLocation location,
                                     String         whosCalling)
        throws FusionException
    {
        SyntaxValue stx = datumToSyntaxMaybe(eval, datum, context, location);
        if (stx == null)
        {
            if (whosCalling == null) whosCalling = "datum_to_syntax";

            throw new ArgumentException(whosCalling,
                                        "syntax object or ionizable data",
                                        -1,
                                        datum);
        }

        return stx;
    }

    /**
     * @param context may be null, in which case no lexical information is
     * applied to converted objects.
     *
     * @return not null.
     */
    static SyntaxValue datumToSyntax(Evaluator      eval,
                                     Object         datum,
                                     SyntaxSymbol   context,
                                     SourceLocation location)
        throws FusionException
    {
        return datumToSyntax(eval, datum, context, location, null);
    }
}
