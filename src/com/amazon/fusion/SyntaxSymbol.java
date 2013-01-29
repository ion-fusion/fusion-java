// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import com.amazon.ion.IonWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

final class SyntaxSymbol
    extends SyntaxText
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


    /** Initialized during {@link #prepare} */
    private Binding myBinding;

    final SyntaxWraps myWraps;

    private SyntaxSymbol(String value, String[] anns, SourceLocation loc,
                         SyntaxWraps wraps)
    {
        super(value, anns, loc);
        myWraps = wraps;
    }

    static SyntaxSymbol make(String value)
    {
        return new SyntaxSymbol(value, EMPTY_STRING_ARRAY, null, null);
    }

    static SyntaxSymbol make(String value, String[] anns, SourceLocation loc)
    {
        return new SyntaxSymbol(value, anns, loc, null);
    }

    /**
     * @param wraps may be null.
     */
    private SyntaxSymbol copyReplacingWraps(SyntaxWraps wraps)
    {
        // We intentionally don't copy the binding, since the wraps are
        // probably different, so the binding may be different.

        SyntaxSymbol copy =
            new SyntaxSymbol(myText, getAnnotations(), getLocation(), wraps);
        return copy;
    }

    SyntaxSymbol stripImmediateEnvWrap(Environment env)
    {
        if (myWraps == null) return this;
        SyntaxWraps wraps = myWraps.stripImmediateEnvWrap(env);
        if (wraps == myWraps) return this;
        return copyReplacingWraps(wraps);
    }

    @Override
    Type getType()
    {
        return Type.SYMBOL;
    }


    @Override
    SyntaxSymbol addWrap(SyntaxWrap wrap)
    {
        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = SyntaxWraps.make(wrap);
        }
        else
        {
            newWraps = myWraps.addWrap(wrap);
        }
        return copyReplacingWraps(newWraps);
    }

    @Override
    SyntaxSymbol addWraps(SyntaxWraps wraps)
    {
        SyntaxWraps newWraps;
        if (myWraps == null)
        {
            newWraps = wraps;
        }
        else
        {
            newWraps = myWraps.addWraps(wraps);
        }
        return copyReplacingWraps(newWraps);
    }


    @Override
    SyntaxSymbol stripWraps()
    {
        if (myWraps == null) return this;
        return copyReplacingWraps(null);
    }

    /**
     * Adds the wraps on this symbol onto those already on another value.
     * @return syntax matching the source, after adding the wraps from this
     * symbol.
     */
    SyntaxValue copyWrapsTo(SyntaxValue source)
    {
        if (myWraps == null) return source;
        return source.addWraps(myWraps);
    }

    /**
     * @return not null.
     */
    Set<Integer> computeMarks()
    {
        if (myWraps == null) return Collections.emptySet();
        return myWraps.computeMarks();
    }


    /** Not set until {@link #resolve} or {@link #expand}. */
    Binding getBinding()
    {
        return myBinding;
    }

    /**
     * Expand-time binding resolution.
     *
     * @return not null.
     */
    Binding resolve()
    {
        if (myBinding == null)
        {
            if (myWraps == null)
            {
                myBinding = new FreeBinding(stringValue());
            }
            else
            {
                myBinding = myWraps.resolve(this);
            }
        }
        return myBinding;
    }


    /**
     *
     * @return not null, but maybe a {@link FreeBinding}.
     */
    Binding uncachedResolve()
    {
        if (myBinding != null) return myBinding;
        if (myWraps   == null) return new FreeBinding(stringValue());
        return myWraps.resolve(this);
    }


    @Override
    SyntaxValue doExpand(Evaluator eval, Expander ctx, Environment env)
        throws SyntaxFailure
    {
        if (myBinding == null)        // Otherwise we've already been prepared
        {
            if (myText == null)
            {
                throw new SyntaxFailure(null,
                                        "null.symbol is not an expression",
                                        this);
            }

            resolve();
            assert myBinding != null;
            if (myBinding instanceof FreeBinding)
            {
                throw new UnboundIdentifierFailure(myText, this);
            }
        }

        return this;
    }


    Object freeIdentifierEqual(Evaluator eval, SyntaxSymbol that)
    {
        // Use originalBinding() so that a reference to an exported binding
        // from outside the module is equivalent to its internal binding.
        Binding thisBinding = this.uncachedResolve().originalBinding();
        Binding thatBinding = that.uncachedResolve().originalBinding();

        boolean result = thisBinding.equals(thatBinding);
        return eval.newBool(result);
    }


    @Override
    Object quote(Evaluator eval)
    {
        return eval.newSymbol(myText, getAnnotations());
    }


    @Override
    void ionize(Evaluator eval, IonWriter writer)
        throws IOException
    {
        ionizeAnnotations(writer);
        writer.writeSymbol(myText);
    }


    //========================================================================


    @Override
    CompiledForm doCompile(Evaluator eval, Environment env)
        throws FusionException
    {
        assert myBinding != null : "No binding for " + myText;

        return myBinding.compileReference(eval, env);
    }
}
