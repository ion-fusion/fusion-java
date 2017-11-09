// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.ModuleNamespace.CompiledImportedVariableReference;
import com.amazon.fusion.Namespace.CompiledTopVariableReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * This is ugly, ugly, ugly.
 */
public final class _Private_HelpForm
    extends SyntacticForm
{
    private static final class HelpDocument
        extends BaseValue
    {
        private final Object[] myArgs;

        private HelpDocument(Object[] args)
        {
            myArgs = args;
        }

        @Override
        public void write(Evaluator eval, Appendable out)
            throws IOException
        {
            for (Object arg : myArgs)
            {
                BindingDoc doc = null;
                if (arg instanceof BindingDoc)
                {
                    doc = (BindingDoc) arg;
                }
                else if (arg instanceof BaseValue)
                {

                    doc = ((BaseValue) arg).document();
                }

                if (doc == null)
                {
                    out.append("\nNo documentation available.\n");
                }
                else
                {
                    if (doc.getKind() != null)
                    {
                        out.append("\n[");
                        // Using enum toString() allows display name to be changed
                        out.append(doc.getKind().toString());
                        out.append("]  ");
                    }
                    if (doc.getUsage() != null)
                    {
                        out.append(doc.getUsage());
                    }
                    out.append('\n');

                    if (doc.getBody() != null)
                    {
                        out.append('\n');
                        out.append(doc.getBody());
                        out.append('\n');
                    }
                }
            }
        }
    }


    public _Private_HelpForm()
    {
        //    "                                                                               |
        super("ident ...",
              "Prints documentation for the given bindings, if available.");
    }

    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);
        int arity = stx.size();

        SyntaxValue[] children = stx.extract(eval);

        // Expand (help) into (help help)
        if (arity == 1)
        {
            children = Arrays.copyOf(children, 2);
            children[1] = children[0];
        }

        // Just make sure we've got a list of identifiers
        for (int i = 1; i < arity; i++)
        {
            SyntaxSymbol identifier = check.requiredIdentifier(i);
            children[i] = expander.expandExpression(env, identifier);
        }

        return stx.copyReplacingChildren(eval, children);
    }


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        CompiledForm[] children = comp.compileExpressions(env, stx, 1);
        return new CompiledHelp(children);
    }

    private static final class CompiledHelp
        implements CompiledForm
    {
        private final CompiledForm[] myChildren;

        private CompiledHelp(CompiledForm[] children)
        {
            myChildren = children;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            ArrayList<Object> docs = new ArrayList<Object>();

            for (CompiledForm form : myChildren)
            {
                BindingDoc doc = null;
                if (form instanceof CompiledImportedVariableReference)
                {
                    CompiledImportedVariableReference ref =
                        (CompiledImportedVariableReference) form;
                    NamespaceStore ns = store.namespace();
                    ModuleStore module =
                        ns.lookupRequiredModule(ref.myModuleAddress);
                    doc = module.document(ref.myBindingAddress);
                }
                else if (form instanceof CompiledTopVariableReference)
                {
                    CompiledTopVariableReference ref =
                        (CompiledTopVariableReference) form;
                    Namespace ns = (Namespace) store.namespace();
                    doc = ns.document(ref.myAddress);
                }

                if (doc != null)
                {
                    docs.add(doc);
                }
                else
                {
                    // Couldn't get docs from the binding, lets evaluate it.
                    Object result = eval.eval(store, form);
                    docs.add(result);
                }
            }

            // TODO write directly to current_output_port or somesuch.
            return new HelpDocument(docs.toArray());
        }
    }
}
