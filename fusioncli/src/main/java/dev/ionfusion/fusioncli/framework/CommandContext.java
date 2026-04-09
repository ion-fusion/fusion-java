// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * Baseline context for command creation and execution.
 */
public class CommandContext
{
    private final CommandSuite mySuite;


    public CommandContext(CommandSuite suite)
    {
        mySuite = suite;
    }


    public CommandSuite commandSuite()
    {
        return mySuite;
    }
}
