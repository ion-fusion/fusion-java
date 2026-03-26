// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import dev.ionfusion.fusioncli.Command;

/**
 * Indicates that a command argument is malformed or otherwise unusable.
 */
@SuppressWarnings("serial")
public class UsageException
    extends Exception
{
    private final Command myCommand;

    public UsageException(Command command, String message)
    {
        super(message);
        myCommand = command;
    }

    public UsageException(String message)
    {
        this(null, message);
    }


    public Command getCommand()
    {
        return myCommand;
    }
}
