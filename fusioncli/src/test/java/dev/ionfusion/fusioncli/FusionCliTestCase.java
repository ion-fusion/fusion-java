// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli;

import static dev.ionfusion.testing.ProjectLayout.fusionBootstrapDirectory;
import static dev.ionfusion.testing.ProjectLayout.mainRepositoryDirectory;

import dev.ionfusion.fusioncli.framework.Cli;
import dev.ionfusion.fusioncli.framework.CliTestCase;
import dev.ionfusion.fusioncli.framework.Stdio;

public class FusionCliTestCase
    extends CliTestCase
{
    @Override
    protected Cli<?> cli()
    {
        return new FusionCli(new Stdio(stdin(), stdout(), stderr()));
    }

    protected String[] commonOptions()
    {
        return new String[] {
            // This enables running tests in IDEA, which doesn't consume the assembled jar
            // containing embedded modules.  The argument has no effect when running via
            // Gradle, since the embedded modules take precedence.
            "--repositories",
            fusionBootstrapDirectory().toString(),

            "--repositories",
            mainRepositoryDirectory().toString(),
        };
    }
}
