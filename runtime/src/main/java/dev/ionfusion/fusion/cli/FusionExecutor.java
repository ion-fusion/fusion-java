// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.fusion.FusionIo.write;
import static dev.ionfusion.fusion.FusionVoid.isVoid;

import dev.ionfusion.fusion.ExitException;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Command executor that uses a shared {@link FusionRuntime} from the
 * {@link GlobalOptions}.
 */
abstract class FusionExecutor
    extends StdioExecutor
{
    private final GlobalOptions myGlobalOptions;


    FusionExecutor(GlobalOptions globals)
    {
        super(globals);

        myGlobalOptions = globals;
    }


    FusionRuntime runtime()
        throws IOException, FusionException, UsageException
    {
        return myGlobalOptions.runtime();
    }


    /**
     * Subclasses should generally override this unless they need special
     * exception handling.
     *
     * @return may be null, a Fusion value, or an array of Fusion values.
     */
    Object execute(TopLevel top)
        throws Exception
    {
        return null;
    }


    @Override
    public int execute(PrintWriter out, PrintWriter err)
        throws Exception
    {
        try
        {
            TopLevel top = runtime().getDefaultTopLevel();

            Object result = execute(top);

            writeResults(top, result, out);
        }
        catch (ExitException e)
        {
            // Do nothing, just return successfully.
        }
        catch (FusionException e)
        {
            // TODO optionally display the stack trace
            err.println(e.getMessage());
            return 1;
        }

        return 0;
    }


    void writeResults(TopLevel top, Object result, PrintWriter out)
        throws FusionException
    {
        if (result instanceof Object[])
        {
            Object[] results = (Object[]) result;
            for (Object r : results)
            {
                write(top, r, out);
                out.println();
            }
        }
        else if (result != null && ! isVoid(top, result))
        {
            write(top, result, out);
            out.println();
        }

        out.flush();
    }
}
