// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import org.junit.jupiter.api.Test;

public class ReplHelpTest
    extends ReplTestCase
{
    @Test
    public void testReplHelp()
        throws Exception
    {
        supplyInput(",help help\n");
        runRepl();

        expectResponse("help (h ?):  Describe available REPL commands");
        expectResponse("Usage: ,help [COMMAND]");
    }

    @Test
    public void helpWithoutArgsListsCommands()
        throws Exception
    {
        supplyInput(",help\n");
        runRepl();

        expectError("Available commands:");
    }

    @Test
    public void helpWithTwoArgsGivesError()
        throws Exception
    {
        supplyInput(",help exit help\n");
        runRepl();

        expectError("Expected at most one command name");
    }

    @Test
    public void helpWithUnknownArgGivesError()
        throws Exception
    {
        supplyInput(",help not_a_command\n");
        runRepl();

        expectError("Unknown command: 'not_a_command'");
    }

    @Test
    public void helpHandlesExtraWhitespace()
        throws Exception
    {
        supplyInput(",help  exit  \n");  // Extra space intentional.
        runRepl();

        expectResponse("exit (x):");
    }
}
