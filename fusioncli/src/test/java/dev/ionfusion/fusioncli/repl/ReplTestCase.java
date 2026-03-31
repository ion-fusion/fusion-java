// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import dev.ionfusion.fusioncli.FusionCliTestCase;

public class ReplTestCase
    extends FusionCliTestCase
{
    protected void runRepl()
        throws Exception
    {
        run("repl");
    }


    protected void expectResponse(String message)
    {
        assertThat(stdoutText, containsString(message));
        assertThat(stderrText, is(emptyString()));
    }

    /**
     * The REPL only writes to stdout. I'm not sure if that is the right behavior.
     */
    protected void expectError(String message)
    {
        assertThat(stdoutText, containsString(message));
        assertThat(stderrText, is(emptyString()));
    }
}
