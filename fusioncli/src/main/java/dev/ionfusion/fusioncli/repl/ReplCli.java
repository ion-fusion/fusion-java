// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.fusioncli.framework.Cli;
import dev.ionfusion.fusioncli.framework.UsageException;

public class ReplCli
    extends Cli<ReplContext>
{
    protected ReplCli(ReplContext context)
    {
        super(context);
    }

    @Override
    protected int handleUsage(UsageException e)
    {
        context().errorln(e.getMessage());
        return 0;
    }
}
