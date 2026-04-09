// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * Executes a sequence of {@link Executor}s.
 */
public class SequenceExecutor
    implements Executor
{
    private final Iterable<Executor> myExecutors;

    public SequenceExecutor(Iterable<Executor> executors)
    {
        myExecutors = executors;
    }


    @Override
    public int execute()
        throws Exception
    {
        for (Executor exec : myExecutors)
        {
            int errorCode = exec.execute();
            if (errorCode != 0)
            {
                return errorCode;
            }
        }

        return 0;
    }
}
