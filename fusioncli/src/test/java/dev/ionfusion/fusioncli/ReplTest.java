// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

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
        assertThat(stderrText, is(emptyString()));
    }


    @Test
    public void testEmptyInput()
        throws Exception
    {
        // No input; REPL should exit on EOF
        run("repl");

        // We want output to end with a newline.
        assertThat(stdoutText, endsWith("\n"));
    }


    @Test
    public void testHelpHelp()
        throws Exception
    {
        supplyInput("(help help)\n");
        run("repl");

        assertThat(stdoutText, containsString("(help ident ...)"));
        assertThat(stderrText, is(emptyString()));
    }
}
