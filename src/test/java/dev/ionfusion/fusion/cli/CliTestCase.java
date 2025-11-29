// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.fusion.TestSetup.fusionBootstrapDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ionfusion.fusion.junit.StdioTestCase;
import java.util.ArrayList;
import java.util.Arrays;

public class CliTestCase
    extends StdioTestCase
{
    protected String stdoutText;
    protected String stderrText;


    void run(String... commandLine)
        throws Exception
    {
        run(0, commandLine);
    }

    void run(int expectedErrorCode, String... commandLine)
        throws Exception
    {
        int errorCode = execute(commandLine);

        assertEquals(expectedErrorCode, errorCode, "error code");

        stdoutText = stdoutToString();
        stderrText = stderrToString();
    }

    private int execute(String... commandLine)
        throws Exception
    {
        commandLine = prependGlobalOptions(commandLine);

        CommandFactory cf = new CommandFactory(stdin(), stdout(), stderr());
        return cf.executeCommandLine(commandLine);
    }

    private String[] prependGlobalOptions(String[] commandLine)
    {
        ArrayList<String> join = new ArrayList<>();

        join.add("--repositories");
        join.add(fusionBootstrapDirectory().toString());

        join.addAll(Arrays.asList(commandLine));
        return join.toArray(new String[0]);
    }
}
