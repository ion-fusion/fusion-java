// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import dev.ionfusion.fusioncli.repl.ConsoleRepLoop;
import dev.ionfusion.fusioncli.repl.NonConsoleRepLoop;
import dev.ionfusion.fusioncli.repl.RepLoop;
import java.io.PrintWriter;

/**
 * A simple Read-Eval-Print Loop for Fusion.
 */
class Repl
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Enter the interactive Read-Eval-Print Loop.";
    private static final String HELP_USAGE =
        "repl";
    private static final String HELP_BODY =
        "Enters the interactive console. Preceding `require`, `eval`, and `load` commands\n" +
        "share the same namespace.\n" +
        "\n" +
        "This command cannot be used when stdin or stdout have been redirected.";


    Repl()
    {
        super("repl");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    //=========================================================================

    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 0)
        {
            return null;  // Evokes a general usage exception
        }

        globals.collectDocumentation();

        return new Executor(globals);
    }


    private static class Executor
        extends FusionExecutor
    {
        Executor(GlobalOptions globals)
        {
            super(globals);
        }


        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            RepLoop loop;
            if (System.console() != null)
            {
                loop = new ConsoleRepLoop(runtime());
            }
            else
            {
                loop = new NonConsoleRepLoop(runtime(), globals().stdin(), out);
            }

            return loop.run();
        }
    }
}
