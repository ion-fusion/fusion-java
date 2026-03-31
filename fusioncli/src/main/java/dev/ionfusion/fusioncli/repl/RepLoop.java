// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import static dev.ionfusion.fusioncli.FusionExecutor.writeResults;

import com.amazon.ion.IonException;
import dev.ionfusion.fusioncli.framework.CommandSuite;
import dev.ionfusion.fusioncli.repl.cmd.ExitCmd;
import dev.ionfusion.fusioncli.repl.cmd.ReplHelpCmd;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class RepLoop
{
    private   final FusionRuntime  myRuntime;
    private   final TopLevel       myTopLevel;
    protected final PrintWriter    myOut;

    private  final  ReplCli        myCli;

    RepLoop(FusionRuntime runtime, PrintWriter stdout)
        throws FusionException
    {
        myRuntime = runtime;
        myTopLevel = runtime.getDefaultTopLevel();

        myTopLevel.requireModule("/fusion/private/cli/repl");

        myOut = stdout;

        CommandSuite commands = new CommandSuite(new ExitCmd(),
                                                 new ReplHelpCmd());
        ReplContext context = new ReplContext(commands, stdout);
        myCli = new ReplCli(context);
    }


    protected abstract String readLine()
        throws IOException;


    public int run()
        throws IOException
    {
        try
        {
            welcome();

            while (rep())
            {
                myOut.flush();
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
        myOut.println("  ,exit  to exit. ^D should work too.");
        myOut.println("  ,help  for see all REPL commands. Try `,help help`!");
        myOut.println();
    }


    private boolean rep()
        throws IOException
    {
        blue("$");
        String line = readLine();

        if (line == null) // EOF
        {
            // Print a newline otherwise the shell's prompt will be on
            // the same line as our prompt, and that's ugly.
            myOut.println();
            return false;
        }

        line = line.trim();
        if (line.startsWith(","))
        {
            // Drop the leading comma.
            return handleCommand(line.substring(1));
        }

        // Might need more flushing of both Fusion-side and Java-side buffers?

        try
        {
            Object result = myTopLevel.eval(line);
            writeResults(myTopLevel, result, myOut);
        }
        catch (FusionException | IonException e)
        {
            red(e.getMessage());
            myOut.println();
        }

        return true;
    }

    private boolean handleCommand(String lineAfterComma)
    {
        // Split on the first space; don't assume that every command splits on space.
        String[] parts = lineAfterComma.trim().split(" ", 2);

        // Trim any extra whitespace around the single (at this level) argument.
        if (parts.length == 2)
        {
            parts[1] = parts[1].trim();
        }

        try
        {
            int result = myCli.executeCommandLine(parts);
            return (result == 0);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
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
