// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DocumentTest
    extends CliTestCase
{
    @TempDir
    public File tmpDir;

    private File noSuchFile()
        throws Exception
    {
        File f = new File(tmpDir, "no-such-file");
        assertFalse(f.exists());
        return f;
    }

    private File plainFile()
        throws Exception
    {
        File f = File.createTempFile("junit", null, tmpDir);
        assert(f.isFile());
        return f;
    }

    private File outputDir()
        throws Exception
    {
        return newFolder(tmpDir, "docs-test-output");
    }


    @Test
    public void testOutputDirIsFile()
        throws Exception
    {
        String aFile = plainFile().getPath();

        run(1, "document", aFile);

        assertThat(stderrText, containsString(aFile));
    }

    @Test
    public void testRepoIsMissing()
        throws Exception
    {
        String repo = noSuchFile().getPath();

        run(1, "document", "--modules", repo, outputDir().getPath());

        assertThat(stderrText, containsString("not a directory"));
        assertThat(stderrText, containsString(repo));
    }

    @Test
    public void testRepoIsFile()
        throws Exception
    {
        String repo = plainFile().getPath();

        run(1, "document", "--modules", repo, outputDir().getPath());

        assertThat(stderrText, containsString("not a directory"));
        assertThat(stderrText, containsString(repo));
    }

    @Test
    @Disabled("This should instead evoke failure when no modules are selected")
    public void testRepoHasNoSrc()
        throws Exception
    {
        String repo = newFolder(tmpDir, "junit").getPath();

        run(1, "document", "--modules", repo, outputDir().getPath());

        assertThat(stderrText, containsString("has no src"));
        assertThat(stderrText, containsString(repo));
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
