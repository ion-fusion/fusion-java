// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.fusion.cli.Command.Executor;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class CommandFactory
{
    public static final String APP_NAME = "fusion";

    private static final int USAGE_ERROR_CODE = 1;


    private final InputStream myStdin;
    private final PrintStream myStdout;
    private final PrintStream myStderr;

    CommandFactory(InputStream stdin, PrintStream stdout, PrintStream stderr)
    {
        myStdin  = stdin;
        myStdout = stdout;
        myStderr = stderr;
    }


    public static Command[] getAllCommands()
    {
        return new Command[]
            {
                new Repl(),
                new Load(),
                new Eval(),
                new Require(),
                new Cover(),
                new Separator(),
                new Help(),
                new Version(),
                new Document(),
            };
    }


    public static Command getMatchingCommand(String subcommand)
    {
        Command[] allCommands = getAllCommands();

        for (Command cmd : allCommands)
        {
            if (cmd.matches(subcommand))
            {
                return cmd;
            }
        }

        return null;
    }


    private void writeUsage(Command cmd)
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
     *
     * @param commandLine must not be <code>null</code>.
     *
     * @return the {@link Command} to execute; not null.
     *
     * @throws UsageException if there are
     * command-line errors preventing the command from being determined.
     */
    public static Command matchCommand(GlobalOptions globals,
                                       String[] commandLine)
        throws Exception
    {
        if (commandLine.length == 0)
        {
            throw new UsageException("No command given.");
        }

        String command = commandLine[0];

        Command cmd = getMatchingCommand(command);
        if (cmd == null)
        {
            throw new UsageException("Unknown command: '" + command + "'");
        }
        return cmd;
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
    public static Executor makeExecutor(GlobalOptions globals,
                                        Command  command,
                                        String[] commandLine)
        throws UsageException
    {
        // Strip off the leading command name, leaving the options and args.
        int argCount = commandLine.length - 1;
        String[] args = new String[argCount];
        System.arraycopy(commandLine, 1, args, 0, argCount);

        Executor exec = command.prepare(globals, args);
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
        GlobalOptions globals = new GlobalOptions(myStdin, myStdout, myStderr);

        try
        {
            commandLine = Command.extractOptions(globals, commandLine, true);

            // Eagerly parse all commands and their args so that any errors can
            // be reported before executing anything.
            List<Executor> execs = makeExecutors(globals, commandLine);
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
            writeUsage(e.myCommand);
            return USAGE_ERROR_CODE;
        }
    }


    private static List<Executor> makeExecutors(GlobalOptions globals,
                                                String[] commandLine)
        throws Exception
    {
        List<Executor> execs = new ArrayList<>();

        int curStartPos = 0;
        for (int i = 0; i < commandLine.length; i++)
        {
            if (";".equals(commandLine[i])) {
                int len = i-curStartPos;
                if (len > 0) {
                    Executor exec = makeExecutor(globals,
                                                 commandLine,
                                                 curStartPos,
                                                 len);
                    execs.add(exec);
                }
                curStartPos = i+1;
            }
        }

        int len = commandLine.length-curStartPos;
        Executor exec = makeExecutor(globals, commandLine, curStartPos, len);
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
    private static Executor makeExecutor(GlobalOptions globals,
                                         String[] commandLine,
                                         int start,
                                         int len)
        throws Exception
    {
        String[] segment = new String[len];
        System.arraycopy(commandLine, start, segment, 0, len);

        Command command = matchCommand(globals, segment);

        return makeExecutor(globals, command, segment);
    }


    static class Separator extends Command
    {

        Separator() {
            super("-----");
            putHelpText("-------------------------", "-----", "-----");
        }

        @Override
        boolean matches(String command)
        {
            return false;
        }

        @Override
        public Executor makeExecutor(String[] arguments)
        {
            return null;
        }
    }
}
