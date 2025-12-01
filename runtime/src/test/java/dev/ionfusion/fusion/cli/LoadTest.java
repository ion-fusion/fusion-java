// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.fusion.TestSetup.testDataFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LoadTest
    extends CliTestCase
{
    @Test
    public void testNoSuchScript()
        throws Exception
    {
        String path = testDataFile("no-such-script").toString();
        run(1, "load", path);

        assertThat(stdoutText, isEmptyString());
        assertThat(stderrText, containsString("not a readable file"));
        assertThat(stderrText, containsString(path));
    }

    @Test
    public void testVoidResult()
        throws Exception
    {
        String path = testDataFile("trivialDefine.fusion").toString();
        run(0, "load", path);

        assertThat(stdoutText, isEmptyString());
        assertThat(stderrText, isEmptyString());
    }

    @Test
    public void testSingleResult()
        throws Exception
    {
        String path = testDataFile("hello.ion").toString();
        run(0, "load", path);

        assertEquals("\"hello\"\n", stdoutText);
        assertThat(stderrText, isEmptyString());
    }

    @Test
    public void testMultipleResults()
        throws Exception
    {
        String path = testDataFile("two-results.fusion").toString();
        run(0, "load", path);

        assertEquals("1\n\"2\"\n", stdoutText);
        assertThat(stderrText, isEmptyString());
    }
}
