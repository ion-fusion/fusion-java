// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionValue.isAnnotated;

/**
 * "Registers" used during macro expansion
 */
final class Expander
{
    enum Context { TOP, MODULE, EXPRESSION }

    private final Evaluator myEval;
    private final Context   myContext;

    Expander(Evaluator eval)
    {
        myEval = eval;
        myContext = Context.TOP;
    }

    private Expander(Evaluator eval, Context ctx)
    {
        myEval = eval;
        myContext = ctx;
    }


    Evaluator getEvaluator()
    {
        return myEval;
    }

    GlobalState getGlobalState()
    {
        return myEval.getGlobalState();
    }

    ModuleInstance getKernel()
    {
        return myEval.findKernel();
    }

    boolean isTopLevelContext()
    {
        return myContext == Context.TOP;
    }

    boolean isModuleContext()
    {
        return myContext == Context.MODULE;
    }

    Expander enterModuleContext()
    {
        assert isTopLevelContext();

        return new Expander(myEval, Context.MODULE);
    }


    /**
     * Expands syntax in the current context.
     *
     * @param stx is not enriched before expansion.
     */
    SyntaxValue expand(Environment env, SyntaxValue stx)
        throws FusionException
    {
        Object datum = stx.unwrap(myEval);
        if (isAnnotated(myEval, datum))
        {
            String message =
                "Annotations not supported in raw syntax. You probably " +
                "want to quote this.";
            throw new SyntaxException(null, message, stx);
        }

        return stx.doExpand(this, env);
    }


    /**
     * Expands a syntax form through one step of macro expansion.
     * <p>
     * This is close to (or perhaps exactly) Racket's {@code expand-once}.
     *
     * @param stx is not enriched before expansion.
     */
    SyntaxValue expandOnce(Environment env, SyntaxValue stx)
        throws FusionException
    {
        // Handle other cases as per Racket spec. In particular:
        // TODO https://github.com/ion-fusion/fusion-java/issues/72 identifier macros
        // TODO https://github.com/ion-fusion/fusion-java/issues/73 rename transformers
        // TODO https://github.com/ion-fusion/fusion-java/issues/74 #%app

        if (! (stx instanceof SyntaxSexp)) return stx;

        SyntaxSexp sexp = (SyntaxSexp) stx;
        if (sexp.hasNoChildren()) return stx;

        SyntaxSymbol maybeMacro = sexp.firstIdentifier(myEval);
        if (maybeMacro == null) return stx;

        SyntacticForm form = maybeMacro.resolveSyntaxMaybe(env);
        if (form instanceof MacroForm)
        {
            // We found a static top-level macro binding. Expand it.
            stx = ((MacroForm)form).expandOnce(this, sexp);
        }

        return stx;
    }


    /**
     * Partially-expands the syntax until the outermost core syntactic form is
     * exposed.
     *
     * @param stx is not enriched before expansion.
     */
    SyntaxValue partialExpand(Environment env, SyntaxValue stx)
        throws FusionException
    {
        while (true)
        {
            SyntaxValue expanded = expandOnce(env, stx);
            if (expanded == stx)
            {
                return stx;
            }
            stx = expanded;
        }
    }


    /**
     * Expands syntax in an expression context.
     * Equivalent to Racket syntax-local-expand-expression.
     */
    SyntaxValue expandExpression(Environment env, SyntaxValue stx)
        throws FusionException
    {
        Expander expander = this;
        if (myContext != Context.EXPRESSION)
        {
            expander = new Expander(myEval, Context.EXPRESSION);
        }

        return expander.expand(env, stx);
    }


    SyntaxValue expand(Environment env, SyntacticForm form, SyntaxSexp stx)
        throws FusionException
    {
        return form.expand(this, env, stx);
    }
}
