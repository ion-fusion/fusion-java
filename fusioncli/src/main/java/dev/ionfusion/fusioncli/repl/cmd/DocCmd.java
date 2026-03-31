// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl.cmd;

import static dev.ionfusion.fusion.FusionSyntax.isIdentifier;
import static dev.ionfusion.fusion._Private_Trampoline.findBindingDoc;

import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.fusioncli.framework.Executor;
import dev.ionfusion.fusioncli.framework.UsageException;
import dev.ionfusion.fusioncli.repl.ReplContext;
import dev.ionfusion.fusioncli.repl.ReplExecutor;
import dev.ionfusion.runtime._private.doc.BindingDoc;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.PrintWriter;

public class DocCmd
    extends Command<ReplContext>
{
    private static final String HELP_ONE_LINER =
        "Print documentation for a given identifier";

    private static final String HELP_USAGE =
        "doc IDENTIFIER";

    private static final String HELP_BODY =
        "Resolves the given identifier in the current namespace and prints any associated\n" +
        "documentation.";


    public DocCmd()
    {
        super("doc");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }

    @Override
    public Executor makeExecutor(ReplContext replContext, String[] args)
        throws UsageException
    {
        assert args.length < 2;
        if (args.length == 0)
        {
            throw usage("Expected an identifier");
        }

        return new DocExecutor(replContext, args[0]);
    }


    private class DocExecutor
        extends ReplExecutor
    {
        private final String myArg;

        private DocExecutor(ReplContext replContext, String arg)
        {
            super(replContext);
            myArg = arg;
        }

        @Override
        public int execute()
            throws Exception
        {
            Object id = determineIdentifier();
            displayDoc(id);

            return 0;
        }

        private Object determineIdentifier()
            throws Exception
        {
            TopLevel top = context().top();

            // TODO This assumes the normal binding of `quote_syntax`

            Object stx;
            try
            {
                stx = top.eval("(quote_syntax " + myArg + ")");
            }
            catch (FusionException e)
            {
                throw usage("Expected an identifier");
            }

            if (! isIdentifier(top, stx))
            {
                throw usage("Expected an identifier");
            }

            return stx;
        }

        private void displayDoc(Object id)
        {
            PrintWriter out = context().out();

            BindingDoc doc = findBindingDoc(context().top(), id);
            if (doc == null)
            {
                out.println("No documentation available.");
                return;
            }

            if (doc.getKind() != null)
            {
                out.append("[");
                // Using enum toString() allows display name to be changed
                out.append(doc.getKind().toString());
                out.append("]  ");
            }
            if (doc.getUsage() != null)
            {
                out.append(doc.getUsage());
            }

            if (doc.getBody() != null)
            {
                out.append('\n');
                out.append(doc.getBody());
                out.append('\n');
            }
        }
    }
}
