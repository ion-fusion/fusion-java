// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * An ordered collection of commands that define a CLI.
 */
public class CommandSuite
{
    private final Command[] myCommands;

    public CommandSuite(Command... commands)
    {
        myCommands = commands;
    }


    public Command[] getAllCommands()
    {
        return myCommands;
    }


    /**
     * Finds the command associated with the given word.
     *
     * @param word must not be null.
     *
     * @return the matching {@link Command}; null if no command matches.
     */
    public Command getMatchingCommand(String word)
    {
        Command[] allCommands = getAllCommands();

        for (Command cmd : allCommands)
        {
            if (cmd.matches(word))
            {
                return cmd;
            }
        }

        return null;
    }
}
