// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import static dev.ionfusion.fusion._private.doc.model.DocTreeNode.buildDocTree;
import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;

import com.amazon.ion.Timestamp;
import com.petebevin.markdown.MarkdownProcessor;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.doc.model.DocTreeNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        DocTreeNode doc = buildDocTree(runtime, filter, repoDir);

        log("Writing module docs");
        writeModuleTree(filter, outputDir, ".", doc);

        log("Building indices");
        DocIndex index = buildDocIndex(doc);

        log("Writing indices");
        writeIndexFile(filter, outputDir, index);
        writePermutedIndexFile(filter, outputDir, index);

        log("Writing Markdown pages");
        writeMarkdownPages(outputDir, ".", repoDir);

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void writeModuleTree(Predicate<ModuleIdentity> filter,
                                        File outputDir,
                                        String baseUrl,
                                        DocTreeNode doc)
        throws IOException
    {
        String name = doc.baseName();
        if (name != null)
        {
            writeModuleFile(filter, outputDir, baseUrl, doc);
            outputDir = new File(outputDir, name);
            baseUrl = baseUrl + "/..";
        }

        for (DocTreeNode submodule : doc.submodules())
        {
            writeModuleTree(filter, outputDir, baseUrl, submodule);
        }
    }


    private static void writeModuleFile(Predicate<ModuleIdentity> filter,
                                        File outputDir,
                                        String baseUrl,
                                        DocTreeNode doc)
        throws IOException
    {
        File outputFile = new File(outputDir, doc.baseName() + ".html");

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


    //========================================================================


    private static final String TITLE_REGEX =
        "^#\\s+(\\p{Print}+)\\s*$";
    private static final Pattern TITLE_PATTERN =
        Pattern.compile(TITLE_REGEX, Pattern.MULTILINE);

    /**
     * Transforms a single Markdown file into HTML.
     * <p>
     * The page title is taken from the first H1, assumed to be authored using
     * the atx syntax: {@code # <Title content>}.
     */
    private static void writeMarkdownPage(File   outputFile,
                                          String baseUrl,
                                          Path   inputFile)
        throws IOException
    {
        // TODO Java11: use Files.readString
        byte[] bytes = Files.readAllBytes(inputFile);
        String markdownContent = new String(bytes, UTF8);

        Matcher matcher = TITLE_PATTERN.matcher(markdownContent);
        String title =
            (matcher.find() ? matcher.group(1) : "Fusion Documentation");

        final MarkdownProcessor markdowner = new MarkdownProcessor();
        String html = markdowner.markdown(markdownContent);

        try (HtmlWriter writer = new HtmlWriter(outputFile))
        {
            writer.openHtml();
            {
                writer.renderHead(title, baseUrl, "common.css", "doc.css");
                writer.openBody();
                {
                    writer.append(HEADER_LINKS);
                    writer.append(html);
                }
                writer.closeBody();
            }
            writer.closeHtml();
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
                writeMarkdownPage(outputFile, baseUrl, repoFile.toPath());
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
