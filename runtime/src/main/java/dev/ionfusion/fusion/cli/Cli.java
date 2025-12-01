// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The Fusion command-line interface, just here to provide a {@link #main} method.
 */
public final class Cli
{
    private Cli() {}


    public static void main(String[] args)
    {
        InputStream stdin  = System.in;
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;

        int errorCode;

        try
        {
            CommandFactory cf = new CommandFactory(stdin, stdout, stderr);
            errorCode = cf.executeCommandLine(args);
        }
        catch (Throwable e)
        {
            errorCode = 1;
            e.printStackTrace(stderr);
        }

        stdout.flush();
        stderr.flush();

        if (errorCode != 0)
        {
            System.exit(errorCode);
        }
    }
}
