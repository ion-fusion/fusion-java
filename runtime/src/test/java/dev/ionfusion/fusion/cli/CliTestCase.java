// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.testing.ProjectLayout.fusionBootstrapDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ionfusion.testing.StdioTestCase;
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

        stdoutText = stdoutToString();
        stderrText = stderrToString();

        if (expectedErrorCode != errorCode)
        {
            dumpStdout();
            dumpStderr();
        }

        assertEquals(expectedErrorCode, errorCode, "error code");
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

        // This enables running tests in IDEA, which doesn't consume the assembled jar
        // containing embedded modules.  The argument has no effect when running via
        // Gradle, since the embedded modules take precedence.
        join.add("--repositories");
        join.add(fusionBootstrapDirectory().toString());

        join.addAll(Arrays.asList(commandLine));
        return join.toArray(new String[0]);
    }
}
