// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;


/**
 * Base class for CLI commands.  At minimum, subclasses must override one of the
 * {@code makeExecutor} variants to give the command some functionality.
 *
 * @param <Context> contains the global state for the CLI and is available to all
 * commands.
 */
public abstract class Command <Context extends CommandContext>
{
    private final String   myName;
    private final String[] myAliases;

    private String myHelpOneLiner = null;
    private String myHelpUsage    = "your guess is as good as mine!";
    private String myHelpBody     = "Sorry, this command is undocumented.\n";


    //=========================================================================
    // Construction and Initialization

    protected Command(String name, String... aliases)
    {
        assert name != null && !name.isEmpty();

        myName = name;
        myAliases = (aliases == null ? new String[0] : aliases);
    }


    /**
     * Sets the help text for this command.  This is intended to be called by subclass
     * constructors.
     *
     * @param oneLiner must be non-empty. It should start with a verb and end with a
     * period.
     * @param usage must be non-empty and must start with the primary command.
     * @param body must be non-empty.  It should not be indented, and it must not end
     * with a newline.  It should be (explicitly) wrapped to display within 80 columns.
     */
    protected void putHelpText(String oneLiner, String usage, String body)
    {
        assert oneLiner != null && !oneLiner.isEmpty();
        assert usage != null && usage.startsWith(myName);
        assert body != null && !body.endsWith("\n");

        myHelpOneLiner = oneLiner;
        myHelpUsage    = usage;
        myHelpBody     = body;
    }


    //=========================================================================
    // Property Accessors

    /**
     * Gets the primary name of this command.
     *
     * @return the name, not null or empty.
     */
    public String getName()
    {
        return myName;
    }

    /**
     * Gets the aliases for this command, generally shortened forms of
     * {@link #getName}.
     *
     * @return the array of aliases, not null.
     */
    public String[] getAliases()
    {
        return myAliases;
    }


    /**
     * If null, the command will not be listed by help mechanisms.
     */
    public String getHelpOneLiner()
    {
        return myHelpOneLiner;
    }

    public String getHelpUsage()
    {
        return myHelpUsage;
    }

    public String getHelpBody()
    {
        return myHelpBody;
    }


    //=========================================================================
    // CLI Argument Processing Methods

    /**
     * Verify that the command string used to invoke the command matches the command or
     * one of its aliases.
     */
    public boolean matches(String command)
    {
        if (getName().equals(command)) return true;
        for (String alias : myAliases)
        {
            if (alias.equals(command)) return true;
        }
        return false;
    }


    /**
     * Perform pre-processing, including in particular argument processing. A null
     * result causes the framework to emit command-specific usage help.
     * <p>
     * This implementation ignores the {@code context} and invokes
     * {@link #makeExecutor(String[])}. Subclasses must override one of these variants.
     *
     * @param args to process
     *
     * @return an {@link Executor} to execute the command; null indicates a usage error.
     *
     * @throws UsageException if there are command-line errors preventing the command
     * from being used.
     */
    public Executor makeExecutor(Context context, String[] args)
        throws UsageException
    {
        return makeExecutor(args);
    }

    /**
     * Parses the command-line arguments to build a {@link Executor} for execution.
     *
     * @param arguments to parse
     *
     * @return an {@link Executor} to execute the command; null indicates a usage error.
     *
     * @throws UsageException if there are command-line errors preventing the command
     * from being used.
     */
    protected Executor makeExecutor(String[] arguments)
        throws UsageException
    {
        return null;
    }


    protected UsageException usage()
    {
        return new UsageException(this, null);
    }

    protected UsageException usage(String message)
    {
        return new UsageException(this, message);
    }
}
