// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

import static dev.ionfusion.fusion.TestSetup.makeRuntimeBuilder;
import static dev.ionfusion.fusion.TestSetup.testRepositoryDirectory;
import static dev.ionfusion.fusion.TestSetup.testScriptDirectory;

import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.junit.TreeWalker;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;


/**
 * Run all the `*.test.fusion` scripts under the scripts directory as individual
 * unit tests.
 * Each test passes if script evaluation completes without failure.
 * <p>
 * All tests run using a single {@link FusionRuntime}.
 */
public class ScriptedTests
{
    /* 2024-03-28 Concurrent execution runs notably slower than same-thread,
     * presumably due to contention over symbol interning and/or module loading.
     */
    @TestFactory
    @DisplayName("scripts/")
    Stream<DynamicNode> scripts()
        throws Exception
    {
        FusionRuntime runtime =
            makeRuntimeBuilder().withRepositoryDirectory(testRepositoryDirectory().toFile())
                                .build();

        return TreeWalker.walk(testScriptDirectory(),
                               dir -> true,
                               file -> file.getFileName().toString().endsWith(".test.fusion"),
                               file -> runtime.makeTopLevel().load(file.toFile()));
    }
}
