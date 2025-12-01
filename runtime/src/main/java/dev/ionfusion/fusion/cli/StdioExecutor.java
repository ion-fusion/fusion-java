// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import dev.ionfusion.fusion.cli.Command.Executor;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Base {@link Executor} that's configured with the standard character-output
 * streams.
 */
abstract class StdioExecutor
    implements Executor
{
    private final GlobalOptions myGlobals;

    StdioExecutor(GlobalOptions globals)
    {
        myGlobals = globals;
    }


    @Override
    public final int execute()
        throws Exception
    {
        PrintWriter out =
            new PrintWriter(new OutputStreamWriter(myGlobals.stdout(), UTF_8));
        PrintWriter err =
            new PrintWriter(new OutputStreamWriter(myGlobals.stderr(), UTF_8));

        try
        {
            return execute(out, err);
        }
        finally
        {
            out.flush();
            err.flush();
        }
    }


    abstract int execute(PrintWriter out, PrintWriter err)
        throws Exception;
}
