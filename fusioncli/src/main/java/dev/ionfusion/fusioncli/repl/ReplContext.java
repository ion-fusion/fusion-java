// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.fusioncli.framework.CommandContext;
import dev.ionfusion.fusioncli.framework.CommandSuite;
import java.io.PrintWriter;

public class ReplContext
    extends CommandContext
{
    protected final PrintWriter myOut;

    public ReplContext(CommandSuite suite, PrintWriter out)
    {
        super(suite);
        myOut = out;
    }


    public PrintWriter out()
    {
        return myOut;
    }

    public void errorln(String message)
    {
        myOut.print("\033[1;31m"); // red
        myOut.print(message);
        myOut.print("\033[m");
        myOut.println();
    }
}
