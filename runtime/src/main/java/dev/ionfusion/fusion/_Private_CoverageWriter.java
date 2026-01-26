// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.CoverageDatabase.SRCLOC_COMPARE;
import static dev.ionfusion.fusion._Private_Trampoline.discoverModulesInRepository;
import static dev.ionfusion.runtime.base.SourceName.FUSION_SOURCE_EXTENSION;
import static java.math.RoundingMode.HALF_EVEN;
import static java.nio.file.Files.walkFileTree;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.OffsetSpan;
import com.amazon.ion.Span;
import com.amazon.ion.SpanProvider;
import com.amazon.ion.Timestamp;
import com.amazon.ion.system.IonReaderBuilder;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class _Private_CoverageWriter
{
    private final static String CSS =
        "body { color:black }" +
        ".uncovered { color:red }" +
        "table.percentgraph { border: 0px;font-size: 130%;margin: 0px;margin-left: auto; margin-right: 0px;" +
        "padding: 0px; cellpadding=\"0px\" cellspacing=\"0px\"}" +
        "table.percentgraph tr.percentgraph { border: 0px;margin: 0px;padding: 0px;}" +
        "table.percentgraph td.percentgraph { border: 0px;margin: 0px;padding: 0px;padding-left: 4px;}" +
        "table.percentgraph td.percentgraphright { align=\"right\"; border: 0px;margin: 0px;padding: 0px;padding-left: 4px; width=\"40\"}" +
        "div.percentgraph { background-color: #f02020;border: #808080 1px solid;height: 1.3em;margin: 0px;padding: 0px;width: 100px;}" +
        "div.percentgraph div.greenbar { background-color: #00f000;height: 1.3em;margin: 0px;padding: 0px;}" +
        "div.percentgraph div.na { background-color: #eaeaea;height: 1.3em;margin: 0px;padding: 0px;}" +
        "div.percentgraph span.text { display: block;position: absolute;text-align: center;width: 100px;}" +
        "table.report { border-collapse: collapse;width: 100%;}" +
        "table.report td { border: #d0d0d0 1px solid;}" +
        "table.report td.heading {background: #dcecff;font-weight: bold;text-align: center;}" +
        "table.report td.value { text-align: right;}" +
        "table tr td, table tr th {font-size: 68%;}" +
        "td.value table tr td {font-size: 11px;}" +
        "div.separator {height: 10px;}";


    private static class CoverageInfoPair
    {
        public long coveredExpressions;
        public long uncoveredExpressions;

        public CoverageInfoPair()
        {
            coveredExpressions   = 0;
            uncoveredExpressions = 0;
        }

        void foundExpression(boolean covered)
        {
            if (covered)
            {
                coveredExpressions++;
            }
            else
            {
                uncoveredExpressions++;
            }
        }

        public long total()
        {
            return coveredExpressions + uncoveredExpressions;
        }

        BigDecimal percentCovered()
        {
            final long total = total();

            if (total == 0) return BigDecimal.ZERO;

            BigDecimal numerator = new BigDecimal(coveredExpressions * 100);

            return numerator.divide(new BigDecimal(total), 2, HALF_EVEN);
        }

        void renderCoveragePercentage(HtmlWriter htmlWriter)
            throws IOException
        {
            htmlWriter.append(percentCovered().toString());
            htmlWriter.append("% expression coverage of ");
            htmlWriter.append(Long.toString(total()));
            htmlWriter.append(" expressions observed");
        }

        void renderTotal(HtmlWriter html)
            throws IOException
        {
            html.append(Long.toString(total()));
        }

        void renderPercentageGraph(HtmlWriter html)
            throws IOException
        {
            final BigDecimal percent = percentCovered();
            final int percentIntVal = percent.intValue();

            html.append("<table class='percentgraph'>"
                          + "<tr class='percentgraph'>"
                          + "<td class='percentgraphright'>");
            html.append(Integer.toString(percentIntVal));
            html.append("%</td>");
            html.append("<td class='percentgraph'>"
                          + "<div class='percentgraph'>"
                          + "<div class='greenbar' style='width:");
            html.append(Integer.toString(percentIntVal));
            html.append("px'><span class='text'>");
            html.append(Long.toString(coveredExpressions));
            html.append("/");
            renderTotal(html);
            html.append("</span></div></div></td></tr></table>");
        }
    }

    private static class EstimatedCoverageInfoPair
        extends CoverageInfoPair
    {
        @Override
        void renderTotal(HtmlWriter html)
            throws IOException
        {
            html.append("???");
        }
    }


    private final CoverageDatabase                  myDatabase;
    private final CoverageConfiguration             myConfig;
    private final Set<ModuleIdentity>               myModules;
    private final Set<File>                         mySourceFiles;
    private final Map<ModuleIdentity, SourceName>   myNamesForModules;
    private final Map<File, SourceName>             myNamesForFiles;
    private final Map<SourceName, CoverageInfoPair> myFileCoverages;
    private final Map<SourceName, String>           myRelativeNamesForSources;

    private final CoverageInfoPair myGlobalCoverage = new CoverageInfoPair();
    private       long             myUnloadedEntries;

    // For rendering highlighted source files
    private static final int BUFFER_SIZE = 2048;
    private final byte[] myCopyBuffer = new byte[BUFFER_SIZE];
    private long myIonBytesRead;
    private boolean coverageState;


    public _Private_CoverageWriter(File dataDir)
        throws FusionException, IOException
    {
        myDatabase = new CoverageDatabase(dataDir.toPath());
        myConfig   = new CoverageConfiguration(dataDir);

        myModules                 = new HashSet<>();
        mySourceFiles             = new HashSet<>();
        myNamesForModules         = new HashMap<>();
        myNamesForFiles           = new HashMap<>();
        myFileCoverages           = new HashMap<>();
        myRelativeNamesForSources = new HashMap<>();
    }


    //=========================================================================
    // Metrics Analysis

    /**
     * Populate {@link #mySourceFiles} set with all the {@code .fusion} files
     * within the configured source directories.
     */
    private void collectSourceFiles()
        throws IOException
    {
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path entry, BasicFileAttributes attrs)
            {
                if (entry.getFileName().toString().endsWith(FUSION_SOURCE_EXTENSION))
                {
                    if (myConfig.fileIsSelected(entry))
                    {
                        mySourceFiles.add(entry.toFile());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };

        for (Path dir : myConfig.getIncludedSourceDirs())
        {
            walkFileTree(dir, visitor);
        }
    }


    private Path commonPrefix(Path a, Path b)
    {
        int maxLen = Math.min(a.getNameCount(), b.getNameCount());
        for (int i = 0; i < maxLen; i++)
        {
            if (! a.getName(i).equals(b.getName(i)))
            {
                maxLen = i;
            }
        }

        // Path.subpath(0, 0) is illegal
        return (maxLen != 0
                    ? a.subpath(0, maxLen)
                    : a.getFileSystem().getPath(""));
    }

    /**
     * Determine the number of leading {@link Path} name elements common to all
     * files of the given {@link SourceName}s.  We omit this prefix from the
     * directory tree of generated HTML files.
     * <p>
     * TODO It would be better to use paths relative to repository roots, to
     *   avoid exposing details of the build-time environment.  This suggests
     *   that {@code SourceName} should track the repository holding the source.
     *
     * @param sourceNames must not be null.
     */
    private int commonPrefixLen(Set<SourceName> sourceNames)
        throws IOException
    {
        Path prefix = null;
        for (SourceName sourceName : sourceNames)
        {
            // Skip URL-based sources.
            File file = sourceName.getFile();
            if (file != null)
            {
                Path path   = file.getCanonicalFile().toPath();
                Path parent = path.getParent();
                if (prefix == null)
                {
                    prefix = parent;
                }
                else {
                    prefix = commonPrefix(prefix, parent);
                }
            }
        }
        return prefix == null ? 0 : prefix.getNameCount();
    }

    private void prepareRelativeNames()
        throws IOException
    {
        Set<SourceName> sourceNames = myDatabase.sourceNames();
        int prefixLen = commonPrefixLen(sourceNames);

        for (SourceName sourceName : sourceNames)
        {
            // TODO Determine this on demand, it's only needed twice per source.
            //      so this code is more complicated than its worth.
            File file = sourceName.getFile();
            if (file != null)
            {
                Path path        = file.getCanonicalFile().toPath();
                Path shorterPath = path.subpath(prefixLen, path.getNameCount());
                myRelativeNamesForSources.put(sourceName, shorterPath.toString());
            }
        }
    }


    /**
     * Collects relevant modules into {@link #myModules}, source files
     * into {@link #mySourceFiles}, and map covered modules and files to
     * {@link SourceName}s via {@link #myNamesForModules} and
     * {@link #myNamesForFiles}.
     * <p>
     * TODO Rename to be more informative; maybe collectModulesAndSources()?
     *
     * @throws FusionException if an error occurs.
     */
    private void analyze()
        throws FusionException, IOException
    {
        // Collect all the modules the repositories can discover, so we can find
        // modules that are not used and don't appear in the database.
        for (File f : myDatabase.getRepositories())
        {
            discoverModulesInRepository(f.toPath(), myConfig.myModuleSelector, myModules::add);
        }

        // Now do the same thing for non-repository source trees.
        // TODO Why aren't these dirs recorded in the database like modules?
        collectSourceFiles();

        for (SourceLocation loc : myDatabase.locations())
        {
            // TODO Assert that a name exists. There's no use in persisting
            //      nameless locations that we can't report.
            SourceName sourceName = loc.getSourceName();
            if (sourceName != null)
            {
                ModuleIdentity id = sourceName.getModuleIdentity();
                // TODO Why doesn't this use myConfig.moduleIsSelected(id) ?
                // TODO Why would this selector differ from the collection-time?
                if (id != null && myConfig.myModuleSelector.test(id))
                {
                    myModules.add(id);

                    SourceName prior = myNamesForModules.put(id, sourceName);
                    assert prior == null || prior == sourceName :
                        "SourceName instance has changed for module " + id;
                }
                else
                {
                    File f = sourceName.getFile();
                    if (f != null)
                    {
                        mySourceFiles.add(f);

                        SourceName prior = myNamesForFiles.put(f, sourceName);
                        assert prior == null || prior == sourceName :
                            "SourceName instance has changed for file " + f +
                                "\nThis can happen if you `load` a module's file " +
                                "within a registered repository.";
                    }
                }
            }
        }
    }


    private ModuleIdentity[] sortedModules()
    {
        ModuleIdentity[] modules = myModules.toArray(new ModuleIdentity[0]);
        Arrays.sort(modules);
        return modules;
    }

    private File[] sortedFiles()
    {
        File[] result = mySourceFiles.toArray(new File[0]);
        Arrays.sort(result);
        return result;
    }


    //=========================================================================
    // Report Rendering

    private void copySourceThroughOffset(HtmlWriter  htmlWriter,
                                         InputStream source,
                                         long        offset)
        throws IOException
    {
        long bytesToCopy = offset - myIonBytesRead;

        long bytesCopied = 0;
        while (bytesCopied < bytesToCopy)
        {
            int toRead = (int) Math.min(bytesToCopy - bytesCopied, BUFFER_SIZE);

            int bytesRead = source.read(myCopyBuffer, 0, toRead);

            if (bytesRead < 0) break; // EOF

            htmlWriter.write(myCopyBuffer, 0, bytesRead);
            bytesCopied += bytesRead;
        }

        myIonBytesRead += bytesCopied;
    }


    private void copySourceThroughCurrentOffset(HtmlWriter   htmlWriter,
                                                InputStream  source,
                                                SpanProvider spanProvider)
        throws IOException
    {
        Span span = spanProvider.currentSpan();
        OffsetSpan offsetSpan = span.asFacet(OffsetSpan.class);
        long offset = offsetSpan.getStartOffset();
        copySourceThroughOffset(htmlWriter, source, offset);
    }


    private void setCoverageState(HtmlWriter   htmlWriter,
                                  InputStream  source,
                                  SpanProvider spanProvider,
                                  boolean      covered)
        throws IOException
    {
        if (covered != coverageState)
        {
            copySourceThroughCurrentOffset(htmlWriter, source, spanProvider);

            htmlWriter.append("</span><span class='");
            if (! covered)
            {
                htmlWriter.append("un");
            }
            htmlWriter.append("covered'>");
        }

        coverageState = covered;
    }

    private InputStream readSource(SourceName source)
        throws IOException
    {
        URL url = source.getUrl();
        if (url != null) return url.openStream();

        return Files.newInputStream(source.getFile().toPath());
    }

    private void renderSource(HtmlWriter sourceHtml,
                              SourceName name)
        throws IOException
    {
        sourceHtml.renderHeadWithInlineCss("Fusion Code Coverage", CSS);
        {
            sourceHtml.append("<h1>");
            ModuleIdentity id = name.getModuleIdentity();
            if (id != null)
            {
                sourceHtml.append("Module ");
                sourceHtml.append(id.absolutePath());
                sourceHtml.append("</h1>\n");

                sourceHtml.append("at ");

                String path;
                File file = name.getFile();
                if (file != null)
                {
                    path = file.getAbsolutePath();
                }
                else
                {
                    // TODO improve this rendering
                    path = name.getUrl().toExternalForm();
                }
                sourceHtml.append(sourceHtml.escapeString(path));
            }
            else
            {
                sourceHtml.append("File ");
                sourceHtml.append(name.display());
                sourceHtml.append("</h1>\n");
            }
        }

        SourceLocation[] locations = myDatabase.sortedLocations(name);
        assert locations.length != 0;

        int locationIndex = 0;
        final CoverageInfoPair coverageInfoPair = new CoverageInfoPair();

        sourceHtml.append("\n<hr/>\n");
        sourceHtml.append("<pre>");

        // Copy the document in chunks separated by coverage state changes.
        // At each change, we insert appropriate HTML <span> markup.
        try (InputStream ionBytes = readSource(name))
        {
            myIonBytesRead = 0;

            try (IonReader ionReader = IonReaderBuilder.standard().build(readSource(name)))
            {
                SpanProvider spanProvider =
                    ionReader.asFacet(SpanProvider.class);

                // We always start with a span so we can always end with one,
                // regardless of the data in between.
                coverageState = false;
                sourceHtml.append("<span class='uncovered'>");

                for (IonType t = ionReader.next(); t != null; )
                {
                    // Determine whether this value has been covered.
                    SourceLocation currentLoc =
                        SourceLocation.forCurrentSpan(ionReader);

                    SourceLocation coverageLoc = locations[locationIndex];

                    // We shouldn't skip past a known location.
                    assert SRCLOC_COMPARE.compare(currentLoc, coverageLoc) <= 0;

                    if (SRCLOC_COMPARE.compare(currentLoc, coverageLoc) == 0)
                    {
                        boolean covered =
                            myDatabase.locationCovered(coverageLoc);
                        setCoverageState(sourceHtml, ionBytes, spanProvider,
                                         covered);
                        locationIndex++;

                        coverageInfoPair.foundExpression(covered);
                        myGlobalCoverage.foundExpression(covered);

                        if (locationIndex == locations.length) break;
                    }

                    if (IonType.isContainer(t))
                    {
                        ionReader.stepIn();
                    }

                    while ((t = ionReader.next()) == null
                           && ionReader.getDepth() != 0)
                    {
                        ionReader.stepOut();
                    }
                }

                assert locationIndex == locations.length
                    : "Not all locations were found in the source";
                assert locationIndex == coverageInfoPair.total()
                    : "Not all locations were counted";

                // Copy the rest of the Ion source.
                copySourceThroughOffset(sourceHtml, ionBytes, Long.MAX_VALUE);

                sourceHtml.append("</span>");
            }
        }

        sourceHtml.append("</pre>\n");
        sourceHtml.append("<hr/>");
        coverageInfoPair.renderCoveragePercentage(sourceHtml);

        myFileCoverages.put(name, coverageInfoPair);
    }


    private String relativeName(SourceName name)
    {
        String resource;
        if (name.getFile() != null)
        {
            resource = myRelativeNamesForSources.get(name);
        }
        else
        {
            URL url = name.getUrl();
            assert (url.getProtocol().equalsIgnoreCase("jar"));
            String path = url.getPath();
            int    bang = path.indexOf("!/");
            assert bang > 1;
            resource = path.substring(bang + 2);
            assert !resource.isEmpty();
            assert !resource.startsWith("/");
        }
        return resource + ".html";
    }


    private void renderSourceFiles(File outputDir)
        throws IOException
    {
        for (SourceName name : myDatabase.sourceNames())
        {
            String relativeName = relativeName(name);
            try (StreamWriter sourceHtml = new StreamWriter(outputDir, relativeName))
            {
                renderSource(new HtmlWriter(sourceHtml), name);
            }
        }
    }


    private <T> void renderRows(HtmlWriter         indexHtml,
                                String             category,
                                T[]                keys,
                                Map<T, SourceName> keyToNames)
        throws IOException
    {
        long totalExpressions = 0;
        long unloadedCount = 0;

        boolean first = true;
        for (T key : keys)
        {
            if (first)
            {
                indexHtml.append("<thead><tr>");
                indexHtml.append("<td class='heading'>Expression Coverage</td>");
                indexHtml.append("<td class='heading'>");
                indexHtml.append(category);
                indexHtml.append("</td>");
                indexHtml.append("</tr></thead>\n");
                first = false;
            }

            CoverageInfoPair pair;
            SourceName name = keyToNames.get(key);
            if (name != null)
            {
                pair = myFileCoverages.get(name);

                totalExpressions += pair.total();
            }
            else // The module was never loaded!
            {
                pair = new EstimatedCoverageInfoPair();

                unloadedCount++;
            }

            indexHtml.append("<tr><td>");
            pair.renderPercentageGraph(indexHtml);
            indexHtml.append("</td><td>");

            if (name != null)
            {
                // TODO Exclude the common prefix when key is a script File.
                indexHtml.append("<a href=\"" + relativeName(name) + "\">");
                indexHtml.append(key.toString());
                indexHtml.append("</a>");
            }
            else
            {
                indexHtml.append(key.toString());
            }

            indexHtml.append("</td></tr>\n");
        }

        if (unloadedCount != 0)
        {
            long loadedCount = keys.length - unloadedCount;
            long average = (loadedCount == 0
                               ? 500                        // TODO Totally made up
                               : totalExpressions / loadedCount);

            myGlobalCoverage.uncoveredExpressions += (unloadedCount * average);

            myUnloadedEntries += unloadedCount;
        }
    }


    private void renderIndex(File outputDir)
        throws IOException
    {
        try (StreamWriter out = new StreamWriter(outputDir, "index.html"))
        {
            HtmlWriter indexHtml = new HtmlWriter(out);
            indexHtml.renderHeadWithInlineCss("Fusion Code Coverage", CSS);

            indexHtml.append("<p>Report generated at ");
            indexHtml.append(Timestamp.now().toString());
            indexHtml.append("</p>\n");

            indexHtml.append("<table class='report'>\n");

            renderRows(indexHtml, "Module", sortedModules(), myNamesForModules);
            renderRows(indexHtml, "File",   sortedFiles(),   myNamesForFiles);

            indexHtml.append("</table>\n<br/>\n");

            // We can't render this earlier since the result is affected by
            // unloaded rows.
            myGlobalCoverage.renderCoveragePercentage(indexHtml);
            if (myUnloadedEntries != 0)
            {
                indexHtml.append(" (estimate since ");
                indexHtml.append(Long.toString(myUnloadedEntries));
                indexHtml.append(" files were not loaded)");
            }
        }
    }


    public void renderFullReport(File outputDir)
        throws FusionException, IOException
    {
        analyze();

        prepareRelativeNames();

        renderSourceFiles(outputDir);
        renderIndex(outputDir);
    }
}
