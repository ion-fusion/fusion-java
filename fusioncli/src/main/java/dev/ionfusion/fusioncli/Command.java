// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static dev.ionfusion.fusioncli.framework.OptionParser.extractOptions;

import dev.ionfusion.fusioncli.framework.Executor;
import dev.ionfusion.fusioncli.framework.UsageException;

public abstract class Command
{
    private final String   myCommand;
    private final String[] myAliases;

    private String myHelpOneLiner = null;
    private String myHelpUsage = "your guess is as good as mine!";
    private String myHelpBody = "Sorry, this command is undocumented.\n";


    //=========================================================================
    // Construction and Initialization

    protected Command(String command, String... aliases)
    {
        myCommand = command;
        myAliases = (aliases == null ? new String[0] : aliases);
    }


    /**
     * Sets the help text for this command.
     * <p>
     * Nomenclature: this is called "put" to hide it from the
     * BeanUtils.copyProperty call in extractOptions
     *
     * @param oneLiner must be non-empty. It should start with a verb and end
     * with a period.
     * @param usage must be non-empty, and must start with the primary command.
     * @param body must be non-empty.  It should not be indented, and it must
     * not end with a newline.  It should be (explicitly) wrapped to display
     * within 80 columns.
     */
    protected void putHelpText(String oneLiner, String usage, String body)
    {
        assert oneLiner != null && !oneLiner.isEmpty();
        assert usage != null && usage.startsWith(myCommand);
        assert body != null && !body.endsWith("\n");

        myHelpOneLiner = oneLiner;
        myHelpUsage    = usage;
        myHelpBody     = body;
    }


    //=========================================================================
    // Property Accessors

    String getCommand()
    {
        return myCommand;
    }

    /**
     * Gets the aliases for this command, generally shortened forms of
     * {@link #getCommand}.
     *
     * @return the array of aliases, not null.
     */
    String[] getAliases()
    {
        return myAliases;
    }

    /**
     * If null, the command will not be listed by `help`.
     */
    String getHelpOneLiner()
    {
        return myHelpOneLiner;
    }

    String getHelpUsage()
    {
        return myHelpUsage;
    }

    String getHelpBody()
    {
        return myHelpBody;
    }


    //=========================================================================
    // CLI Argument Processing Methods

    /**
     * Verify that the command string used to invoke the command matches
     * the command or one of its aliases.
     */
    public boolean matches(String command)
    {
        if (getCommand().equals(command)) return true;
        for (String alias : myAliases)
        {
            if (alias.equals(command)) return true;
        }
        return false;
    }


    /**
     * Create a new object to receive command-specific options via injection.
     * Subclasses should override this if they have options.
     *
     * @param globals the populated global options.
     *
     * @return null if there are no command options.
     */
    Object makeOptions(GlobalOptions globals)
    {
        return null;
    }


    /**
     * Perform pre-processing, including in particular argument processing.
     * A null result causes the framework to emit command-specific usage help.
     *
     * @param args to process
     *
     * @return an {@link Executor} to execute the command; null if there are
     * usage errors.
     *
     * @throws UsageException if there are
     * command-line errors preventing the command from being used.
     */
    Executor prepare(GlobalOptions globals, String[] args)
        throws UsageException
    {
        Object options = makeOptions(globals);

        args = extractOptions(options, args, true);

        return makeExecutor(globals, options, args);
    }


    /**
     * Prepare a command executor based on the global options, any local options,
     * and the remaining command-line arguments.
     * <p>
     * This implementation ignores the {@code options} and invokes
     * {@link #makeExecutor(GlobalOptions, String[])}.
     *
     * @return null if the arguments are inappropriate or insufficient.
     *
     * @throws UsageException if there are command-line errors preventing the
     * command from being used.
     */
    Executor makeExecutor(GlobalOptions globals,
                          Object        options,
                          String[]      arguments)
        throws UsageException
    {
        return makeExecutor(globals, arguments);
    }


    Executor makeExecutor(GlobalOptions globals,
                          String[]      arguments)
        throws UsageException
    {
        return makeExecutor(arguments);
    }


    /**
     * Parses the command-line arguments to build a {@link Executor} for
     * execution.
     * Note that any options (<em>i.e.</em>, arguments prefixed by
     * {@code "--"}) will have already been extracted from the
     * {@code arguments} array.
     *
     * @param arguments to parse
     * @return null if the arguments are inappropriate or insufficient.
     */
    Executor makeExecutor(String[] arguments)
        throws UsageException
    {
        return null;
    }


    UsageException usage()
    {
        return new UsageException(this, null);
    }

    UsageException usage(String message)
    {
        return new UsageException(this, message);
    }
}
