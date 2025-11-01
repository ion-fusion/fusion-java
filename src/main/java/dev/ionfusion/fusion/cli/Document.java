// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static java.nio.file.Files.isDirectory;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.tool.RepoEntity;
import dev.ionfusion.fusion._private.doc.tool.SiteBuilder;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Consumer;
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

        // We don't want this documented yet since it's not stable.
//        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }



    private class Options
    {
        private Path myModulesDir;
        private Path myArticlesDir;
        private Path myAssetsDir;

        public void setModules(Path dir)
            throws UsageException
        {
            if (! isDirectory(dir))
            {
                throw usage("--modules is not a directory: " + dir);
            }
            if (! isDirectory(dir.resolve("src")))
            {
                throw usage("--modules has no src directory: " + dir);
            }
            myModulesDir = dir;
        }

        public void setArticles(Path path)
            throws UsageException
        {
            if (! isDirectory(path))
            {
                throw usage("--articles is not a directory: " + path);
            }
            myArticlesDir = path;
        }

        public void setAssets(Path path)
            throws UsageException
        {
            if (! isDirectory(path))
            {
                throw usage("--assets is not a directory: " + path);
            }
            myAssetsDir = path;
        }
    }

    Object makeOptions(GlobalOptions globals)
    {
        return new Options();
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, Object locals, String[] args)
        throws UsageException
    {
        Options options = (Options) locals;

        if (args.length != 1) throw usage("Wrong number of arguments");

        File outputDir = new File(args[0]);
        if (outputDir.isFile())
        {
            throw usage("Output location is a file: " + outputDir);
        }

        globals.collectDocumentation();

        // TODO This may not be sufficient, if this repo depends on others.
        Path repoDir = options.myModulesDir;
        globals.setRepositories(repoDir.toAbsolutePath().toString());

        return new Executor(globals, options, outputDir);
    }


    private static class Executor
        extends FusionExecutor
    {
        private final Options myOptions;
        private final File    myOutputDir;

        private Executor(GlobalOptions globals, Options options, File outputDir)
        {
            super(globals);

            myOptions   = options;
            myOutputDir = outputDir;
        }

        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws Exception
        {
            FusionRuntime runtime = runtime();

            Consumer<String> log = message -> {
                err.print(Timestamp.now());
                err.print(" ");
                err.println(message);
            };

            Predicate<ModuleIdentity> filter = id -> {
                String path = id.absolutePath();
                boolean isPrivate = path.endsWith("/private") || path.contains("/private/");
                return !isPrivate;
            };

            Path repoDir = myOptions.myModulesDir;
            log.accept("Building module docs");
            RepoEntity  repo = new RepoEntity(repoDir, filter, runtime.makeTopLevel());
            SiteBuilder site = new SiteBuilder(repo, filter);

            log.accept("Discovering module docs");
            site.placeModules();

            Path articles = myOptions.myArticlesDir;
            if (articles != null)
            {
                log.accept("Discovering Markdown pages");
                site.placeArticles(articles);
            }

            Path assets = myOptions.myAssetsDir;
            if (assets != null)
            {
                log.accept("Discovering static assets");
                site.placeAssets(assets);
            }

            log.accept("Building indices");
            site.prepareIndexes();

            log.accept("Writing HTML pages");
            site.build().generate(myOutputDir.toPath());

            log.accept("DONE writing HTML docs to " + myOutputDir);
            return 0;
        }
    }
}
