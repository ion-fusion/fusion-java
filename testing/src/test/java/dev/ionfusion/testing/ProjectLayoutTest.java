// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ProjectLayoutTest
{
    @Test
    public void testBootstrapRepo()
    {
        Path dir = ProjectLayout.fusionBootstrapDirectory();
        assertTrue(isDirectory(dir));

        Path file = dir.resolve("modules").resolve("fusion.fusion");
        assertTrue(isRegularFile(file));
    }


    @Test
    public void testScripts()
    {
        Path dir = ProjectLayout.testScriptDirectory();
        assertTrue(isDirectory(dir));

        Path file = dir.resolve("empty.fusion");
        assertTrue(isRegularFile(file));
    }


    @Test
    public void testData()
    {
        Path dir = ProjectLayout.testDataDirectory();
        assertTrue(isDirectory(dir));

        Path file = dir.resolve("empty.ion");
        assertTrue(isRegularFile(file));

        Path file2 = ProjectLayout.testDataFile("empty.ion");
        assertEquals(file, file2);
    }
}
