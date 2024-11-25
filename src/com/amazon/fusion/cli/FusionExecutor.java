// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.cli;

import static com.amazon.fusion.FusionIo.write;
import static com.amazon.fusion.FusionVoid.isVoid;
import com.amazon.fusion.ExitException;
import com.amazon.fusion.FusionException;
import com.amazon.fusion.FusionRuntime;
import com.amazon.fusion.TopLevel;
import java.io.PrintWriter;

/**
 *
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
        throws FusionException, UsageException
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
