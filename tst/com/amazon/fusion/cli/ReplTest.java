// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import org.junit.jupiter.api.Test;

public class ReplTest
    extends CliTestCase
{
    @Test
    public void testSimpleExpression()
        throws Exception
    {
        supplyInput("33908\n");
        run("repl");

        assertThat(stdoutText, containsString("33908\n"));
        assertThat(stderrText, isEmptyString());
    }

    @Test
    public void testHelpHelp()
        throws Exception
    {
        supplyInput("(help help)\n");
        run("repl");

        assertThat(stdoutText, containsString("(help ident ...)"));
        assertThat(stderrText, isEmptyString());
    }
}
