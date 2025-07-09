// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

/**
 * NOT FOR APPLICATION USE
 */
public final class DocGenerator
{
    /** HTML content for the masthead links */
    static final String HEADER_LINKS =
        "<div class='indexlink'>" +
        "<a href='index.html'>Top</a> " +
        "<a href='binding-index.html'>Binding Index</a> " +
        "(<a href='permuted-index.html'>Permuted</a>)" +
        "</div>\n";

    private DocGenerator() {}


    public static void writeHtmlTree(FusionRuntime runtime,
                                     File outputDir,
                                     File repoDir,
                                     Predicate<ModuleIdentity> filter)
        throws IOException, FusionException
    {
        log("Building module docs");
        RepoEntity repo = new RepoEntity(repoDir.toPath(), filter, runtime.makeTopLevel());

        log("Writing module docs");
        writeModules(repo.getModules(), filter, outputDir);

        log("Building indices");
        DocIndex index = buildDocIndex(repo.getModules());

        log("Writing indices");
        writeIndexFile(filter, outputDir, index);
        writePermutedIndexFile(filter, outputDir, index);

        log("Writing Markdown pages");
        // TODO Path extension is messy magic.
        writeMarkdownPages(outputDir, ".", new File(repoDir, "src"));

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void writeModules(Set<ModuleEntity> modules,
                                     Predicate<ModuleIdentity> filter,
                                     File siteDir)
        throws IOException
    {
        for (ModuleEntity module : modules)
        {
            ModuleIdentity id = module.getIdentity();

            StringBuilder baseUrl   = new StringBuilder(".");
            File          moduleDir = siteDir;
            for (Iterator<String> i = id.iterate(); i.hasNext(); )
            {
                String name = i.next();

                if (i.hasNext())
                {
                    baseUrl.append("/..");
                    moduleDir = new File(moduleDir, name);
                }
            }

            writeModuleFile(filter, moduleDir, baseUrl.toString(), module);
        }
    }


    private static void writeModuleFile(Predicate<ModuleIdentity> filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleEntity doc)
        throws IOException
    {
        ModuleIdentity id = doc.getIdentity();
        File outputFile = new File(outputDir, id.baseName() + ".html");

        try (ModuleWriter writer =
                 new ModuleWriter(filter, outputFile, baseUrl, doc))
        {
            writer.renderModule();
        }
    }


    private static void writeIndexFile(Predicate<ModuleIdentity> filter,
                                       File outputDir,
                                       DocIndex index)
        throws IOException
    {
        File outputFile = new File(outputDir, "binding-index.html");

        try (IndexWriter writer = new IndexWriter(filter, outputFile))
        {
            writer.renderIndex(index);
        }
    }


    private static void writePermutedIndexFile(Predicate<ModuleIdentity> filter,
                                               File outputDir,
                                               DocIndex index)
        throws IOException
    {
        File outputFile = new File(outputDir, "permuted-index.html");

        try (PermutedIndexWriter writer =
                 new PermutedIndexWriter(filter, index, outputFile))
        {
            writer.renderIndex();
        }
    }


    /**
     * Recursively discover {@code .md} files and transform to {@code .html}.
     */
    private static void writeMarkdownPages(File   outputDir,
                                           String baseUrl,
                                           File   repoDir)
        throws IOException
    {
        String[] fileNames = repoDir.list();

        for (String fileName : fileNames)
        {
            File repoFile = new File(repoDir, fileName);

            if (fileName.endsWith(".md"))
            {
                String docName = fileName.substring(0, fileName.length() - 2);
                File outputFile = new File(outputDir, docName + "html");

                try (MarkdownPageWriter writer =
                         new MarkdownPageWriter(outputFile.toPath(), baseUrl, repoFile.toPath()))
                {
                    writer.render();
                }
            }
            else if (repoFile.isDirectory())
            {
                File subOutputDir = new File(outputDir, fileName);
                writeMarkdownPages(subOutputDir, baseUrl + "/..", repoFile);
            }
        }
    }


    private static void log(String message)
    {
        System.out.print(Timestamp.now());
        System.out.print(" ");
        System.out.println(message);
    }
}
