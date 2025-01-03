// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;

public class CommandFactoryTest
    extends CliTestCase
{
    @Test
    public void testUsage()
        throws Exception
    {
        run(1, "not-a-command");

        assertThat(stderrText, startsWith("\nUnknown command: 'not-a-command'\n"));
    }
}
