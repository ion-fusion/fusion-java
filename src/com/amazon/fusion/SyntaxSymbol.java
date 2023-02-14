// Copyright (c) 2012-2022 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionSymbol.makeSymbol;
import static com.amazon.fusion.FusionSyntax.checkIdentifierArg;
import static com.amazon.fusion.FusionUtils.EMPTY_OBJECT_ARRAY;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.util.Collections;
import java.util.Set;

final class SyntaxSymbol
    extends SyntaxText<SyntaxSymbol>
{
    /** A zero-length array of {@link SyntaxSymbol}. */
    static final SyntaxSymbol[] EMPTY_ARRAY = new SyntaxSymbol[0];

    /** Extract the names from an array of symbols. */
    static String[] toNames(SyntaxSymbol[] symbols)
    {
        if (symbols == null || symbols.length == 0)
        {
            return FusionUtils.EMPTY_STRING_ARRAY;
        }
        else
        {
            String[] names = new String[symbols.length];
            for (int i = 0; i < symbols.length; i++)
            {
                names[i] = symbols[i].stringValue();
            }
            return names;
        }
    }


    /** Initialized during {@link #doExpand} */
    private BoundIdentifier myBoundId;

    /**
     * @param datum must not be null.
     */
    private SyntaxSymbol(Evaluator      eval,
                         SyntaxWraps    wraps,
                         SourceLocation loc,
                         Object[]       properties,
                         BaseSymbol     datum)
    {
        super(wraps, loc, properties, datum);
    }



    static SyntaxSymbol makeOriginal(Evaluator      eval,
                                     SourceLocation loc,
                                     BaseSymbol     symbol)
    {
        return new SyntaxSymbol(eval, null, loc, ORIGINAL_STX_PROPS, symbol);
    }

    static SyntaxSymbol make(Evaluator      eval,
                             SourceLocation loc,
                             BaseSymbol     symbol)
    {
        return new SyntaxSymbol(eval, null, loc, EMPTY_OBJECT_ARRAY, symbol);
    }


    /**
     * @param value may be null.
     */
    static SyntaxSymbol make(Evaluator eval, SyntaxWraps wraps, String value)
    {
        BaseSymbol datum = makeSymbol(eval, value);
        return new SyntaxSymbol(eval, wraps, /*location*/ null,
                                EMPTY_OBJECT_ARRAY, datum);
    }


    /**
     * @param value may be null.
     */
    static SyntaxSymbol make(Evaluator eval, SyntaxWrap wrap, String value)
    {
        return make(eval, SyntaxWraps.make(wrap), value);
    }


    /**
     * @param value may be null.
     */
    static SyntaxSymbol make(Evaluator eval, String value)
    {
        BaseSymbol datum = makeSymbol(eval, value);
        return new SyntaxSymbol(eval, null, null, EMPTY_OBJECT_ARRAY, datum);
    }


    //========================================================================


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    SyntaxSymbol copyReplacingProperties(Object[] properties)
    {
        SyntaxSymbol id = new SyntaxSymbol(null,
                                           myWraps,
                                           getLocation(),
                                           properties,
                                           getName());
        id.myBoundId = myBoundId;
        return id;
    }


    /**
     * @param wraps may be null.
     */
    @Override
    SyntaxSymbol copyReplacingWraps(SyntaxWraps wraps)
    {
        // We intentionally don't copy the binding, since the wraps are
        // probably different, so the binding may be different.

        SyntaxSymbol copy =
            new SyntaxSymbol(null, wraps, getLocation(), getProperties(),
                             getName());
        return copy;
    }


    SyntaxSymbol copyReplacingBinding(Binding binding)
    {
        SyntaxSymbol copy =
            new SyntaxSymbol(null, myWraps, getLocation(), getProperties(),
                             getName());
        copy.myBoundId = uncachedResolveBoundIdentifier().copyReplacingBinding(binding);
        return copy;
    }


    //========================================================================

    BaseSymbol getName()
    {
        return (BaseSymbol) myDatum;
    }


    /**
     * @return not null.
     */
    Set<MarkWrap> computeMarks()
    {
        if (myWraps == null) return Collections.emptySet();
        return myWraps.computeMarks();
    }


    @Override
    boolean hasMarks(Evaluator eval)
    {
        if (myBoundId != null) return myBoundId.hasMarks();
        return super.hasMarks(eval);
    }


    /** Not set until {@link #resolve} or {@link #doExpand}. */
    Binding getBinding()
    {
        return myBoundId.getBinding();
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
        if (myWraps != null)
        {
            return myWraps.resolveBoundIdentifier(getName());
        }
        return new BoundIdentifier(new FreeBinding(getName()),
                                   Collections.<MarkWrap>emptySet());
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
        if (myWraps   == null) return null;
        return myWraps.resolveMaybe(getName());
    }


    /**
     * Copies this identifier, caching a top-resolved binding.
     * @return not null.
     */
    SyntaxSymbol copyAndResolveTop()
    {
        Binding b = null;
        if (myWraps != null)
        {
            b = myWraps.resolveTopMaybe(getName());
        }
        if (b == null)
        {
            b = new FreeBinding(getName());
        }

        return copyReplacingBinding(b);
    }


    /**
     * Checks if this symbol is bound to a {@link SyntacticForm} in the given
     * environment.  If so, cache the binding and return the form.  Otherwise
     * do nothing.
     *
     * @return may be null.
     */
    SyntacticForm resolveSyntaxMaybe(Environment env)
    {
        BoundIdentifier b = uncachedResolveBoundIdentifier();
        Object resolved = env.namespace().lookup(b.getBinding());
        if (resolved instanceof SyntacticForm)
        {
            myBoundId = b;
            return (SyntacticForm) resolved;
        }
        return null;
    }


    @Override
    SyntaxValue doExpand(Expander expander, Environment env)
        throws FusionException
    {
        if (myBoundId == null)        // Otherwise we've already been expanded
        {
            // FIXME Ensure that this validation always happens when necessary,
            //       even if other code calls resolve() before expansion.

            String text = stringValue();
            if (text == null)
            {
                String message =
                    "`null.symbol` is not a valid expression; use `(quote null.symbol)` instead.";
                throw new SyntaxException(null, message, this);
            }

            if (text.length() == 0)
            {
                String message =
                    "The empty symbol is not a valid expression; use `(quote '')` instead.";
                throw new SyntaxException(null, message, this);
            }

            Binding b = resolve();
            if (b instanceof FreeBinding)
            {
                Evaluator eval = expander.getEvaluator();
                BaseSymbol topSym = makeSymbol(eval, "#%top");
                SyntaxSymbol top =
                    new SyntaxSymbol(eval,
                                     myWraps,
                                     /*location*/ null,
                                     /*properties*/ EMPTY_OBJECT_ARRAY,
                                     topSym);
                if (top.resolve() instanceof FreeBinding)
                {
                    throw new UnboundIdentifierException(this);
                }

                assert ! FusionValue.isAnnotated(eval, myDatum);
                SyntaxSexp topExpr = SyntaxSexp.make(eval, top, this);

                // TODO FUSION-207 tail expand
                return expander.expandExpression(env, topExpr);
            }
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
     * Give a debugging representation: the symbol name and all its marks.
     * For example, {@code "symbol_name#26#12"}.
     */
    String debugString()
    {
        String base = toString();
        Set<MarkWrap> marks = this.computeMarks();
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
