// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

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
            try
            {
                return new FusionCli(myStdio).executeCommandLine(args);
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
