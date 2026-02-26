// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.testing.ProjectLayout;
import java.io.IOException;

public class TestSetup
{
    /**
     * Create a standard Fusion runtime builder, configured for testing.
     *
     * @return a new, mutable builder instance.
     */
    public static FusionRuntimeBuilder makeRuntimeBuilder()
        throws FusionException
    {
        FusionRuntimeBuilder b = FusionRuntimeBuilder.standard();

        // This allows tests to run in an IDE, so that we don't have to copy the
        // bootstrap repo into the classpath.  In scripted builds, this has no
        // effect since the classpath includes the code, which will shadow the
        // content of this directory.
        b = b.withBootstrapRepository(ProjectLayout.fusionBootstrapDirectory().toFile());

        // Enable this to have coverage collected during an IDE run.
//      b = b.withCoverageDataDirectory(new File("build/private/fcoverage"));

        // This has no effect in an IDE, since this file is not on its copy of
        // the test classpath.  In scripted builds, this provides the coverage
        // configuration. Historically, it also provided the bootstrap repo.
        try
        {
            b = b.withConfigProperties(TestSetup.class, "/fusion.properties");
        }
        catch (IOException e)
        {
            throw new FusionException(e);
        }

        return b;
    }
}
