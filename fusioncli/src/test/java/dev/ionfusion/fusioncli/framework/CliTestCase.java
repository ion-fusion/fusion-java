// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import static dev.ionfusion.runtime._private.util.Empties.EMPTY_STRING_ARRAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ionfusion.testing.StdioTestCase;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class CliTestCase
    extends StdioTestCase
{
    protected String stdoutText;
    protected String stderrText;

    protected abstract Cli<?> cli();

    /**
     * Options to be prepended to every command line.
     */
    protected String[] commonOptions()
    {
        return EMPTY_STRING_ARRAY;
    }


    protected void run(String... commandLine)
        throws Exception
    {
        run(0, commandLine);
    }

    protected void run(int expectedErrorCode, String... commandLine)
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

    protected int execute(String... commandLine)
        throws Exception
    {
        String[] fullLine = concat(commonOptions(), commandLine);

        return cli().executeCommandLine(fullLine);
    }


    private String[] concat(String[] head, String[] tail)
    {
        ArrayList<String> join = new ArrayList<>();

        join.addAll(Arrays.asList(head));
        join.addAll(Arrays.asList(tail));

        return join.toArray(EMPTY_STRING_ARRAY);
    }
}
