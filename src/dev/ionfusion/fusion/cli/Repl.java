// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import dev.ionfusion.fusion.ExitException;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.TopLevel;
import com.amazon.ion.IonException;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

        Console console = System.console();
        if (console == null)
        {
            InputStreamReader isr =
                new InputStreamReader(globals.stdin(), UTF_8);
            BufferedReader in = new BufferedReader(isr);

            OutputStreamWriter osw =
                new OutputStreamWriter(globals.stdout(), UTF_8);
            PrintWriter out = new PrintWriter(osw);

            return new Executor(globals, in, out);
        }

        return new Executor(globals, console);
    }


    private static class Executor
        extends FusionExecutor
    {
        private       TopLevel       myTopLevel;
        private final Console        myConsole;
        private final BufferedReader myIn;
        private final PrintWriter    myOut;


        Executor(GlobalOptions globals, Console console)
        {
            super(globals);

            myConsole = console;
            myIn      = null;
            myOut     = console.writer();
        }

        Executor(GlobalOptions globals, BufferedReader in, PrintWriter out)
        {
            super(globals);

            myConsole = null;
            myIn      = in;
            myOut     = out;
        }


        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            try
            {
                // Bootstrap the runtime before printing the welcome banner, so
                // that we don't do that when there's usage problems.
                myTopLevel = runtime().getDefaultTopLevel();
                myTopLevel.requireModule("/fusion/private/repl");

                welcome();

                while (rep())
                {
                    // loop!
                }
            }
            finally
            {
                myOut.flush();
            }

            return 0;
        }


        private void welcome()
        {
            red("\nWelcome to Fusion!\n\n");
            myOut.println("Type...");
            myOut.println("  (exit)            to exit");
            myOut.println("  (help SOMETHING)  to see documentation; try '(help help)'!\n");
        }


        private boolean rep()
            throws IOException
        {
            blue("$");
            String line = read();

            if (line == null)
            {
                // Print a newline otherwise the user's shell prompt will be on
                // the same line, and that's ugly.
                myOut.println();
                return false;
            }

            try
            {
                Object result = myTopLevel.eval(line);
                writeResults(myTopLevel, result, myOut);
            }
            catch (ExitException e)
            {
                blue("Goodbye!\n");
                myOut.flush();
                return false;
            }
            catch (FusionException | IonException e)
            {
                red(e.getMessage());
                myOut.println();
            }

            return true;
        }

        private String read()
            throws IOException
        {
            if (myConsole != null)
            {
                return myConsole.readLine(" ");
            }
            else
            {
                myOut.flush();
                return myIn.readLine();
            }
        }

        private void blue(String text)
        {
            myOut.print("\033[1;34m");
            myOut.print(text);
            myOut.print("\033[m");
        }

        private void red(String text)
        {
            myOut.print("\033[1;31m");
            myOut.print(text);
            myOut.print("\033[m");
        }
    }
}
