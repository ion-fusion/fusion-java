// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;

public class FusionCliTest
    extends FusionCliTestCase
{
    @Test
    public void noCommand()
        throws Exception
    {
        run(1, "");

        assertThat(stderrText, startsWith("No command given.\n"));
    }

    @Test
    public void testUsage()
        throws Exception
    {
        run(1, "not-a-command");

        assertThat(stderrText, startsWith("Unknown command: 'not-a-command'\n"));
    }
}
