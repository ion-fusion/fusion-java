// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionList.immutableList;
import static com.amazon.fusion.FusionList.unsafeListElement;

/**
 * Base class for quasiquote and quasisyntax.
 */
abstract class QuasiBaseForm
    extends SyntacticForm
{
    /** Binding for quasiquote/quasisyntax */
    private final Binding myQBinding;

    /** Binding for unquote/unsyntax */
    private final Binding myUBinding;


    QuasiBaseForm(Object qIdentifier, Object uIdentifier)
    {
        super("template", "...");

        SyntaxSymbol id = (SyntaxSymbol) qIdentifier;
        myQBinding = id.resolve();

        id = (SyntaxSymbol) uIdentifier;
        myUBinding = id.resolve();
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        if (stx.size() != 2)
        {
            throw new SyntaxException(getInferredName(),
                                      "a single template required",
                                      stx);
        }

        SyntaxValue subform = stx.get(eval, 1);
        subform = expand(expander, env, subform, 0);

        stx = SyntaxSexp.make(expander, stx.getLocation(),
                              stx.get(eval, 0), subform);
        return stx;
    }

    private SyntaxValue expand(Expander expander, Environment env,
                               SyntaxValue stx, int depth)
        throws FusionException
    {
        // TODO FUSION-225 handle unquote/unsyntax inside structs
        if (stx instanceof SyntaxSexp)
        {
            return expand(expander, env, (SyntaxSexp) stx, depth);
        }
        else if (stx instanceof SyntaxList)
        {
            return expand(expander, env, (SyntaxList) stx, depth);
        }
        else
        {
            return stx;
        }
    }

    private SyntaxValue expand(Expander expander, Environment env,
                               SyntaxSexp stx, int depth)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        int size = stx.size();
        if (size == 0) return stx;

        SyntaxValue[] children = stx.extract(eval);

        Binding binding = stx.firstBindingMaybe(eval);
        if (myUBinding == binding)
        {
            check(eval, stx).arityExact(2);

            if (depth < 1)
            {
                SyntaxValue subform = children[1];
                children[1] = expander.expandExpression(env, subform);

                // TODO accept annotations on unquote/unsyntax form?
                if (stx.annotationsAsJavaStrings().length != 0)
                {
                    String message =
                        "Annotations not accepted on this form";
                    throw check(eval, stx).failure(message);
                }

                return SyntaxSexp.make(eval, stx.getLocation(), children);
            }

            depth--;
        }
        else if (myQBinding == binding)
        {
            check(eval, stx).arityExact(2);
            depth++;
        }

        boolean same = true;
        for (int i = 0; i < size; i++)
        {
            SyntaxValue subform = stx.get(eval, i);
            SyntaxValue expanded = expand(expander, env, subform, depth);
            same &= (subform == expanded);
            children[i] = expanded;
        }

        if (same) return stx;

        return SyntaxSexp.make(eval,
                               stx.getLocation(),
                               stx.annotationsAsJavaStrings(),
                               children);
    }


    private SyntaxValue expand(Expander expander, Environment env,
                               SyntaxList stx, int depth)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        int size = stx.size();
        if (size == 0) return stx;

        Object list = stx.unwrap(eval);

        boolean same = true;
        Object[] children = new Object[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue subform = (SyntaxValue) unsafeListElement(eval, list, i);
            SyntaxValue expanded = expand(expander, env, subform, depth);
            same &= (subform == expanded);
            children[i] = expanded;
        }

        if (same) return stx;

        list = immutableList(eval, stx.annotationsAsJavaStrings(), children);
        return SyntaxList.make(eval, stx.getLocation(), list);
    }


    //========================================================================


    abstract CompiledConstant constant(Evaluator   eval,
                                       SyntaxValue quotedStx)
        throws FusionException;


    abstract CompiledForm unquote(Evaluator    eval,
                                  SyntaxValue  unquotedStx,
                                  CompiledForm unquotedForm)
        throws FusionException;


    abstract CompiledForm quasiSexp(Evaluator      eval,
                                    SyntaxSexp     originalStx,
                                    CompiledForm[] children)
        throws FusionException;


    abstract CompiledForm quasiList(Evaluator      eval,
                                    SyntaxList     originalStx,
                                    CompiledForm[] children)
        throws FusionException;


    //========================================================================


    @Override
    CompiledForm compile(Evaluator eval, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        SyntaxValue node = stx.get(eval, 1);
        return compile(eval, env, node, 0);
    }


    private CompiledForm compile(Evaluator eval, Environment env,
                                 SyntaxValue stx, int depth)
        throws FusionException
    {
        // TODO FUSION-225 handle unquote/unsyntax inside structs
        if (stx instanceof SyntaxSexp)
        {
            return compile(eval, env, (SyntaxSexp) stx, depth);
        }
        else if (stx instanceof SyntaxList)
        {
            return compile(eval, env, (SyntaxList) stx, depth);
        }
        else
        {
            return constant(eval, stx);
        }
    }


    private CompiledForm compile(Evaluator eval, Environment env,
                                 SyntaxSexp stx, int depth)
        throws FusionException
    {
        int size = stx.size();
        if (size == 0) return constant(eval, stx);

        // Look for an (unquote ...) or (unsyntax ...) form
        if (size == 2)
        {
            Binding binding = stx.firstBindingMaybe(eval);
            if (myUBinding == binding)
            {
                if (depth == 0)
                {
                    assert stx.annotationsAsJavaStrings().length == 0;
                    SyntaxValue unquotedSyntax = stx.get(eval, 1);
                    CompiledForm unquotedForm =
                        eval.compile(env, unquotedSyntax);

                    return unquote(eval, unquotedSyntax, unquotedForm);
                }
                depth--;
            }
            else if (myQBinding == binding)
            {
                depth++;
            }
        }

        boolean same = true;
        CompiledForm[] children = new CompiledForm[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue orig = stx.get(eval, i);
            children[i] = compile(eval, env, orig, depth);
            same &= (children[i] instanceof CompiledConstant);
        }

        if (same)
        {
            // There's no unquote within the children, so use the original.
            return constant(eval, stx);
        }

        return quasiSexp(eval, stx, children);
    }


    private CompiledForm compile(Evaluator eval, Environment env,
                                 SyntaxList stx, int depth)
        throws FusionException
    {
        int size = stx.size();
        if (size == 0) return constant(eval, stx);

        boolean same = true;
        CompiledForm[] children = new CompiledForm[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue orig = stx.get(eval, i);
            children[i] = compile(eval, env, orig, depth);
            same &= (children[i] instanceof CompiledConstant);
        }

        if (same)
        {
            // There's no unquote within the children, so use the original.
            return constant(eval, stx);
        }

        return quasiList(eval, stx, children);
    }
}
