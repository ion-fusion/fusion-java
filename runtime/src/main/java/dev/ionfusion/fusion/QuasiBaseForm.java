// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.unsafeListElement;

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
        SyntaxSymbol id = (SyntaxSymbol) qIdentifier;
        myQBinding = id.resolve().target();

        id = (SyntaxSymbol) uIdentifier;
        myUBinding = id.resolve().target();
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        check(eval, stx).arityExact(2);

        SyntaxValue subform = stx.get(eval, 1);
        subform = expand(expander, env, subform, 0);

        return stx.copyReplacingChildren(eval, stx.get(eval, 0), subform);
    }

    private SyntaxValue expand(Expander expander, Environment env,
                               SyntaxValue stx, int depth)
        throws FusionException
    {
        // TODO handle unquote/unsyntax inside structs
        //  https://github.com/ion-fusion/fusion-java/issues/62
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

        Binding binding = stx.firstTargetBinding(eval);
        if (myUBinding == binding)
        {
            check(eval, stx).arityExact(2);

            if (depth < 1)
            {
                SyntaxValue subform = children[1];
                children[1] = expander.expandExpression(env, subform);

                // TODO accept annotations on unquote/unsyntax form?
                if (FusionValue.isAnnotated(eval, stx.unwrap(eval)))
                {
                    String message =
                        "Annotations not accepted on this form";
                    throw check(eval, stx).failure(message);
                }

                return stx.copyReplacingChildren(eval, children);
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

        return stx.copyReplacingChildren(eval, children);
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
        SyntaxValue[] children = new SyntaxValue[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue subform = (SyntaxValue) unsafeListElement(eval, list, i);
            SyntaxValue expanded = expand(expander, env, subform, depth);
            same &= (subform == expanded);
            children[i] = expanded;
        }

        if (same) return stx;

        return stx.copyReplacingChildren(eval, children);
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
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        SyntaxValue node = stx.get(eval, 1);
        return compile(comp, env, node, 0);
    }


    private CompiledForm compile(Compiler comp, Environment env,
                                 SyntaxValue stx, int depth)
        throws FusionException
    {
        // TODO handle unquote/unsyntax inside structs
        //  https://github.com/ion-fusion/fusion-java/issues/62
        if (stx instanceof SyntaxSexp)
        {
            return compile(comp, env, (SyntaxSexp) stx, depth);
        }
        else if (stx instanceof SyntaxList)
        {
            return compile(comp, env, (SyntaxList) stx, depth);
        }
        else
        {
            return constant(comp.getEvaluator(), stx);
        }
    }


    private CompiledForm compile(Compiler comp, Environment env,
                                 SyntaxSexp stx, int depth)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        int size = stx.size();
        if (size == 0) return constant(eval, stx);

        // Look for an (unquote ...) or (unsyntax ...) form
        if (size == 2)
        {
            Binding binding = stx.firstTargetBinding(eval);
            if (myUBinding == binding)
            {
                if (depth == 0)
                {
                    assert ! FusionValue.isAnnotated(eval, stx.unwrap(eval));
                    SyntaxValue unquotedSyntax = stx.get(eval, 1);
                    CompiledForm unquotedForm =
                        comp.compileExpression(env, unquotedSyntax);

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
            children[i] = compile(comp, env, orig, depth);
            same &= (children[i] instanceof CompiledConstant);
        }

        if (same)
        {
            // There's no unquote within the children, so use the original.
            return constant(eval, stx);
        }

        return quasiSexp(eval, stx, children);
    }


    private CompiledForm compile(Compiler comp, Environment env,
                                 SyntaxList stx, int depth)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        int size = stx.size();
        if (size == 0) return constant(eval, stx);

        boolean same = true;
        CompiledForm[] children = new CompiledForm[size];
        for (int i = 0; i < size; i++)
        {
            SyntaxValue orig = stx.get(eval, i);
            children[i] = compile(comp, env, orig, depth);
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
