// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.isEof;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSyntax.isSyntax;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.GlobalState.MODULE;
import static dev.ionfusion.fusion.StandardReader.readSyntax;
import static dev.ionfusion.fusion.Syntax.datumToSyntax;

import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import dev.ionfusion.runtime.embed.TopLevel;
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
            Binding binding = sexp.firstTargetBinding(eval);
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
                    Object body = unsafePairTail(eval, beginStx.unwrap(eval));
                    pushSexpElements(eval, forms, body);
                    stx = null;
                }
                else
                {
                    // We've partial-expanded, now full-expand.
                    stx = expander.expand(ns, stx);

                    CompiledForm compiled = eval.compile(ns, stx);
                    stx = null; // Don't hold garbage

                    Object r = eval.eval(ns, compiled);

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
     * Push the sexp elements on the stack, in reverse order.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void pushSexpElements(Evaluator eval, LinkedList stack, Object sexp)
        throws FusionException
    {
        if (isPair(eval, sexp))
        {
            pushSexpElements(eval, stack, unsafePairTail(eval, sexp));
            stack.push(unsafePairHead(eval, sexp));
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

        return callCurrentEval(eval, topLevelForm);
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

        return callCurrentEval(eval, source);
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
            SyntaxSymbol maybeKeyword = maybeModule.firstIdentifier(eval);
            if (maybeKeyword != null)
            {
                maybeKeyword = (SyntaxSymbol) ns.syntaxIntroduce(maybeKeyword);
                SyntaxSymbol moduleKeyword =
                    eval.getGlobalState().kernelBoundIdentifier(MODULE);
                if (maybeKeyword.freeIdentifierEqual(moduleKeyword))
                {
                    // Stash the resolved identifier back in the sexp.
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
     * Parses the top-level syntax forms in Fusion source code, passing each
     * resulting syntax object to a procedure.
     * After all top-level forms are processed, the {@code receiver} is invoked
     * one more time with {@linkplain FusionIo#eof(Evaluator) the canonical EOF
     * value}.
     * <p>
     * See http://docs.racket-lang.org/tools/drracket_eval.html#%28def._%28%28lib._drracket%2Ftool-lib..rkt%29._drracket~3aeval~3atraverse-program%2Fmultiple%29%29
     * </p>
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
        IonReader i = eval.getIonReaderBuilder().build(source);
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
        TopLevelNamespace ns = (TopLevelNamespace) eval.findCurrentNamespace();

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
            evalCompileTimePartOfTopLevel(eval, ns, stx);
        }

        return stx;
    }


    /**
     * Parses and expands the top-level syntax forms in Fusion source code,
     * passing each resulting syntax object to a procedure.
     * After all top-level forms are processed, the {@code receiver} is invoked
     * one more time with {@linkplain FusionIo#eof(Evaluator) the canonical EOF
     * value}.
     * <p>
     * See http://docs.racket-lang.org/tools/drracket_eval.html#%28def._%28%28lib._drracket%2Ftool-lib..rkt%29._drracket~3aeval~3aexpand-program%29%29
     * </p>
     * @param receiver is a 1-argument procedure that accepts a fully-expanded
     *   syntax object or EOF.
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
                if (! isEof(eval, arg))
                {
                    SyntaxValue stx = (SyntaxValue) arg;

                    stx = enrich(eval, stx);

                    arg = expandSyntaxTopLevelWithCompileTimeEvals(eval, stx);
                }

                return eval.bounceTailCall(receiver, arg);
            }
        };

        traverseProgram(eval, source, name, r2);
    }


    /**
     * Evaluates the expansion-time code of a top-level form.
     * This generally means that top-level bindings are created as a result of
     * require and define forms.  Additionally, module declarations are
     * compiled and registered, but not visited or instantiated.
     *
     * @param topStx must be a fully-expanded top-level form, excluding
     * {@code begin}.
     *
     * @see <a href="http://docs.racket-lang.org/syntax/toplevel.html#%28def._%28%28lib._syntax%2Ftoplevel..rkt%29._expand-syntax-top-level-with-compile-time-evals%29%29">
     *   eval-compile-time-part-of-top-level</a>
     */
    private static void evalCompileTimePartOfTopLevel(Evaluator eval,
                                                      TopLevelNamespace topNs,
                                                      SyntaxValue topStx)
        throws FusionException
    {
        eval.evalCompileTimePart(topNs, topStx);
    }


    //========================================================================


    static final class ExpandProc
        extends Procedure1
    {
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
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(eval, 1, 2, args);

            Namespace ns = null;
            if (args.length == 2)
            {
                if (args[1] instanceof Namespace)
                {
                    ns = (Namespace) args[1];
                }
                else
                {
                    throw argError(eval, "namespace", 1, args);
                }
            }

            return FusionEval.eval(eval, args[0], ns);  // TODO TAIL
        }
    }
}
