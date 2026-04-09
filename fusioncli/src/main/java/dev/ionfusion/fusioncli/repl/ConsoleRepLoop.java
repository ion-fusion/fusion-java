// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import java.io.Console;

public class ConsoleRepLoop
    extends RepLoop
{
    private final Console myConsole;

    public ConsoleRepLoop(FusionRuntime runtime)
        throws FusionException
    {
        super(runtime, System.console().writer());

        myConsole = System.console();
    }


    protected String readLine()
    {
        return myConsole.readLine(" ");
    }
}
