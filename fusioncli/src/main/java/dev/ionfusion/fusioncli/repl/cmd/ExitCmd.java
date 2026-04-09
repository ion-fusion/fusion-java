// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl.cmd;

import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.fusioncli.framework.Executor;
import dev.ionfusion.fusioncli.framework.UsageException;
import dev.ionfusion.fusioncli.repl.ReplContext;

public class ExitCmd
    extends Command<ReplContext>
{
    private static final String HELP_ONE_LINER =
        "Exit the REPL";

    private static final String HELP_USAGE =
        "exit";

    private static final String HELP_BODY =
        "Terminates the REPL.";


    public ExitCmd()
    {
        super("exit", "x");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }

    @Override
    protected Executor makeExecutor(String[] arguments)
        throws UsageException
    {
        // Ignore any arguments and signal that the REPL should exit.
        return () -> 1;
    }
}
