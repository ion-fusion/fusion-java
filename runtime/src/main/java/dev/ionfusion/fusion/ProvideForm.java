// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSymbol.isSymbol;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionSyntax.syntaxTrackOrigin;
import static dev.ionfusion.fusion.Syntax.datumToSyntax;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import java.util.ArrayList;


final class ProvideForm
    extends SyntacticForm
{
    /**
     * Note that we expand `provide` after expanding the rest of the module.
     */
    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp form)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, form);

        if (!expander.isModuleContext())
        {
            throw check.failure("May only appear in a module context");
        }


        Namespace moduleNamespace = (Namespace) env;

        int size = form.size(eval);
        ArrayList<SyntaxValue> expanded = new ArrayList<>(size);
        expanded.add(form.get(eval, 0));

        for (int i = 1; i < size; i++)
        {
            SyntaxValue spec = form.get(eval, i);
            Object datum = spec.unwrap(eval);
            if (isSymbol(eval, datum))
            {
                check.requiredIdentifier("bound identifier", i);

                expandProvideId(moduleNamespace, (SyntaxSymbol) spec,
                                check, expanded);
            }
            else if (isSexp(eval, datum))
            {
                expandProvideSexp(eval, moduleNamespace,
                                  (SyntaxSexp) spec,
                                  check,
                                  expanded);
            }
            else
            {
                throw check.failure("expected provide-spec", spec);
            }
        }

        SyntaxValue[] children = expanded.toArray(SyntaxValue.EMPTY_ARRAY);
        return form.copyReplacingChildren(eval, children);
    }

    private void expandProvideId(Namespace moduleNamespace,
                                 SyntaxSymbol identifier,
                                 SyntaxChecker check,
                                 ArrayList<SyntaxValue> expanded)
        throws FusionException
    {
        Binding b = verifyBoundId(moduleNamespace, identifier, check);

        BaseSymbol freeName = b.getName();
        if (freeName != identifier.getName())
        {
            String message =
                "cannot export binding since symbolic name " +
                printQuotedSymbol(identifier.stringValue()) +
                " differs from resolved name " +
                printQuotedSymbol(freeName.stringValue());
            throw check.failure(message);
        }

        expanded.add(identifier);
    }

    /**
     * Check that an identifier is bound within the module and can be exported.
     */
    private Binding verifyBoundId(Namespace moduleNamespace,
                                  SyntaxSymbol identifier,
                                  SyntaxChecker check)
        throws FusionException
    {
        // TODO id.resolve works when the id has the ns in context
        //  https://github.com/ion-fusion/fusion-java/issues/82
        // It doesn't work when the ns isn't in context because the
        // provided binding was macro-introduced.

        Binding b = identifier.resolve();
        // Binding local = moduleNamespace.localResolve(identifier);
        // localResolve isn't right either since it doesn't find imports

        if (b instanceof FreeBinding)
        {
            String message =
                "cannot export " + printQuotedSymbol(identifier.stringValue()) +
                " since it is neither defined in nor imported into this module.";
            throw check.failure(message, identifier);
        }

        return b;
    }

    private void expandProvideSexp(Evaluator eval,
                                   Namespace moduleNamespace,
                                   SyntaxSexp specForm,
                                   SyntaxChecker provideCheck,
                                   ArrayList<SyntaxValue> expanded)
        throws FusionException
    {
        SyntaxChecker check = new SyntaxChecker(eval, specForm);

        GlobalState globalState = eval.getGlobalState();

        SyntaxSymbol specId = specForm.firstIdentifier(eval);
        Binding b = specForm.firstTargetBinding(eval);
        if (b == globalState.myKernelAllDefinedOutBinding)
        {
            // Expanded form is a sequence of identifiers.

            check.arityExact(1);

            // Add an all_defined form that is ignored during compilation,
            // but is needed for syntax analysis and origin tracking.
            SyntaxSymbol allDefinedSym =
                syntaxTrackOrigin(eval,
                                  SyntaxSymbol.make(specId.getLocation(),
                                                    makeSymbol(eval, "all_defined")),
                                  specForm, specId);
            expanded.add(specForm.copyReplacingChildren(eval, allDefinedSym));

            // Filter by lexical context: we shouldn't export identifiers
            // introduced by macros unless this form was also introduced
            // at the same time.
            for (NsDefinedBinding binding : moduleNamespace.getDefinedBindings())
            {
                // TODO the datum->syntax context should be the sexp
                //  form `(all_defined_out)` not just `all_defined_out` but
                //  we don't currently retain context on SyntaxSexp after
                //  it has been pushed down to children.
                //  https://github.com/ion-fusion/fusion-java/issues/68
                SyntaxSymbol localized = (SyntaxSymbol)
                    datumToSyntax(eval,
                                  binding.getName(),
                                  specId,
                                  null);
                Binding localBinding = moduleNamespace.resolveDefinition(localized);
                if (localBinding != null && binding.sameTarget(localBinding))
                {
                    localized = localized.copyReplacingBinding(binding);
                    expanded.add(localized);
                }

                // TODO handle rename-transformers per Racket
                //   https://github.com/ion-fusion/fusion-java/issues/73
            }
        }
        else if (b == globalState.myKernelRenameOutBinding)
        {
            // Expanded form is a sequence of (rename local exported) triples.

            SyntaxSymbol renameSym =
                syntaxTrackOrigin(eval,
                                  SyntaxSymbol.make(specId.getLocation(),
                                                    makeSymbol(eval, "rename")),
                                  specForm, specId);

            int arity = check.arityAtLeast(1);
            for (int i = 1; i < arity; i++)
            {
                SyntaxChecker rename = check.subformSexp("rename pair", i);
                rename.arityExact(2);

                SyntaxSymbol inner =
                    rename.requiredIdentifier("local identifier", 0);
                SyntaxSymbol outer =
                    rename.requiredIdentifier("exported identifier", 1);

                verifyBoundId(moduleNamespace, inner, check);

                expanded.add(SyntaxSexp.make(eval, renameSym, inner, outer));
            }
        }
        else
        {
            throw provideCheck.failure("invalid provide-spec");
        }
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        throw new IllegalStateException("Provide shouldn't be compiled");
    }


    //========================================================================


    /**
     * This class primarily provides syntax to bind to provide-clauses so that
     * {@link ProvideForm} can compare bindings, not symbolic names.  This is
     * as specified by Racket.
     */
    private abstract static class AbstractProvideClauseForm
        extends SyntacticForm
    {
        @Override
        SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
            throws FusionException
        {
            throw check(expander, stx).failure("must be used inside `provide`");
        }

        @Override
        CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
            throws FusionException
        {
            throw new IllegalStateException("`provide` shouldn't be compiled");
        }
    }


    static final class AllDefinedOutForm
        extends AbstractProvideClauseForm
    {
    }


    static final class RenameOutForm
        extends AbstractProvideClauseForm
    {
    }
}
