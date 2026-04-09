// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

public class ReplTest
    extends ReplTestCase
{
    @Test
    public void replTakesNoArgs()
        throws Exception
    {
        // This fails before entering the REPL.
        run(1, "repl", "arg");
        assertThat(stderrText, containsString("Usage: repl"));
    }


    @Test
    public void testSimpleExpression()
        throws Exception
    {
        supplyInput("33908\n");
        runRepl();

        expectResponse("33908\n");
    }


    @Test
    public void testEmptyInput()
        throws Exception
    {
        // No input; REPL should exit on EOF
        runRepl();

        // We want output to end with a newline.
        assertThat(stdoutText, endsWith("\n"));
    }


    @Test
    public void testIonSyntaxError()
        throws Exception
    {
        supplyInput("(void]\n");
        runRepl();

        expectError("Error reading source:");
    }


    @Test
    public void testFusionSyntaxError()
        throws Exception
    {
        supplyInput("(no_binding)\n");
        runRepl();

        expectError("Bad syntax: unbound identifier.");
        expectError("no_binding");
    }


    //==================================================================================
    // Basic comma-commands

    @Test
    public void commaWithoutCommand()
        throws Exception
    {
        supplyInput(",\n");
        runRepl();

        expectError("No command given.");
    }

    @Test
    public void unknownCommand()
        throws Exception
    {
        supplyInput(",bad\n");
        runRepl();

        expectError("Unknown command: 'bad'");
    }


    @Test
    public void exitIgnoresSubsequentCommands()
        throws Exception
    {
        supplyInput(",exit\n,bad\n");
        runRepl();

        // We shouldn't get to the unknown command
        assertThat(stdoutText, not(containsString("bad")));
        assertThat(stderrText, not(containsString("bad")));
    }
}
