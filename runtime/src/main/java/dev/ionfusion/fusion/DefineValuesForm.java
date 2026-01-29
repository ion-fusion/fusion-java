// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.isSexp;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.GlobalState.DEFINE_VALUES;

/**
 * Implementation of the fundamental definition syntax form.
 *
 * @see <a href="https://docs.racket-lang.org/reference/define.html#%28form._%28%28quote._~23~25kernel%29._define-values%29%29">Racket
 * Reference</a>
 */
final class DefineValuesForm
    extends SyntacticForm
{
    /**
     * Predefines the identifiers at module level. As with all binding forms,
     * we modify the original identifier so that its
     * {@link SyntaxSymbol#getBinding()} returns the new binding.
     * This is a hack to communicate that information between the "expand" and
     * "compile" phases, since the compiler doesn't have the same environment
     * and cannot resolve bindings.
     *
     * @return the {@code define} form, with the bound identifiers updated to
     * have the new {@link Binding}.
     */
    static SyntaxSexp predefine(Evaluator eval,
                                ModuleNamespace ns,
                                SyntaxSexp defineStx,
                                SyntaxValue formForErrors)
        throws FusionException
    {
        SyntaxChecker check = new SyntaxChecker(eval, DEFINE_VALUES, defineStx);
        check.arityAtLeast(3);

        SyntaxValue[] children = defineStx.extract(eval);

        SyntaxValue first = children[1];
        if (isSexp(eval, first.unwrap(eval)))
        {
            SyntaxSymbol[] ids = checkIds(eval, check);
            for (int i = 0; i < ids.length; i++)
            {
                ids[i] = ns.predefine(ids[i], formForErrors);
            }

            SyntaxSexp idSexp = (SyntaxSexp) first;
            children[1] = idSexp.copyReplacingChildren(eval, ids);
        }
        else
        {
            // Bad syntax; report it later during expansion.
            return defineStx;
        }

        return defineStx.copyReplacingChildren(eval, children);
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(expander, stx);
        if (! (expander.isTopLevelContext() || expander.isModuleContext()))
        {
            throw check.failure("Definitions must be at top-level or module level");
        }

        int arity = check.arityAtLeast(3); // (define_values ids docs? values)

        SyntaxSymbol[] boundIds = checkIds(eval, check);

        // https://docs.racket-lang.org/reference/define.html says:
        //   At the top level, the top-level binding for id is created after
        //   evaluating expr, if it does not exist already, and the top-level
        //   mapping of id (in the namespace linked with the compiled
        //   definition) is set to the binding at the same time.
        // This works differently from our predefine(), used at module level.

        SyntaxValue[] children = stx.extract(eval);

        int bodyPos = 2;
        if (arity == 4)
        {
            if (boundIds.length == 1 &&
                    isString(eval, unsafeSyntaxUnwrap(eval, children[2])))
            {
                // Wrap a single doc-string in a list, so compilation is easier.
                Object list = immutableList(eval, new Object[]{children[2]});
                children[2] = SyntaxList.make(eval, null, list);
            }
            else
            {
                SyntaxChecker docs =
                    check.subformSeq("documentation sequence", 2);

                int idCount = boundIds.length;
                if (docs.form().size(eval) != idCount)
                {
                    throw docs.failure("expected " + idCount + " doc strings");
                }

                for (int i = 0; i < idCount; i++)
                {
                    // TODO allow null?
                    docs.requiredString("documentation string", i);
                }
            }

            bodyPos = 3;
        }
        else if (arity > 4)
        {
            throw check.failure("Too many subforms");
        }

        SyntaxValue valueStx = children[bodyPos];
        children[bodyPos] = expander.expandExpression(env, valueStx);

        return stx.copyReplacingChildren(eval, children);
    }


    private static SyntaxSymbol[] checkIds(Evaluator eval, SyntaxChecker formChecker)
        throws FusionException
    {
        SyntaxChecker subformChecker =
            formChecker.subformSexp("sexp of identifiers to bind", 1);

        SyntaxSexp argSexp = (SyntaxSexp) subformChecker.form();
        int size = argSexp.size(eval);
        for (int i = 0; i < size; i++)
        {
            // Prepare the identifiers so they resolve to their own binding.
            subformChecker.requiredIdentifier(i).resolve();
        }

        // Check for duplicate identifiers
        SyntaxSymbol[] ids = argSexp.extract(eval, SyntaxSymbol.class);
        SyntaxSymbol.ensureUniqueIdentifiers(ids, formChecker.form());
        return ids;
    }


    //========================================================================


    @Override
    void evalCompileTimePart(Compiler comp,
                             TopLevelNamespace topNs,
                             SyntaxSexp topStx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();
        Object idSexp = topStx.get(eval, 1).unwrap(eval);
        while (isPair(eval, idSexp))
        {
            SyntaxSymbol id = (SyntaxSymbol) unsafePairHead(eval, idSexp);
            topNs.predefine(id, id);
            idSexp = unsafePairTail(eval, idSexp);
        }
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        return comp.compileDefineValues(env, stx);
    }
}
