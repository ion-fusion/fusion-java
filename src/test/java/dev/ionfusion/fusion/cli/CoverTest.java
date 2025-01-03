// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CoverTest
    extends CliTestCase
{
    @TempDir
    public File myFolder;


    private File plainFile()
        throws Exception
    {
        File f = File.createTempFile("junit", null, myFolder);
        assert(f.isFile());
        return f;
    }

    private File dataDir()
        throws IOException
    {
        return newFolder(myFolder, "data");
    }

    private File reportDir()
        throws IOException
    {
        return newFolder(myFolder, "report");
    }


    @Test
    public void testNoArgs()
        throws Exception
    {
        run(1, "report_coverage");
        assertThat(stderrText, containsString("Usage:"));
    }


    @Test
    public void testDataDirIsMissing()
        throws Exception
    {
        File f = new File(myFolder, "no file");
        assertFalse(f.exists());

        run(1, "report_coverage", f.getPath(), reportDir().getPath());

        assertThat(stderrText, containsString("not a directory"));
        assertThat(stderrText, containsString(f.getPath()));
    }

    @Test
    public void testDataDirIsFile()
        throws Exception
    {
        String f = plainFile().getPath();

        run(1, "report_coverage", f, reportDir().getPath());

        assertThat(stderrText, containsString("not a directory"));
        assertThat(stderrText, containsString(f));
    }

    @Test
    public void testReportDirIsFile()
        throws Exception
    {
        String f = plainFile().getPath();

        run(1, "report_coverage", dataDir().getPath(), f);

        assertThat(stderrText, containsString("not a directory"));
        assertThat(stderrText, containsString(f));
    }


    @Test
    public void testCoverCompletionMessage()
        throws Exception
    {
        String dataDir   = dataDir().getPath();
        String reportDir = reportDir().getPath();

        // I'm surprised this works without any coverage data!
        run(0, "report_coverage", dataDir, reportDir);
        assertThat(stdoutText,
                   containsString("Wrote Fusion coverage report to " + reportDir));
        assertThat(stderrText, isEmptyString());

        assertTrue(new File(reportDir, "index.html").isFile());
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
