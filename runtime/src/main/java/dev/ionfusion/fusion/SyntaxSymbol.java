// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionSyntax.checkIdentifierArg;
import static dev.ionfusion.fusion.SyntaxException.makeSyntaxError;
import static dev.ionfusion.fusion.UnboundIdentifierException.makeUnboundError;

import dev.ionfusion.commons.resources.ResourcePosition;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.base.FusionException;
import java.util.Set;

final class SyntaxSymbol
    extends SimpleSyntaxValue<BaseSymbol>
{
    /** A zero-length array of {@link SyntaxSymbol}. */
    static final SyntaxSymbol[] EMPTY_ARRAY = new SyntaxSymbol[0];


    /** Initialized during {@link #doExpand} */
    private BoundIdentifier myBoundId;

    /**
     * @param wraps can be null.
     * @param pos can be null.
     * @param properties must not be null.
     * @param datum must not be null.
     */
    private SyntaxSymbol(SyntaxWraps    wraps,
                         ResourcePosition pos,
                         Object[]       properties,
                         BaseSymbol     datum)
    {
        super(wraps, pos, properties, datum);
    }



    /**
     * @param pos can be null.
     * @param symbol must not be null.
     */
    static SyntaxSymbol makeOriginal(ResourcePosition pos, BaseSymbol symbol)
    {
        return new SyntaxSymbol(SyntaxWraps.EMPTY, pos, ORIGINAL_STX_PROPS, symbol);
    }

    /**
     * @param pos can be null.
     * @param symbol must not be null.
     */
    static SyntaxSymbol make(ResourcePosition pos, BaseSymbol symbol)
    {
        return new SyntaxSymbol(SyntaxWraps.EMPTY, pos, EMPTY_OBJECT_ARRAY, symbol);
    }


    /**
     * @param value can be null.
     */
    static SyntaxSymbol make(Evaluator eval, String value)
    {
        BaseSymbol datum = makeSymbol(eval, value);
        return new SyntaxSymbol(SyntaxWraps.EMPTY, null, EMPTY_OBJECT_ARRAY, datum);
    }


    //========================================================================


    @Override
    SyntaxSymbol copyReplacing(SyntaxWraps wraps, Object[] properties)
    {
        SyntaxSymbol id = new SyntaxSymbol(wraps, getPosition(), properties, getName());
        if (getWraps() == wraps)
        {
            // Avoid re-resolving when the lexical context is the same.
            id.myBoundId = myBoundId;
        }
        return id;
    }


    SyntaxSymbol copyReplacingBinding(Binding binding)
    {
        SyntaxSymbol copy =
            new SyntaxSymbol(getWraps(), getPosition(), getProperties(), getName());
        copy.myBoundId = uncachedResolveBoundIdentifier().copyReplacingBinding(binding);
        return copy;
    }


    //========================================================================

    /**
     * @return not null, but potentially {@code null.symbol}.
     */
    BaseSymbol getName()
    {
        return getContent();
    }

    String stringValue()
    {
        return getContent().stringValue();
    }


    /**
     * Resolves this identifier to a {@link BoundIdentifier}, but doesn't cache
     * the result if it has not been previously resolved.
     *
     * @return not null.
     */
    BoundIdentifier uncachedResolveBoundIdentifier()
    {
        if (myBoundId != null) return myBoundId;

        return myWraps.resolveBoundIdentifier(getName());
    }

    /**
     * Resolves this identifier to a {@link BoundIdentifier}, permanently
     * caching the result.
     *
     * @return not null.
     */
    BoundIdentifier resolveBoundIdentifier()
    {
        if (myBoundId == null)
        {
            myBoundId = uncachedResolveBoundIdentifier();
        }
        return myBoundId;
    }


    /**
     * Expand-time binding resolution.
     * As a precondition, this symbol's text must be non-empty.
     * As a postcondition, {@link #myBoundId} is not null.
     *
     * @return not null.
     */
    Binding resolve()
    {
        return resolveBoundIdentifier().getBinding();
    }


    /**
     * Resolves this identifier, but doesn't cache the result if it has not
     * been previously resolved.
     *
     * @return not null, but maybe a {@link FreeBinding}.
     */
    Binding uncachedResolve()
    {
        return uncachedResolveBoundIdentifier().getBinding();
    }


    /**
     * Resolves this identifier, but doesn't cache the result if it has not
     * been previously resolved.
     *
     * @return null is equivalent to a {@link FreeBinding}, and either may be
     * returned.
     */
    Binding uncachedResolveMaybe()
    {
        if (myBoundId != null) return myBoundId.getBinding();

        return myWraps.resolveMaybe(getName());
    }


    /**
     * Copies this identifier, caching a top-resolved binding.
     * @return not null.
     */
    SyntaxSymbol copyAndResolveTop()
    {
        Binding b = myWraps.resolveTopMaybe(getName());
        if (b == null)
        {
            b = new FreeBinding(getName());
        }

        return copyReplacingBinding(b);
    }


    /**
     * Resolves this identifier, then checks if it is bound to a {@link SyntacticForm}
     * in the given environment.
     *
     * @return null if this identifier is not bound to a syntactic form.
     */
    SyntacticForm resolveSyntaxMaybe(Environment env)
    {
        Binding binding = resolve();
        Object resolved = env.namespace().lookup(binding);
        if (resolved instanceof SyntacticForm)
        {
            return (SyntacticForm) resolved;
        }
        return null;
    }


    @Override
    SyntaxValue<?> doExpand(Expander expander, Environment env)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();

        String text = stringValue();
        if (text == null)
        {
            String message =
                "`null.symbol` is not a valid expression; use `(quote null.symbol)` instead.";
            throw makeSyntaxError(eval, null, message, this);
        }

        if (text.isEmpty())
        {
            String message =
                "The empty symbol is not a valid expression; use `(quote '')` instead.";
            throw makeSyntaxError(eval, null, message, this);
        }

        // TODO #72 identifier macros
        if (resolveSyntaxMaybe(env) != null)
        {
            String message = "Invalid use of syntax form as identifier expression.";
            throw makeSyntaxError(eval, null, message, this);
        }

        Binding b = resolve();
        if (b instanceof FreeBinding)
        {
            BaseSymbol topSym = makeSymbol(eval, "#%top");
            SyntaxSymbol top =
                new SyntaxSymbol(myWraps,
                                 /*location*/ null,
                                 /*properties*/ EMPTY_OBJECT_ARRAY,
                                 topSym);
            if (top.resolve() instanceof FreeBinding)
            {
                throw makeUnboundError(this);
            }

            assert ! FusionValue.isAnnotated(eval, getContent());
            SyntaxSexp topExpr = SyntaxSexp.make(eval, top, this);

            // TODO #71 Eliminate this tail-call.
            return expander.expandExpression(env, topExpr);
        }

        return this;
    }


    boolean boundIdentifierEqual(SyntaxSymbol that)
    {
        BoundIdentifier thisId = this.uncachedResolveBoundIdentifier();
        BoundIdentifier thatId = that.uncachedResolveBoundIdentifier();
        return thisId.equals(thatId);
    }

    boolean freeIdentifierEqual(SyntaxSymbol that)
    {
        Binding thisBinding = this.uncachedResolve();
        Binding thatBinding = that.uncachedResolve();
        return thisBinding.sameTarget(thatBinding);
    }


    /**
     * Verifies that a set of identifiers are unique with respect to
     * {@link #boundIdentifierEqual}.
     *
     * @param identifiers must not be null.
     * @param formForErrors the syntax form to be implicated in error messages.
     *
     * @throws SyntaxException if a duplicate is found.
     */
    static void ensureUniqueIdentifiers(Evaluator      eval,
                                        SyntaxSymbol[] identifiers,
                                        SyntaxValue<?> formForErrors)
        throws SyntaxException
    {
        if (identifiers.length <= 1) return;

        // TODO Avoid a hashmap when count==2, do a simple comparison.
        BoundIdMap<SyntaxSymbol> ids = new BoundIdMap<>();
        for (SyntaxSymbol id : identifiers)
        {
            SyntaxSymbol dupe = ids.put(id, id);
            if (dupe != null)
            {
                String message =
                    "duplicate binding identifier: " +
                        printQuotedSymbol(id.stringValue());

                SyntaxException ex = makeSyntaxError(eval, null, message, id);
                ex.addContext(formForErrors.getPosition());
                throw ex;
            }
        }
    }


    /**
     * Give a debugging representation: the symbol name and all its marks.
     * For example, {@code "symbol_name#26#12"}.
     */
    String debugString()
    {
        String base = getName().toString();
        Set<MarkWrap> marks = myWraps.computeMarks();
        if (! marks.isEmpty())
        {
            StringBuilder buf = new StringBuilder(base);
            for (MarkWrap mark : marks)
            {
                buf.append('#');
                buf.append(mark.getMark());
            }
            base = buf.toString();
        }
        return base;
    }


    //========================================================================
    // Procedures


    static final class BoundIdentifierEqualProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object arg1, Object arg2)
            throws FusionException
        {
            SyntaxSymbol id1 = checkIdentifierArg(eval, this, "identifier", 0, arg1, arg2);
            SyntaxSymbol id2 = checkIdentifierArg(eval, this, "identifier", 1, arg1, arg2);

            return makeBool(eval, id1.boundIdentifierEqual(id2));
        }
    }


    static final class FreeIdentifierEqualProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object arg1, Object arg2)
            throws FusionException
        {
            SyntaxSymbol id1 = checkIdentifierArg(eval, this, "identifier", 0, arg1, arg2);
            SyntaxSymbol id2 = checkIdentifierArg(eval, this, "identifier", 1, arg1, arg2);

            return makeBool(eval, id1.freeIdentifierEqual(id2));
        }
    }
}
