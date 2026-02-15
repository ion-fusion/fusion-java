// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import dev.ionfusion.runtime._private.cover.CoverageConfiguration;
import dev.ionfusion.runtime._private.cover.CoverageDatabase;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        "report_coverage [--configFile FILE] --htmlDir REPORT_DIR DATA_DIR ...";
    private static final String HELP_BODY =
        "Reads Fusion code-coverage data from the DATA_DIRs, then writes an\n" +
        "HTML report to the REPORT_DIR.\n" +
        "\n" +
        "Multiple data directories can be given, generating an aggregate report.";


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
        private Path myHtmlDir;

        public void setConfigFile(Path configFile)
            throws UsageException
        {
            if (!isRegularFile(configFile) || !isReadable(configFile))
            {
                throw usage("--configFile is not a readable file: " + configFile);
            }
            myConfigFile = configFile;
        }

        public void setHtmlDir(Path dir)
            throws UsageException
        {
            if (exists(dir) && !isDirectory(dir))
            {
                throw usage("--htmlDir is not a directory: " + dir);
            }
            myHtmlDir = dir;
        }
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, Object locals, String[] args)
        throws UsageException
    {
        if (args.length == 0) return null;

        List<Path> dataDirs = new ArrayList<>();
        for (String dataPath : args)
        {
            // Avoid resolving to the current directory.
            if (dataPath.isEmpty()) return null;

            Path dataDir = Paths.get(dataPath);
            if (!isDirectory(dataDir) || !isReadable(dataDir))
            {
                throw usage("Coverage data directory is not a readable directory: " + dataDir);
            }
            dataDirs.add(dataDir);
        }

        Options options = (Options) locals;

        if (options.myHtmlDir == null)
        {
            throw usage("No HTML output directory given; provide with --htmlDir");
        }

        if (options.myConfigFile == null && dataDirs.size() > 1)
        {
            throw usage("Must provide --configFile when generating an aggregate report");
        }

        return new Executor(globals, options, dataDirs);
    }


    static class Executor
        extends StdioExecutor
    {
        private final Options    myLocals;
        private final List<Path> myDataDirs;
        private final Path       myReportDir;

        private Executor(GlobalOptions globals, Options locals, List<Path> dataDirs)
        {
            super(globals);
            myLocals = locals;
            myDataDirs = dataDirs;
            myReportDir = locals.myHtmlDir;
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
                assert myDataDirs.size() == 1; // Checked in makeExecutor()
                config = CoverageConfiguration.forDataDir(myDataDirs.get(0));
            }

            CoverageDatabase database = new CoverageDatabase();
            for (Path dataDir : myDataDirs)
            {
                database.loadSessions(dataDir);
            }

            CoverageReportWriter renderer = new CoverageReportWriter(config, database);

            Path index = renderer.renderFullReport(myReportDir);

            out.print("Wrote Fusion coverage report to ");
            out.println(index.toUri());

            return 0;
        }
    }
}
