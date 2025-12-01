// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

/**
 * Indicates that an argument to the CLI is malformed or otherwise unusable.
 */
@SuppressWarnings("serial")
class UsageException
    extends Exception
{
    final Command myCommand;

    UsageException(Command command, String message)
    {
        super(message);
        myCommand = command;
    }

    UsageException(String message)
    {
        this(null, message);
    }
}
