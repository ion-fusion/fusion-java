// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import dev.ionfusion.fusioncli.framework.Cli;
import dev.ionfusion.fusioncli.framework.CommandSuite;
import dev.ionfusion.fusioncli.framework.Separator;
import dev.ionfusion.fusioncli.framework.Stdio;

/**
 * Entry point to the Fusion command-line interface.
 */
public final class Main
{
    private final Stdio myStdio;


    public Main(Stdio stdio)
    {
        myStdio = stdio;
    }


    public int executeCommandLine(String[] args)
    {
        try
        {
            CommandSuite suite = new CommandSuite(new Repl(),
                                                  new Load(),
                                                  new Eval(),
                                                  new Require(),
                                                  new Cover(),
                                                  new Separator(),
                                                  new Help(),
                                                  new Version(),
                                                  new Document());

            GlobalOptions globals = new GlobalOptions(suite, myStdio);

            Cli<?> cli = new FusionCli(globals);

            try
            {
                return cli.executeCommandLine(args);
            }
            finally
            {
                // "Noisy" flush so we don't ignore failures to flush stdout.
                // We don't want to "succeed" when failing to write all output!
                myStdio.flush();
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace(myStdio.stderr());
            return 1;
        }
        finally
        {
            // Final effort to flush everything.
            myStdio.flushQuietly();
        }
    }


    public static void main(String[] args)
    {
        Main cli = new Main(Stdio.forSystem());

        int errorCode = cli.executeCommandLine(args);
        if (errorCode != 0)
        {
            System.exit(errorCode);
        }
    }
}
