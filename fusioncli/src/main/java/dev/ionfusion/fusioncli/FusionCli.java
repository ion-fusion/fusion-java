// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static dev.ionfusion.fusioncli.framework.OptionParser.extractOptions;

import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.fusioncli.framework.SequenceCli;
import dev.ionfusion.fusioncli.framework.UsageException;
import java.io.PrintStream;

class FusionCli
    extends SequenceCli<GlobalOptions>
{
    public static final String APP_NAME = "fusion";

    private static final int USAGE_ERROR_CODE = 1;


    private final PrintStream myStdout;
    private final PrintStream myStderr;

    FusionCli(GlobalOptions context)
    {
        super(context);

        myStdout = context.stdout();
        myStderr = context.stderr();
    }


    @Override
    protected String[] extractCommonOptions(String[] commandLine)
        throws UsageException
    {
        return extractOptions(context(), commandLine, true);
    }


    private void writeUsage(Command<?> cmd)
    {
        if (cmd != null)
        {
            myStderr.print("Usage: ");
            myStderr.println(cmd.getHelpUsage());
        }

        myStderr.print("Type '" + APP_NAME + " help");

        if (cmd != null)
        {
            myStderr.print(' ');
            myStderr.print(cmd.getName());
        }

        myStderr.println("' for more information.");
    }


    @Override
    protected int handleUsage(UsageException e)
    {
        myStdout.flush();                // Avoid commingled console output.
        myStderr.println();
        String message = e.getMessage();
        if (message != null)
        {
            myStderr.println(message);
            myStderr.println();
        }
        writeUsage(e.getCommand());
        return USAGE_ERROR_CODE;
    }
}
