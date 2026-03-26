// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Entry point to the Fusion command-line interface.
 */
public final class Main
{
    private final InputStream myStdin;
    private final PrintStream myStdout;
    private final PrintStream myStderr;


    public Main(InputStream stdin, PrintStream stdout, PrintStream stderr)
    {
        myStdin  = stdin;
        myStdout = stdout;
        myStderr = stderr;
    }


    public int executeCommandLine(String[] args)
    {
        try
        {
            CommandFactory cf = new CommandFactory(myStdin, myStdout, myStderr);
            return cf.executeCommandLine(args);
        }
        catch (Throwable e)
        {
            e.printStackTrace(myStderr);
            return 1;
        }
        finally
        {
            myStdout.flush();
            myStderr.flush();
        }
    }


    public static void main(String[] args)
    {
        Main cli = new Main(System.in, System.out, System.err);

        int errorCode = cli.executeCommandLine(args);
        if (errorCode != 0)
        {
            System.exit(errorCode);
        }
    }
}
