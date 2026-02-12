// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime._private.cover;

import static dev.ionfusion.runtime._private.cover.CoverageCollectorFactory.fromDirectory;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createSymbolicLink;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.ionfusion.fusion.CoreTestCase;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CoverageCollectorTest
    extends CoreTestCase
{
    @TempDir
    public Path tmpDir;


    /**
     * Coverage collectors are unique, even when sharing the same directory.
     */
    @Test
    public void testUniqueInstances()
        throws Exception
    {
        CoverageCollectorImpl c1 = fromDirectory(tmpDir);
        CoverageCollectorImpl c2 = fromDirectory(tmpDir);
        assertNotSame(c1, c2);
    }


    /**
     * The implementation shares sessions within the same (canonicalized) data
     * directory.
     */
    @Test
    public void testSessionSharing()
        throws Exception
    {
        Path dir1 = tmpDir;
        Path dir2 = tmpDir.resolve(getClass().getSimpleName() + "-linkholder");
        createDirectory(dir2);

        Path linkToDir1 = dir2.resolve("link");
        createSymbolicLink(linkToDir1, dir1);

        CoverageCollectorImpl c1 = fromDirectory(dir1);
        CoverageCollectorImpl c2 = fromDirectory(dir2);
        CoverageCollectorImpl c3 = fromDirectory(linkToDir1);


        assertNotSame(c1.getSession(), c2.getSession(), "different dirs");
        assertSame(c1.getSession(), c3.getSession(), "canonicalized symlink");
    }
}
