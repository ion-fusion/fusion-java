// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

import static dev.ion_fusion.fusion.CoreTestCase.ftstRepositoryDirectory;
import static dev.ion_fusion.fusion.CoreTestCase.fusionBootstrapDirectory;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import dev.ion_fusion.fusion.FusionException;
import dev.ion_fusion.fusion.FusionRuntime;
import dev.ion_fusion.fusion.FusionRuntimeBuilder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;


/**
 * Run all the `*.test.fusion` scripts under the `ftst` directory as individual
 * unit tests.
 * Each test passes if script evaluation completes without failure.
 * <p>
 * All tests run using a single {@link FusionRuntime}.
 * If `ftst/repo` is a directory, it's added as a repository.
 */
public class ScriptedTests
{
    /* 2024-03-28 Concurrent execution runs notably slower than same-thread,
     * presumably due to contention over symbol interning and/or module loading.
     */
    @TestFactory
    @DisplayName("ftst/")
    Stream<DynamicNode> ftst()
        throws Exception
    {
        FusionRuntime runtime = makeRuntimeBuilder().build();

        return forDir(Paths.get("ftst"), runtime);
    }


    //========================================================================


    private FusionRuntimeBuilder makeRuntimeBuilder()
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
        b = b.withConfigProperties(getClass(), "/fusion.properties");

        b.addRepositoryDirectory(ftstRepositoryDirectory().toFile());

        return b.immutable();
    }


    private Stream<DynamicNode> forDir(Path dir, FusionRuntime runtime)
    {
        String[] fileNames = dir.toFile().list();
        assert fileNames != null : "Not a directory: " + dir.toAbsolutePath();

        // Sort the fileNames so they are listed in order.
        // This is not a functional requirement, but it helps humans scanning
        // the output looking for a specific file.
        Arrays.sort(fileNames);

        return Arrays.stream(fileNames)
                     .map(n -> forChild(dir.resolve(n), runtime))
                     .filter(Objects::nonNull);
    }


    private DynamicNode forChild(Path file, FusionRuntime runtime)
    {
        String name = file.getFileName().toString();
        URI    uri  = file.toUri();

        if (Files.isDirectory(file))
        {
            return dynamicContainer(name + "/", uri, forDir(file, runtime));
        }
        else if (name.endsWith(".test.fusion"))
        {
            return dynamicTest(name, uri, () -> runtime.makeTopLevel().load(file.toFile()));
        }
        return null;
    }
}
