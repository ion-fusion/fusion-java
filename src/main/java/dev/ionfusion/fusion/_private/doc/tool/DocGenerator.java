// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import java.io.File;
import java.io.IOException;
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
        SiteBuilder site = new SiteBuilder(repo, filter);

        log("Discovering module docs");
        site.placeModules();

        log("Building indices");
        DocIndex index = buildDocIndex(repo.getModules());

        log("Writing indices");
        writeIndexFile(filter, outputDir, index);
        writePermutedIndexFile(filter, outputDir, index);

        log("Writing Markdown pages");
        // TODO Path extension is messy magic.
        writeMarkdownPages(outputDir, ".", new File(repoDir, "src"));

        log("Writing HTML pages");
        site.build().generate(outputDir.toPath());

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void writeIndexFile(Predicate<ModuleIdentity> filter,
                                       File outputDir,
                                       DocIndex index)
        throws IOException
    {
        try (StreamWriter writer = new StreamWriter(outputDir, "binding-index.html"))
        {
            new IndexWriter(filter, writer).renderIndex(index);
        }
    }


    private static void writePermutedIndexFile(Predicate<ModuleIdentity> filter,
                                               File outputDir,
                                               DocIndex index)
        throws IOException
    {
        try (StreamWriter writer = new StreamWriter(outputDir, "permuted-index.html"))
        {
            new PermutedIndexWriter(filter, index, writer).renderIndex();
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

                try (StreamWriter writer = new StreamWriter(outputDir, docName + "html"))
                {
                    new MarkdownPageWriter(writer, baseUrl, repoFile.toPath()).render();
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
