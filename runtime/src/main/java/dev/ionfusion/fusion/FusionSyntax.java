// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;


/**
 * Utilities for working with syntax objects.
 */
final class FusionSyntax
{
    private FusionSyntax() {}


    /**
     * Determines whether a Fusion value is a syntax object.
     */
    static boolean isSyntax(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxValue);
    }


    /**
     * Determines whether a Fusion value is an identifier; that is, a syntax
     * object wrapping a symbol.
     */
    static boolean isIdentifier(Evaluator eval, Object value)
    {
        return (value instanceof SyntaxSymbol);
    }


    static Object unsafeSyntaxUnwrap(Evaluator eval, Object stx)
        throws FusionException
    {
        return ((SyntaxValue) stx).unwrap(eval);
    }

    static SourceLocation unsafeSyntaxLocation(Evaluator eval, Object stx)
    {
        return ((SyntaxValue) stx).getLocation();
    }


    /**
     * @param stx must be a syntax object.
     * @param key must not be null.
     * @return void if no value is associated with the key.
     */
    static Object unsafeSyntaxGetProperty(Evaluator eval,
                                          Object    stx,
                                          Object    key)
        throws FusionException
    {
        return ((SyntaxValue) stx).findProperty(eval, key);
    }


    /**
     * Determines whether {@code id1} would bind {@code id2} (or vice versa) if
     * one were in a binding position and the other in a suitable reference
     * position.
     *
     * @param id1
     *   must be an {@linkplain #isIdentifier(Evaluator, Object) identifier}.
     * @param id2
     *   must be an {@linkplain #isIdentifier(Evaluator, Object) identifier}.
     */
    static boolean unsafeBoundIdentifierEqual(Evaluator eval,
                                              Object    id1,
                                              Object    id2)
        throws FusionException
    {
        return ((SyntaxSymbol) id1).boundIdentifierEqual((SyntaxSymbol) id2);
    }


    /**
     * Determines whether two identifiers refer to the same binding.
     *
     * @param id1
     *   must be an {@linkplain #isIdentifier(Evaluator, Object) identifier}.
     * @param id2
     *   must be an {@linkplain #isIdentifier(Evaluator, Object) identifier}.
     */
    static boolean unsafeFreeIdentifierEqual(Evaluator eval,
                                             Object    id1,
                                             Object    id2)
        throws FusionException
    {
        return ((SyntaxSymbol) id1).freeIdentifierEqual((SyntaxSymbol) id2);
    }


    /**
     * Provides the site where an identifier was bound.
     * The result is only valid when the identifier is taked from fully-expanded
     * code.
     * <p>
     * Based on Racket's <a href="http://docs.racket-lang.org/reference/stxcmp.html?q=member#%28def._%28%28quote._~23~25kernel%29._identifier-binding%29%29">{@code identifier-binding}</a>.
     *
     * @param id must be an identifier
     *
     * @return null if the identifier is free (unbound).
     */
    static BindingSite unsafeIdentifierBinding(Evaluator eval, Object id)
        throws FusionException
    {
        return ((SyntaxSymbol) id).uncachedResolve().getBindingSite();
    }


    /**
     * @return void if the origin property isn't set.
     *
     * @deprecated No longer necessary. Used by the Eclipse plugin.
     */
    @Deprecated
    static Object syntaxOrigin(Evaluator eval, SyntaxValue stx)
        throws FusionException
    {
        return stx.findProperty(eval, eval.getGlobalState().myStxPropOrigin);
    }


    @SuppressWarnings("unchecked")
    static <T extends SyntaxValue> T syntaxTrackOrigin(Evaluator    eval,
                                                       T            newStx,
                                                       SyntaxValue  origStx,
                                                       SyntaxSymbol origin)
        throws FusionException
    {
        return (T) newStx.trackOrigin(eval, origStx, origin);
    }


    //========================================================================
    // Predicates

    static boolean unsafeSyntaxIsOriginal(Evaluator eval, Object stx)
        throws FusionException
    {
        return ((SyntaxValue) stx).isOriginal(eval);
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     */
    static SyntaxValue checkSyntaxArg(Evaluator eval,
                                      Procedure who,
                                      String    expectation,
                                      int       argNum,
                                      Object... args)
        throws FusionException
    {
        Object arg = args[argNum];
        if (arg instanceof SyntaxValue)
        {
            return (SyntaxValue) arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * @param expectation must not be null.
     */
    static SyntaxSymbol checkIdentifierArg(Evaluator eval,
                                           Procedure who,
                                           String    expectation,
                                           int       argNum,
                                           Object... args)
        throws FusionException
    {
        Object arg = args[argNum];
        if (arg instanceof SyntaxSymbol)
        {
            return (SyntaxSymbol) arg;
        }

        throw who.argError(eval, expectation, argNum, args);
    }


    //========================================================================
    // Procedures

    static final class IsIdentifierProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean result = isIdentifier(eval, arg);
            return makeBool(eval, result);
        }
    }


    static final class IsSyntaxProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean result = isSyntax(eval, arg);
            return makeBool(eval, result);
        }
    }


    static final class ToDatumProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);
            SyntaxValue stx = checkSyntaxArg(eval, 0, args);
            return stx.syntaxToDatum(eval);
        }
    }


    static final class UnwrapProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);
            SyntaxValue stx = checkSyntaxArg(eval, 0, args);
            return stx.unwrap(eval);
        }
    }


    static final class PropertyProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(2, 3, args);

            SyntaxValue stx =
                FusionSyntax.checkSyntaxArg(eval, this, "syntax object", 0, args);

            if (args.length == 2)
            {
                return stx.findProperty(eval, args[1]);
            }
            else
            {
                return stx.copyWithProperty(eval, args[1], args[2]);
            }
        }
    }


    static final class TrackOriginProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(3, args);

            SyntaxValue newStx =
                FusionSyntax.checkSyntaxArg(eval, this, "syntax object", 0, args);
            SyntaxValue origStx =
                FusionSyntax.checkSyntaxArg(eval, this, "syntax object", 1, args);
            SyntaxSymbol origin =
                checkIdentifierArg(eval, this, "syntax identifier", 2, args);

            return newStx.trackOrigin(eval, origStx, origin);
        }
    }


    static final class IsOriginalProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(1, args);

            SyntaxValue stx =
                FusionSyntax.checkSyntaxArg(eval, this, "syntax object", 0, args);

            boolean o = stx.isOriginal(eval);
            return FusionBool.makeBool(eval, o);
        }
    }
}
