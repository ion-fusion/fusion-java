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
    public void replTakesNoArgs()
        throws Exception
    {
        // This fails before entering the REPL.
        run(1, "repl", "arg");
        assertThat(stderrText, containsString("Usage: repl"));
    }


    //==================================================================================


    private void expectResponse(String message)
    {
        assertThat(stdoutText, containsString(message));
        assertThat(stderrText, is(emptyString()));
    }


    /**
     * The REPL only writes to stdout. I'm not sure if that is the right behavior.
     */
    private void expectError(String message)
    {
        assertThat(stdoutText, containsString(message));
        assertThat(stderrText, is(emptyString()));
    }


    //==================================================================================


    @Test
    public void testSimpleExpression()
        throws Exception
    {
        supplyInput("33908\n");
        run("repl");

        expectResponse("33908\n");
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
    public void testIonSyntaxError()
        throws Exception
    {
        supplyInput("(void]\n");
        run("repl");

        expectError("Error reading source:");
    }


    @Test
    public void testFusionSyntaxError()
        throws Exception
    {
        supplyInput("(no_binding)\n");
        run("repl");

        expectError("Bad syntax: unbound identifier.");
        expectError("no_binding");
    }


    @Test
    public void testHelpHelp()
        throws Exception
    {
        supplyInput("(help help)\n");
        run("repl");

        expectResponse("(help ident ...)");
    }
}
