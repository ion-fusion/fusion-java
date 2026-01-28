// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import java.io.File;
import java.io.PrintWriter;

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
        "report_coverage COVERAGE_DATA_DIR REPORT_DIR";
    private static final String HELP_BODY =
        "Reads Fusion code-coverage data from the COVERAGE_DATA_DIR, then writes an\n" +
        "HTML report to the REPORT_DIR.";


    Cover()
    {
        super("report_coverage");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 2) return null;

        String dataPath = args[0];
        if (dataPath.isEmpty()) return null;

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

        return new Executor(globals, dataDir, reportDir);
    }


    static class Executor
        extends StdioExecutor
    {
        private final File myDataDir;
        private final File myReportDir;

        private Executor(GlobalOptions globals, File dataDir, File reportDir)
        {
            super(globals);

            myDataDir   = dataDir;
            myReportDir = reportDir;
        }

        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            CoverageReportWriter renderer = new CoverageReportWriter(myDataDir);

            renderer.renderFullReport(myReportDir);

            out.print("Wrote Fusion coverage report to ");
            out.println(myReportDir.getPath());

            return 0;
        }
    }
}
