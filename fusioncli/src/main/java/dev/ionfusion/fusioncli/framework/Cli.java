// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * Base class for a Command Line Interface.
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
}
