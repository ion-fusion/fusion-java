// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import static dev.ionfusion.fusion.DocIndex.buildDocIndex;
import static dev.ionfusion.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import static dev.ionfusion.fusion.ModuleDoc.buildDocTree;

import com.amazon.ion.Timestamp;
import com.petebevin.markdown.MarkdownProcessor;
import dev.ionfusion.fusion.ModuleDoc.Filter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOT FOR APPLICATION USE
 */
public final class _Private_ModuleDocumenter
{
    private _Private_ModuleDocumenter() {}


    public static void writeHtmlTree(FusionRuntime runtime,
                                     File outputDir,
                                     File repoDir)
        throws IOException, FusionException
    {
        Filter filter = new Filter();

        log("Building module docs");
        ModuleDoc doc = buildDocTree(runtime, filter, repoDir);

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


    private static void writeModuleTree(Filter filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleDoc doc)
        throws IOException
    {
        String name = doc.baseName();
        if (name != null)
        {
            writeModuleFile(filter, outputDir, baseUrl, doc);
            outputDir = new File(outputDir, name);
            baseUrl = baseUrl + "/..";
        }

        Collection<ModuleDoc> submodules = doc.submodules();
        for (ModuleDoc submodule : submodules)
        {
            writeModuleTree(filter, outputDir, baseUrl, submodule);
        }
    }


    private static void writeModuleFile(Filter filter,
                                        File outputDir,
                                        String baseUrl,
                                        ModuleDoc doc)
        throws IOException
    {
        File outputFile = new File(outputDir, doc.baseName() + ".html");

        try (ModuleWriter writer =
                 new ModuleWriter(filter, outputFile, baseUrl, doc))
        {
            writer.renderModule();
        }
    }


    private static void writeIndexFile(Filter filter,
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


    private static void writePermutedIndexFile(Filter filter,
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

    private static void writeMarkdownPage(File   outputFile,
                                          String baseUrl,
                                          File   inputFile)
        throws IOException
    {
        String markdownContent;
        try (FileInputStream inStream = new FileInputStream(inputFile))
        {
            try (Reader inReader = new InputStreamReader(inStream, UTF8))
            {
                markdownContent = FusionUtils.loadReader(inReader);
            }
        }

        Matcher matcher = TITLE_PATTERN.matcher(markdownContent);
        String title =
            (matcher.find() ? matcher.group(1) : "Fusion Documentation");

        final MarkdownProcessor markdowner = new MarkdownProcessor();
        String html = markdowner.markdown(markdownContent);

        try (HtmlWriter writer = new HtmlWriter(outputFile))
        {
            writer.renderHead(title, baseUrl, null /*style*/);
            writer.append(html);
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
                writeMarkdownPage(outputFile, baseUrl, repoFile);
            }
            else if (repoFile.isDirectory())
            {
                File subOutputDir = new File(outputDir, fileName);
                writeMarkdownPages(subOutputDir, baseUrl + "/..", repoFile);
            }
        }
    }


    //========================================================================


    private static final class ModuleWriter
        extends HtmlWriter
    {
        private final Filter            myFilter;
        private final String            myBaseUrl;
        private final ModuleDoc         myDoc;
        private final ModuleIdentity    myModuleId;
        private final MarkdownProcessor myMarkdown = new MarkdownProcessor();

        public ModuleWriter(Filter    filter,
                            File      outputFile,
                            String    baseUrl,
                            ModuleDoc doc)
            throws IOException
        {
            super(outputFile);
            myFilter  = filter;
            myBaseUrl = baseUrl;
            myDoc = doc;
            myModuleId = doc.myModuleId;
        }

        void renderModule()
            throws IOException
        {
            openHtml();
            {
                String modulePath = myModuleId.absolutePath();
                renderHead(modulePath, myBaseUrl, "module.css");

                openBody();
                {
                    renderHeader();
                    renderModuleIntro();
                    renderSubmoduleLinks();
                    renderBindings();
                }
                closeBody();
            }
            closeHtml();
        }


        private void renderModulePathWithLinks(ModuleIdentity id)
            throws IOException
        {
            ModuleIdentity parent = id.parent();
            if (parent != null)
            {
                renderModulePathWithLinks(parent);
            }

            append('/');

            String baseName = id.baseName();

            if (id == myModuleId)
            {
                // Don't link to ourselves, that's silly.
                append(baseName);
            }
            else
            {
                linkToModule(id, baseName);
            }
        }

        private void renderHeader()
            throws IOException
        {
            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='binding-index.html'>Binding Index</a> " +
                   "(<a href='permuted-index.html'>Permuted</a>)" +
                   "</div>\n");

            append("<h1>Module ");
            renderModulePathWithLinks(myModuleId);
            append("</h1>");
        }

        private void renderModuleIntro()
            throws IOException
        {
            if (myDoc.myIntroDocs != null)
            {
                markdown(myDoc.myIntroDocs);
            }
        }

        private void renderSubmoduleLinks()
            throws IOException
        {
            Map<String, ModuleDoc> submodules = myDoc.submoduleMap();
            if (submodules == null) return;

            renderHeader2("Submodules");

            String[] names = submodules.keySet().toArray(EMPTY_STRING_ARRAY);
            Arrays.sort(names);

            append("<ul class='submodules'>");
            for (String name : names)
            {
                ModuleDoc sub = submodules.get(name);

                String escapedName = escapeString(name);
                append("<li>");
                linkToModule(sub.myModuleId, escapedName);

                String oneLiner = sub.oneLiner();
                if (oneLiner != null)
                {
                    append(" &ndash; <span class='oneliner'>");
                    markdown(oneLiner);
                    append("</span>");
                }
                append("</li>\n");
            }
            append("</ul>\n");
        }


        private void renderBindingIndex(String[] names)
            throws IOException
        {
            if (names.length == 0) return;

            append("<div class='exports'>\n");
            for (String name : names)
            {
                String escapedName = escapeString(name);
                linkToBindingAsName(myModuleId, escapedName);
                append("&nbsp;&nbsp;\n");
            }
            append("</div>\n");
        }


        private void renderBindings()
            throws IOException
        {
            Map<String, BindingDoc> bindings = myDoc.bindingMap();
            if (bindings == null) return;

            renderHeader2("Exported Bindings");

            String[] names = myDoc.sortedExportedNames();

            renderBindingIndex(names);

            for (String name : names)
            {
                // May be null:
                BindingDoc binding = bindings.get(name);
                renderBinding(name, binding);
            }
        }


        /* CSS hierarchy:
         *
         *  binding
         *    name
         *    kind
         *    doc
         *      oneliner -- presently unused, intended to be text description
         *      body
         *      also
         */
        private void renderBinding(String name, BindingDoc doc)
            throws IOException
        {
            String escapedName = escapeString(name);

            append("\n<div class='binding' id='");
            append(escapedName);
            append("'><span class='name'>");
            append(escapedName);
            append("</a></span>");   // binding div is still open

            if (doc == null)
            {
                append("<p class='nodoc'>No documentation available.<p>\n\n");
            }
            else
            {
                if (doc.getKind() != null)
                {
                    append(" <span class='kind'>");
                    // Using enum toString() allows display name to be changed
                    append(doc.getKind().toString().toLowerCase());
                    append("</span>\n");
                }

                append("<div class='doc'>");

                if (doc.getUsage() != null || doc.getBody() != null)
                {
                    StringBuilder buf = new StringBuilder();

                    if (doc.getUsage() != null)
                    {
                        buf.append("    ");
                        buf.append(doc.getUsage());
                        buf.append('\n');
                    }

                    if (doc.getBody() != null)
                    {
                        buf.append('\n');
                        buf.append(doc.getBody());
                        buf.append('\n');
                    }

                    append("<div class='body'>");
                    markdown(buf.toString());
                    append("</div>\n");
                }

                append('\n');


                ModuleIdentity[] ids =
                    doc.getProvidingModules().toArray(new ModuleIdentity[0]);
                Arrays.sort(ids);

                boolean printedOne = false;
                for (ModuleIdentity id : ids)
                {
                    if (id != myModuleId && myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        else
                        {
                            append("<p class='also'>Also provided by ");
                        }

                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }
                if (printedOne)
                {
                    append("</p>\n");
                }

                append("</div>\n"); // doc
            }

            append("</div>\n"); // binding
        }


        private void markdown(String text)
            throws IOException
        {
            String md = myMarkdown.markdown(text);
            append(md);
        }
    }


    //========================================================================


    private static final class IndexWriter
        extends HtmlWriter
    {
        private final Filter myFilter;


        public IndexWriter(Filter filter, File outputFile)
            throws IOException
        {
            super(outputFile);
            myFilter = filter;
        }


        void renderIndex(DocIndex index)
            throws IOException
        {
            renderHead("Fusion Binding Index", null, "index.css");

            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='permuted-index.html'>Permuted Index</a>" +
                   "</div>\n");

            renderHeader1("Binding Index");

            append("<table>");
            for (Entry<String, Set<ModuleIdentity>> entry
                    : index.getNameMap().entrySet())
            {
                String escapedName = escapeString(entry.getKey());
                append("<tr><td class='bound'>");
                append(escapedName);
                append("</td><td>");

                boolean printedOne = false;
                for (ModuleIdentity id : entry.getValue())
                {
                    if (myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }

                append("</td>\n");
            }
            append("</table>\n");
        }
    }


    //========================================================================


    private static final class PermutedIndexWriter
        extends HtmlWriter
    {
        private final Filter   myFilter;
        private final DocIndex myIndex;

        /** Maps keywords to the lines in which they exist. */
        private final TreeSet<Line> myLines;


        /** An index line. */
        private static final class Line
            implements Comparable<Line>
        {
            private final String myPrefix;
            private final String myKeyword;
            private final Entry<String, Set<ModuleIdentity>> myEntry;


            Line(Entry<String, Set<ModuleIdentity>> entry,
                 int keywordStartPos,
                 int keywordLimitPos)
            {
                String name = entry.getKey();
                myPrefix  = name.substring(0, keywordStartPos);
                myKeyword = name.substring(keywordStartPos, keywordLimitPos);
                myEntry   = entry;
            }

            String bindingName()
            {
                return myEntry.getKey();
            }

            String prefix()
            {
                return myPrefix;
            }

            String keyword()
            {
                return myKeyword;
            }

            String suffix()
            {
                int pos = myPrefix.length() + myKeyword.length();
                return bindingName().substring(pos);
            }

            Set<ModuleIdentity> modules()
            {
                return myEntry.getValue();
            }

            @Override
            public int compareTo(Line that)
            {
                int result = myKeyword.compareTo(that.myKeyword);
                if (result == 0)
                {
                    result = myPrefix.compareTo(that.myPrefix);
                    if (result == 0)
                    {
                        // We shouldn't get this far often, so we spend time to
                        // get the suffix rather that memory to cache it.
                        result = suffix().compareTo(that.suffix());
                    }
                }
                return result;
            }
        }


        public PermutedIndexWriter(Filter filter, DocIndex index,
                                   File outputFile)
            throws IOException
        {
            super(outputFile);

            myFilter = filter;
            myIndex  = index;
            myLines  = new TreeSet<>();
        }


        private void permute()
        {
            for (Entry<String, Set<ModuleIdentity>> entry
                     : myIndex.getNameMap().entrySet())
            {
                String name = entry.getKey();

                int pos = 0;
                while (true)
                {
                    int underscorePos = name.indexOf('_', pos);
                    if (underscorePos == -1)
                    {
                        myLines.add(new Line(entry, pos, name.length()));
                        break;
                    }
                    else if (pos < underscorePos)
                    {
                        myLines.add(new Line(entry, pos, underscorePos));
                    }
                    pos = underscorePos + 1;
                }
            }
        }


        void renderIndex()
            throws IOException
        {
            permute();

            renderHead("Fusion Binding Index (Permuted)", null, "index.css");

            append("<div class='indexlink'>" +
                   "<a href='index.html'>Top</a> " +
                   "<a href='binding-index.html'>Alphabetical Index</a>" +
                   "</div>\n");

            renderHeader1("Permuted Binding Index");

            append("<table>");
            for (Line line : myLines)
            {
                String escapedName = escapeString(line.bindingName());

                append("<tr><td class='prefix'>");
                escape(line.prefix());
                append("</td><td class='tail'><span class='keyword'>");
                escape(line.keyword());
                append("</span>");
                escape(line.suffix());
                append("</td><td>");

                boolean printedOne = false;
                for (ModuleIdentity id : line.modules())
                {
                    if (myFilter.accept(id))
                    {
                        if (printedOne)
                        {
                            append(", ");
                        }
                        linkToBindingAsModulePath(id, escapedName);
                        printedOne = true;
                    }
                }

                append("</td>\n");
            }
            append("</table>\n");
        }
    }


    private static void log(String message)
    {
        System.out.print(Timestamp.now());
        System.out.print(" ");
        System.out.println(message);
    }
}
