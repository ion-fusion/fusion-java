// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Coarse-grained testing of the CLI executable.
 * <p>
 * We are not trying to test the functionality of the CLI; that's handled by
 * unit tests that don't require installing the distibution.  Instead, our goal
 * is to ensure that the <em>distribution</em> packaging is correct: that the
 * shell scripts run properly, have the correct classpath, etc.
 *
 */
public class CliSmokeTest
{
    private static final String CLI_PATH = "build/install/ion-fusion-sdk/bin/fusion";

    @BeforeAll
    public static void ensureCliIsExecutable()
    {
        assertTrue(new File(CLI_PATH).exists(),
                   "CLI does not exist: " + CLI_PATH);
        assertTrue(new File(CLI_PATH).canExecute(),
                   "CLI can't be executed: " + CLI_PATH);
    }


    @Test
    public void testSimpleEval()
        throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder(CLI_PATH, "eval", "(+ 1 2)");
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String output = toString(p.getInputStream());

        assertEquals("3\n", output);

        int exitCode = p.waitFor();
        assertEquals(0, exitCode, "exit code");
    }


    public String toString(InputStream in)
        throws IOException
    {
        StringBuilder buf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                buf.append(line).append('\n');
            }
        }
        return buf.toString();
    }
}
