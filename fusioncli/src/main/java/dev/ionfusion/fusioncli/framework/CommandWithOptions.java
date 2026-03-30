// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import static dev.ionfusion.fusioncli.framework.OptionParser.extractOptions;

public abstract class CommandWithOptions<Context extends CommandContext>
    extends Command<Context>
{
    protected CommandWithOptions(String command, String... aliases)
    {
        super(command, aliases);
    }


    /**
     * Create a new object to receive command-specific options via injection.
     *
     * @param context the command context, including global options.
     *
     * @return the options argument to populate.
     */
    protected abstract Object makeOptions(Context context);


    @Override
    public final Executor makeExecutor(Context context, String[] args)
        throws UsageException
    {
        Object options = makeOptions(context);

        args = extractOptions(options, args, true);

        return makeExecutor(context, options, args);
    }


    /**
     * Prepare a command executor based on the global options, any local options,
     * and the remaining command-line arguments.
     * <p>
     * Note that any options (<em>i.e.</em>, arguments prefixed by
     * {@code "--"}) will have already been extracted from the
     * {@code arguments} array.
     *
     * @return null if the arguments are inappropriate or insufficient.
     *
     * @throws UsageException if there are command-line errors preventing the
     * command from being used.
     */
    protected abstract Executor makeExecutor(Context  context,
                                             Object   options,
                                             String[] arguments)
        throws UsageException;
}
