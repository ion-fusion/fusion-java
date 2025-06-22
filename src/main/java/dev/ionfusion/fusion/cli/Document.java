// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static dev.ionfusion.fusion._private.doc.tool.DocGenerator.writeHtmlTree;

import dev.ionfusion.fusion.ModuleIdentity;
import java.io.File;
import java.io.PrintWriter;
import java.util.function.Predicate;


class Document
    extends Command
{
    //=+===============================================================================
    @SuppressWarnings("unused")
    private static final String HELP_ONE_LINER =
        "Generate reference documentation for a repository.";

    @SuppressWarnings("unused")
    private static final String HELP_USAGE =
        "document OUTPUT_DIR REPO_DIR";

    @SuppressWarnings("unused")
    private static final String HELP_BODY =
        "Given a REPO_DIR directory containing Fusion source code, generate reference\n" +
        "documentation (in HTML format) into the OUTPUT_DIR.";


    //=========================================================================
    // Constructors

    Document()
    {
        super("document");

        // We don't want this documented yet since its not stable.
//        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 2) return null;

        File outputDir = new File(args[0]);
        File repoDir   = new File(args[1]);

        if (outputDir.isFile())
        {
            throw usage("Output location is a file: " + outputDir);
        }

        if (! repoDir.isDirectory())
        {
            throw usage("Repository is not a directory: " + repoDir);
        }

        if (! new File(repoDir, "src").isDirectory())
        {
            throw usage("Repository has no src directory: " + repoDir);
        }

        globals.collectDocumentation();
        globals.setRepositories(repoDir.getAbsolutePath());

        return new Executor(globals, outputDir, repoDir);
    }


    private static class Executor
        extends FusionExecutor
    {
        private final File myOutputDir;
        private final File myRepoDir;

        private Executor(GlobalOptions globals, File outputDir, File repoDir)
        {
            super(globals);

            myOutputDir = outputDir;
            myRepoDir   = repoDir;
        }

        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            // TODO send log messages to the output PrintWriter, not System.out
            File srcDir = new File(myRepoDir, "src");

            Predicate<ModuleIdentity> filter = id -> {
                String path = id.absolutePath();
                boolean isPrivate = path.endsWith("/private") || path.contains("/private/");
                return !isPrivate;
            };

            writeHtmlTree(runtime(), myOutputDir, srcDir, filter);
            return 0;
        }
    }
}
