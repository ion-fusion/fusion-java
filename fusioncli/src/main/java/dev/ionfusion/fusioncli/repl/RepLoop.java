// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import static dev.ionfusion.fusioncli.FusionExecutor.writeResults;

import com.amazon.ion.IonException;
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

    RepLoop(FusionRuntime runtime, PrintWriter stdout)
        throws FusionException
    {
        myRuntime = runtime;
        myTopLevel = runtime.getDefaultTopLevel();

        myTopLevel.requireModule("/fusion/private/cli/repl");

        myOut = stdout;
    }


    protected abstract String readLine()
        throws IOException;


    public int run()
        throws FusionException, IOException
    {
        try
        {
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
        myOut.println("  ^D                to exit");
        myOut.println("  (help SOMETHING)  to see documentation; try '(help help)'!\n");
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
