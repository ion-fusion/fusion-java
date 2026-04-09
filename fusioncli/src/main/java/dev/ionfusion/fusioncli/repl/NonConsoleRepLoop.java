// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import static java.nio.charset.StandardCharsets.UTF_8;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class NonConsoleRepLoop
    extends RepLoop
{
    private final BufferedReader myIn;

    public NonConsoleRepLoop(FusionRuntime runtime,
                             InputStream stdin,
                             PrintWriter stdout)
        throws FusionException
    {
        super(runtime, stdout);

        myIn = new BufferedReader(new InputStreamReader(stdin, UTF_8));
    }


    @Override
    protected String readLine()
        throws IOException
    {
        myOut.flush();
        return myIn.readLine();
    }
}
