// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.fusioncli.framework.CommandContext;
import dev.ionfusion.fusioncli.framework.CommandSuite;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.PrintWriter;

public class ReplContext
    extends CommandContext
{
    private   final TopLevel    myTopLevel;
    protected final PrintWriter myOut;

    public ReplContext(CommandSuite suite, TopLevel topLevel, PrintWriter out)
    {
        super(suite);
        myTopLevel = topLevel;
        myOut = out;
    }


    public TopLevel top()
    {
        return myTopLevel;
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
