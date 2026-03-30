// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static dev.ionfusion.fusioncli.framework.OptionParser.extractOptions;

import dev.ionfusion.fusioncli.framework.Cli;
import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.fusioncli.framework.Executor;
import dev.ionfusion.fusioncli.framework.UsageException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class CommandFactory
    extends Cli<GlobalOptions>
{
    public static final String APP_NAME = "fusion";

    private static final int USAGE_ERROR_CODE = 1;


    private final PrintStream myStdout;
    private final PrintStream myStderr;

    CommandFactory(GlobalOptions context)
    {
        super(context);

        myStdout = context.stdout();
        myStderr = context.stderr();
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
            myStderr.print(cmd.getCommand());
        }

        myStderr.println("' for more information.");
    }


    /**
     * Makes an {@link Executor} for a single command in the sequence.
     *
     * @param commandLine includes the leading command name.
     *
     * @return an {@link Executor} to execute the command; not null.
     *
     * @throws UsageException if there are
     * command-line errors preventing the command from being used.
     */
    public Executor makeExecutor(Command  command,
                                 String[] commandLine)
        throws UsageException
    {
        // Strip off the leading command name, leaving the options and args.
        int argCount = commandLine.length - 1;
        String[] args = new String[argCount];
        System.arraycopy(commandLine, 1, args, 0, argCount);

        Executor exec = command.makeExecutor(context(), args);
        if (exec == null)
        {
            throw new UsageException(command, null);
        }
        return exec;
    }


    /**
     * @return an error code, zero meaning success.
     */
    int executeCommandLine(String... commandLine)
        throws Exception
    {
        try
        {
            commandLine = extractOptions(context(), commandLine, true);

            // Eagerly parse all commands and their args so that any errors can
            // be reported before executing anything.
            List<Executor> execs = makeExecutors(commandLine);
            for (Executor exec : execs)
            {
                int errorCode = exec.execute();
                if (errorCode != 0)
                {
                    return errorCode;
                }
            }

            return 0;
        }
        catch (UsageException e)
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


    private List<Executor> makeExecutors(String[] commandLine)
        throws Exception
    {
        List<Executor> execs = new ArrayList<>();

        int curStartPos = 0;
        for (int i = 0; i < commandLine.length; i++)
        {
            if (";".equals(commandLine[i])) {
                int len = i-curStartPos;
                if (len > 0) {
                    Executor exec = makeExecutor(commandLine, curStartPos, len);
                    execs.add(exec);
                }
                curStartPos = i+1;
            }
        }

        int len = commandLine.length-curStartPos;
        Executor exec = makeExecutor(commandLine, curStartPos, len);
        execs.add(exec);

        return execs;
    }


    /**
     * Parses a segment of the command line into its command, options, and
     * arguments, producing an {@link Executor} that implements the command.
     *
     * @param start the position within {@code commandLine} at which to start
     * the segment.
     * @param len the number of words in the segment.
     *
     * @return an error code, zero meaning success.
     */
    private Executor makeExecutor(String[] commandLine,
                                  int start,
                                  int len)
        throws Exception
    {
        String[] segment = new String[len];
        System.arraycopy(commandLine, start, segment, 0, len);

        Command command = context().commandSuite().matchCommand(segment);

        return makeExecutor(command, segment);
    }
}
