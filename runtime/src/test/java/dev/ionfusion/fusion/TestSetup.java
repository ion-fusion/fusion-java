// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;import java.nio.file.Path;
import java.nio.file.Paths;

public class TestSetup
{
    /**
     * The root directory of this source code, as an absolute path.
     * <p>
     * Historically, test code assumed it was run from the project root, but
     * we'd prefer that not be a requirement.  Points of coupling to the code
     * layout should instead use this path so they are easily adjusted if this
     * assumption becomes false.
     * <p>
     * This is {@code static final} because Java doesn't guarantee that the
     * "working directory" can't be changed dynamically.
     * <p>
     * This is {@code public} because we'd prefer test code use this directly
     * than be blocked by not having a better resolver here.
     * </p>
     */
    public static final Path PROJECT_DIRECTORY =
        Paths.get("").toAbsolutePath();

    /**
     * The directory holding the Fusion bootstrap code (core libraries).
     *
     * @return an absolute path.
     */
    public static Path fusionBootstrapDirectory()
    {
        return PROJECT_DIRECTORY.resolve("src/main/fusion");
    }

    public static Path testScriptDirectory()
    {
        return testRepositoryDirectory().resolve("scripts");
    }

    public static Path testRepositoryDirectory()
    {
        return PROJECT_DIRECTORY.resolve("src/test/fusion");
    }

    public static Path testDataDirectory()
    {
        return PROJECT_DIRECTORY.resolve("src/test/data");
    }

    public static Path testDataFile(String path)
    {
        return testDataDirectory().resolve(path);
    }


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
        b = b.withBootstrapRepository(fusionBootstrapDirectory().toFile());

        // Enable this to have coverage collected during an IDE run.
//      b = b.withCoverageDataDirectory(new File("build/private/fcoverage"));

        // This has no effect in an IDE, since this file is not on its copy of
        // the test classpath.  In scripted builds, this provides the coverage
        // configuration. Historically, it also provided the bootstrap repo.
        b = b.withConfigProperties(TestSetup.class, "/fusion.properties");

        return b;
    }
}
