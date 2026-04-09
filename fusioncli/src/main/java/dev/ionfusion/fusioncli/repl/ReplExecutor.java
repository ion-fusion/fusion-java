// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.fusioncli.framework.Executor;

public abstract class ReplExecutor
    implements Executor
{
    private final ReplContext myContext;

    protected ReplExecutor(ReplContext context)
    {
        myContext = context;
    }


    protected ReplContext context()
    {
        return myContext;
    }
}
