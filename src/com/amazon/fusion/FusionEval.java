// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionSyntax.isSyntax;
import static com.amazon.fusion.FusionVoid.voidValue;
import static com.amazon.fusion.StandardReader.readSyntax;
import static com.amazon.fusion.Syntax.datumToSyntax;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import java.util.LinkedList;


final class FusionEval
{
    private FusionEval() {}


    /**
     * If the given syntax is a kernel {@code begin} form, return it;
     * otherwise return null.
     */
    private static SyntaxSexp asBeginForm(Evaluator eval, SyntaxValue stx)
        throws FusionException
    {
        if (stx instanceof SyntaxSexp)
        {
            SyntaxSexp sexp = (SyntaxSexp) stx;
            Binding binding = sexp.firstBinding(eval);
            if (binding == eval.getGlobalState().myKernelBeginBinding)
            {
                return sexp;
            }
        }
        return null;
    }


    /**
     * Turns a given form (datum or syntax) into a top-level syntax object,
     * and optionally enriching it.
     *
     * @param whosCalling The form to name for error messages; may be null.
     */
    private static SyntaxValue topLevelStx(Evaluator eval,
                                           Object topLevelForm,
                                           boolean enrichSyntaxObject,
                                           String whosCalling)
        throws FusionException
    {
        SyntaxValue stx;
        if (isSyntax(eval, topLevelForm))
        {
            stx = (SyntaxValue) topLevelForm;
            if (enrichSyntaxObject)
            {
                stx = enrich(eval, stx);
            }
        }
        else
        {
            stx = datumToSyntax(eval, topLevelForm,
                                null, // context
                                null, // location
                                whosCalling);
            stx = enrich(eval, stx);
        }

        return stx;
    }

    /**
     * The default evaluation handler, evaluating the given source
     * within the current namespace.
     *
     * @param topLevelForm is not enriched with lexical information if it is
     *  a syntax object.
     *
     * @see <a href="http://docs.racket-lang.org/reference/eval.html#%28def._%28%28quote._~23~25kernel%29._current-eval%29%29">
         Racket's <code>eval</code></a>
     */
    @SuppressWarnings("javadoc")
    private static Object defaultEval(Evaluator eval, Object topLevelForm)
        throws FusionException
    {
        SyntaxValue stx =
            topLevelStx(eval, topLevelForm, false, "default_eval_handler");
        SourceLocation topLocation = stx.getLocation();

        Namespace ns = eval.findCurrentNamespace();

        Expander expander = new Expander(eval);

        LinkedList<SyntaxValue> forms = new LinkedList<>();
        forms.push(stx);

        try
        {
            Object result = voidValue(eval);
            while (! forms.isEmpty())
            {
                stx = expander.partialExpand(ns, forms.pop());

                SyntaxSexp beginStx = asBeginForm(eval, stx);
                if (beginStx != null)
                {
                    // Splice 'begin' elements into the top-level sequence.
                    for (int i = beginStx.size() - 1; i != 0;  i--)
                    {
                        forms.push(beginStx.get(eval, i));
                    }
                    stx = null;
                }
                else
                {
                    // We've partial-expanded, now full-expand.
                    stx = expander.expand(ns, stx);

                    CompiledForm compiled = eval.compile(ns, stx);
                    stx = null; // Don't hold garbage

                    Object r = eval.eval(ns, compiled); // TODO TAIL

                    // Don't retain the result value longer than needed.
                    if (forms.isEmpty())
                    {
                        result = r;
                    }
                }
            }

            return result;
        }
        catch (FusionException e)
        {
            e.addContext(topLocation);
            throw e;
        }
    }


    /**
     * Placeholder so we can later add current-eval parameter.
     */
    static Object callCurrentEval(Evaluator eval, Object topLevelForm)
        throws FusionException
    {
        return defaultEval(eval, topLevelForm);
    }


    /**
     * Expands, compiles, and evaluates a single top-level form.
     * <p>
     * Equivalent to Racket's {@code eval} (but incomplete at the moment.)
     *
     * @param ns may be null to use {@link Evaluator#findCurrentNamespace()}.
     * @param topLevelForm will be enriched.
     */
    static Object eval(Evaluator eval, Object topLevelForm, Namespace ns)
        throws FusionException
    {
        eval = eval.parameterizeCurrentNamespace(ns);

        if (isSyntax(eval, topLevelForm))
        {
            topLevelForm = enrich(eval, (SyntaxValue) topLevelForm);
        }

        return callCurrentEval(eval, topLevelForm); // TODO TAIL
    }


    /**
     * Like {@link #eval(Evaluator, Object, Namespace)},
     * but does not enrich the source's lexical context.
     *
     * @param ns may be null to use {@link Evaluator#findCurrentNamespace()}.
     */
    static Object evalSyntax(Evaluator eval, SyntaxValue source, Namespace ns)
        throws FusionException
    {
        eval = eval.parameterizeCurrentNamespace(ns);

        return callCurrentEval(eval, source); // TODO TAIL
    }


    /**
     * Enriches a syntax object "in the same way as eval", using the current
     * namespace.
     */
    private static SyntaxValue enrich(Evaluator eval, SyntaxValue topLevelForm)
        throws FusionException
    {
        Namespace ns = eval.findCurrentNamespace();

        // Handle (module ...) such that we don't push bindings into the body.
        if (topLevelForm instanceof SyntaxSexp)
        {
            SyntaxSexp maybeModule = (SyntaxSexp) topLevelForm;
            if (maybeModule.size() > 1 &&
                maybeModule.get(eval, 0) instanceof SyntaxSymbol)
            {
                SyntaxSymbol maybeKeyword = (SyntaxSymbol)
                    maybeModule.get(eval, 0);
                maybeKeyword = (SyntaxSymbol) ns.syntaxIntroduce(maybeKeyword);
                SyntaxSymbol moduleKeyword =
                    eval.getGlobalState().myKernelModuleIdentifier;
                if (maybeKeyword.freeIdentifierEqual(moduleKeyword))
                {
                    SyntaxValue[] children = maybeModule.extract(eval);
                    children[0] = maybeKeyword;
                    return maybeModule.copyReplacingChildren(eval, children);
                }
            }
        }

        topLevelForm = ns.syntaxIntroduce(topLevelForm);
        return topLevelForm;
    }


    //========================================================================
    // Program traversal, for use by tools.


    /**
     * See http://docs.racket-lang.org/tools/drracket_eval.html#%28def._%28%28lib._drracket%2Ftool-lib..rkt%29._drracket~3aeval~3atraverse-program%2Fmultiple%29%29
     *
     * @param receiver is a 1-argument procedure that accepts a syntax object
     * or EOF.
     */
    static void traverseProgram(Evaluator  eval,
                                IonReader  source,
                                SourceName name,
                                Procedure  receiver)
        throws FusionException
    {
        if (source.getType() == null) source.next();

        while (source.getType() != null)
        {
            SyntaxValue sourceExpr = readSyntax(eval, source, name);

            eval.callNonTail(receiver, sourceExpr);

            source.next();
        }

        eval.callNonTail(receiver, FusionIo.eof(eval));
    }


    /**
     * @param receiver is a 1-argument procedure that accepts a syntax object
     * or EOF.
     */
    static void traverseProgram(Evaluator  eval,
                                String     source,
                                SourceName name,
                                Procedure  receiver)
        throws FusionException
    {
        IonSystem system = eval.getGlobalState().myIonSystem;
        IonReader i = system.newReader(source);
        traverseProgram(eval, i, name, receiver);
    }


    /**
     * See http://docs.racket-lang.org/syntax/toplevel.html#%28def._%28%28lib._syntax%2Ftoplevel..rkt%29._expand-syntax-top-level-with-compile-time-evals%29%29
     */
    static SyntaxValue
    expandSyntaxTopLevelWithCompileTimeEvals(Evaluator eval, SyntaxValue stx)
        throws FusionException
    {
        Expander expander = new Expander(eval);
        Namespace ns = eval.findCurrentNamespace();

        stx = expander.partialExpand(ns, stx);

        SyntaxSexp beginStx = asBeginForm(eval, stx);
        if (beginStx != null)
        {
            SyntaxValue[] elts = beginStx.extract(eval);
            for (int i = 1; i < elts.length; i++)
            {
                elts[i] =
                    expandSyntaxTopLevelWithCompileTimeEvals(eval, elts[i]);
            }

            stx = beginStx.copyReplacingChildren(eval, elts);
        }
        else
        {
            stx = expander.expand(ns, stx);

            // TODO this isn't correct
            eval.compile(ns, stx);
        }

        return stx;
    }


    /**
     * See http://docs.racket-lang.org/tools/drracket_eval.html#%28def._%28%28lib._drracket%2Ftool-lib..rkt%29._drracket~3aeval~3aexpand-program%29%29
     * @param receiver is a 1-argument procedure that accepts a syntax object
     * or EOF.
     */
    static void expandProgram(TopLevel        top,
                              String          source,
                              SourceName      name,
                              final Procedure receiver)
        throws FusionException
    {
        final Evaluator eval = StandardTopLevel.toEvaluator(top);

        Procedure r2 = new Procedure1()
        {
            @Override
            public Object doApply(Evaluator eval, Object arg)
                throws FusionException
            {
                if (FusionIo.isEof(eval, arg)) return arg;

                SyntaxValue stx = (SyntaxValue) arg;

                stx = enrich(eval, stx);

                stx = expandSyntaxTopLevelWithCompileTimeEvals(eval, stx);

                return eval.bounceTailCall(receiver, stx);
            }
        };

        traverseProgram(eval, source, name, r2);
    }



    //========================================================================


    static final class ExpandProc
        extends Procedure1
    {
        ExpandProc()
        {
            //    "                                                                               |
            super("Expands a top-level form to core syntax, using the bindings of the current\n" +
                  "namespace.\n" +
                  "\n" +
                  "The `top_level_form` may be a syntax object or another datum.",
                  "top_level_form");
        }

        /**
         * @see FusionEval#eval(Evaluator, Object, Namespace)
         */
        @Override
        Object doApply(Evaluator eval, Object arg0)
            throws FusionException
        {
            SyntaxValue topLevelForm =
                topLevelStx(eval, arg0, true, identify());

            Namespace ns = eval.findCurrentNamespace();
            Expander expander = new Expander(eval);
            topLevelForm = expander.expand(ns, topLevelForm);

            return topLevelForm;
        }
    }


    static final class ExpandOnceProc
        extends Procedure1
    {
        ExpandOnceProc()
        {
            //    "                                                                               |
            super("Expands a top-level form through one step of macro expansion, using the\n" +
                  "bindings of the current namespace.\n" +
                  "\n" +
                  "The `top_level_form` may be a syntax object or another datum.",
                  "top_level_form");
        }

        @Override
        Object doApply(Evaluator eval, Object arg0)
            throws FusionException
        {
            SyntaxValue topLevelForm =
                topLevelStx(eval, arg0, true, identify());

            Namespace ns = eval.findCurrentNamespace();
            Expander expander = new Expander(eval);
            topLevelForm = expander.expandOnce(ns, topLevelForm);

            return topLevelForm;
        }
    }


    static final class EvalProc
        extends Procedure
    {
        EvalProc()
        {
            //    "                                                                               |
            super("Evaluates a `top_level_form` within a `namespace`.  If `namespace` is absent\n" +
                  "then the [`current_namespace`](fusion/namespace.html#current_namespace) parameter is\n" +
                  "used.\n" +
                  "\n" +
                  "The `top_level_form` must be a valid top-level syntactic form with respect to\n" +
                  "the bindings visible in the namespace.  The form is expanded, compiled, and\n" +
                  "evaluated, and its result is returned.  Any side effects made to the namespace\n" +
                  "will be visible to later evaluations.",
                  "top_level_form", "[namespace]");
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(1, 2, args);

            Namespace ns = null;
            if (args.length == 2)
            {
                if (args[1] instanceof Namespace)
                {
                    ns = (Namespace) args[1];
                }
                else
                {
                    throw argFailure("namespace", 1, args);
                }
            }

            return FusionEval.eval(eval, args[0], ns);  // TODO TAIL
        }
    }
}
