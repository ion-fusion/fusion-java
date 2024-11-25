// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.cli;

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
        String scriptPath = "tst-data/no-such-script";
        run(1, "load", scriptPath);

        assertThat(stdoutText, isEmptyString());
        assertThat(stderrText, containsString("not a readable file"));
        assertThat(stderrText, containsString(scriptPath));
    }

    @Test
    public void testVoidResult()
        throws Exception
    {
        run(0, "load", "tst-data/trivialDefine.fusion");

        assertThat(stdoutText, isEmptyString());
        assertThat(stderrText, isEmptyString());
    }

    @Test
    public void testSingleResult()
        throws Exception
    {
        run(0, "load", "tst-data/hello.ion");

        assertEquals("\"hello\"\n", stdoutText);
        assertThat(stderrText, isEmptyString());
    }

    @Test
    public void testMultipleResults()
        throws Exception
    {
        run(0, "load", "tst-data/two-results.fusion");

        assertEquals("1\n\"2\"\n", stdoutText);
        assertThat(stderrText, isEmptyString());
    }
}
