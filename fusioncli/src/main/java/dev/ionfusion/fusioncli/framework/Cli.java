// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * Base class for defining a Command Line Interface and executing commands.
 * <p>
 * Execution of a command line has these phases:
 * <ol>
 *     <li>The CLI matches the command name to a {@link Command}.</li>
 *     <li>The {@link Command} makes an {@link Executor} from any options and arguments.</li>
 *     <li>The {@link Executor} executes the command.</li>
 * </ol>
 * Note that the full command line is parsed before any commands are executed.
 *
 * @param <Context> contains the global state for the CLI and is available to all
 * commands.
 */
public abstract class Cli<Context extends CommandContext>
{
    private final Context myContext;

    protected Cli(Context context)
    {
        myContext = context;
    }

    public Context context()
    {
        return myContext;
    }


    /**
     * Parses the given command line and executes the appropriate command(s).
     *
     * @return an error code, zero meaning success.
     */
    public int executeCommandLine(String... commandLine)
        throws Exception
    {
        try
        {
            commandLine = extractCommonOptions(commandLine);

            return parseCommandLine(commandLine).execute();
        }
        catch (UsageException e)
        {
            return handleUsage(e);
        }
    }


    /**
     * Extract any common options from the command line, applying them to our context.
     * <p>
     * This implementation does nothing.
     *
     * @return the command line with common options removed.
     */
    protected String[] extractCommonOptions(String[] commandLine)
        throws UsageException
    {
        return commandLine;
    }


    /**
     * Handle a usage exception raised during command parsing or execution.
     *
     * @return the result to return from {@link #executeCommandLine(String...)}.
     */
    protected abstract int handleUsage(UsageException e);


    /**
     * Parses the given command line, producing an {@code Executor}.
     * <p>
     * This implementation treats the entire command line as a single command.
     */
    protected Executor parseCommandLine(String[] commandLine)
        throws Exception
    {
        return parseCommand(commandLine, 0, commandLine.length);
    }


    /**
     * Parses a segment of a command line as a single command. The {@link CommandSuite}
     * matches the first element to a {@link Command}, which then makes an
     * {@link Executor} encapsulating any options and arguments.
     *
     * @param start the position within {@code commandLine} at which to start the
     * segment.
     * @param len the number of elements in the segment.
     *
     * @return an {@code Executor} implementing the segment, encapsulating all
     * necessary context.
     */
    @SuppressWarnings("unchecked")
    protected Executor parseCommand(String[] commandLine, int start, int len)
        throws Exception
    {
        if (len == 0)
        {
            throw new UsageException("No command given.");
        }

        String commandName = commandLine[start];
        if (commandName.isEmpty())
        {
            throw new UsageException("No command given.");
        }

        Command<Context> command =
            myContext.commandSuite().getMatchingCommand(commandName);
        if (command == null)
        {
            throw new UsageException("Unknown command: '" + commandName + "'");
        }

        // Strip off the leading command name, leaving the options and args.
        int      argCount = len - 1;
        String[] args     = new String[argCount];
        System.arraycopy(commandLine, start + 1, args, 0, argCount);

        Executor exec = command.makeExecutor(myContext, args);
        if (exec == null)
        {
            throw new UsageException(command, null);
        }
        return exec;
    }
}
