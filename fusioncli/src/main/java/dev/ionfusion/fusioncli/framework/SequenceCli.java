// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for CLIs that accept a sequence of semicolon-separated subcommands.
 *
 * @param <Context> contains the global state for the CLI and is available to all
 * commands.
 */
public abstract class SequenceCli<Context extends CommandContext>
    extends Cli<Context>
{
    protected SequenceCli(Context context)
    {
        super(context);
    }

    /**
     * Parses the given command line, first splitting it into semicolon-separated
     * segments.
     */
    @Override
    protected Executor parseCommandLine(String[] commandLine)
        throws Exception
    {
        List<Executor> execs = new ArrayList<>();

        int curStartPos = 0;
        for (int i = 0; i < commandLine.length; i++)
        {
            if (";".equals(commandLine[i]))
            {
                int len = i - curStartPos;
                if (len > 0)
                {
                    Executor exec = parseCommand(commandLine, curStartPos, len);
                    execs.add(exec);
                }
                curStartPos = i + 1;
            }
        }

        int      len  = commandLine.length - curStartPos;
        Executor exec = parseCommand(commandLine, curStartPos, len);
        execs.add(exec);

        return new SequenceExecutor(execs);
    }
}
