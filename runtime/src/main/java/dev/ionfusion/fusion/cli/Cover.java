// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import dev.ionfusion.runtime._private.cover.CoverageConfiguration;
import dev.ionfusion.runtime._private.cover.CoverageDatabase;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;

/**
 *
 */
class Cover
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Generate a code coverage report.";
    private static final String HELP_USAGE =
        "report_coverage [--configFile FILE] COVERAGE_DATA_DIR REPORT_DIR";
    private static final String HELP_BODY =
        "Reads Fusion code-coverage data from the COVERAGE_DATA_DIR, then writes an\n" +
        "HTML report to the REPORT_DIR.";


    Cover()
    {
        super("report_coverage");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    Object makeOptions(GlobalOptions globals)
    {
        return new Options();
    }

    private class Options
    {
        private Path myConfigFile;

        public void setConfigFile(Path configFile)
            throws UsageException
        {
            if (!isRegularFile(configFile) || !isReadable(configFile))
            {
                throw usage("--configFile is not a readable file: " + configFile);
            }
            myConfigFile = configFile;
        }
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, Object locals, String[] args)
        throws UsageException
    {
        if (args.length != 2) return null;

        String dataPath = args[0];
        if (dataPath.isEmpty()) return null;

        // TODO Support multiple data directories, to generate an aggregate
        //   report from a few test suites, Gradle subprojects, etc.
        File dataDir = new File(dataPath);
        if (! dataDir.isDirectory())
        {
            throw usage("Coverage data directory is not a directory: " + dataPath);
        }

        String reportPath = args[1];
        if (reportPath.isEmpty()) return null;

        File reportDir = new File(reportPath);
        if (reportDir.exists() && ! reportDir.isDirectory())
        {
            throw usage("Report directory is not a directory: " + reportPath);
        }

        return new Executor(globals, (Options) locals, dataDir, reportDir);
    }


    static class Executor
        extends StdioExecutor
    {
        private final Options myLocals;
        private final File myDataDir;
        private final File myReportDir;

        private Executor(GlobalOptions globals, Options locals, File dataDir, File reportDir)
        {
            super(globals);
            myLocals = locals;

            myDataDir   = dataDir;
            myReportDir = reportDir;
        }

        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            CoverageConfiguration config;
            if (myLocals.myConfigFile != null)
            {
                config = CoverageConfiguration.forConfigFile(myLocals.myConfigFile);
            }
            else
            {
                config = CoverageConfiguration.forDataDir(myDataDir.toPath());
            }

            CoverageDatabase database = new CoverageDatabase(myDataDir.toPath());

            CoverageReportWriter renderer = new CoverageReportWriter(config, database);

            Path index = renderer.renderFullReport(myReportDir);

            out.print("Wrote Fusion coverage report to ");
            out.println(index.toUri());

            return 0;
        }
    }
}
