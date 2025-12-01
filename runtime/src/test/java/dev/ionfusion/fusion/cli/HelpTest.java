// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.fusion.cli.Help.APP_HELP_TEXT_INTRO;
import static dev.ionfusion.fusion.cli.Help.HELP_ONE_LINER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;

public class HelpTest
    extends CliTestCase
{
    @Test
    public void testHelpNoArgs()
        throws Exception
    {
        run("help");

        assertThat(stdoutText, startsWith("\n" + APP_HELP_TEXT_INTRO));
        assertThat(stderrText, isEmptyString());
    }


    @Test
    public void testHelpHelp()
        throws Exception
    {
        run("help", "help");

        assertThat(stdoutText, startsWith("\nhelp (?, h)\n  " + HELP_ONE_LINER));
        assertThat(stderrText, isEmptyString());
    }


    @Test
    public void testHelpBadCommand()
        throws Exception
    {
        run(1, "help", "bad");

        assertThat(stdoutText, startsWith("\nUnknown command: 'bad'\n"));
        assertThat(stderrText, startsWith("\nType 'fusion help' for more information."));
    }
}
