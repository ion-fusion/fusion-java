// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import static dev.ionfusion.runtime.base.SourceLocation.compareByLineColumn;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.OffsetSpan;
import com.amazon.ion.Span;
import com.amazon.ion.SpanProvider;
import com.amazon.ion.Timestamp;
import com.amazon.ion.system.IonReaderBuilder;
import dev.ionfusion.runtime._private.io.StreamWriter;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public final class CoverageReportWriter
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


    private final CoverageReport myReport;

    /**
     * Maps each reported source file to its HTML file in the generated site.
     */
    private final Map<Path, String> myRelativeNamesForSources;

    /**
     * Counts the number of modules and/or scripts that have no coverage data.
     * Used to estimate coverage stats for them.
     */
    private long myUnloadedEntries;

    // For rendering highlighted source files
    private static final int BUFFER_SIZE = 2048;
    private final byte[] myCopyBuffer = new byte[BUFFER_SIZE];
    private long myIonBytesRead;
    private boolean coverageState;


    public CoverageReportWriter(CoverageReport report)
    {
        myReport = report;
        myRelativeNamesForSources = new HashMap<>();
    }


    //=========================================================================
    // Metrics Analysis

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
     * files in our {@link CoverageReport}.  We omit this prefix from the
     * directory tree of generated HTML files.
     * <p>
     * TODO It would be better to use paths relative to repository roots, to
     *   avoid exposing details of the build-time environment.  This suggests
     *   that {@code SourceName} should track the repository holding the source.
     */
    private int commonPrefixLen()
        throws IOException
    {
        Path prefix = null;
        for (CoveredFile sourceName : myReport.sourceFiles())
        {
            // Skip URL-based sources.
            Path file = sourceName.getPath();
            if (file != null)
            {
                Path path   = file.toRealPath();
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
        int prefixLen = commonPrefixLen();

        for (CoveredFile sourceName : myReport.sourceFiles())
        {
            // TODO Determine this on demand, it's only needed twice per source.
            //      so this code is more complicated than its worth.
            Path file = sourceName.getPath();
            if (file != null)
            {
                Path path        = file.toRealPath();
                Path shorterPath = path.subpath(prefixLen, path.getNameCount());
                myRelativeNamesForSources.put(path, shorterPath.toString());
            }
        }
    }


    private List<CoveredModule> sortedModules()
    {
        return myReport.modules()
                       .stream()
                       .sorted(comparing(CoveredModule::getId))
                       .collect(toList());
    }

    private List<CoveredFile> sortedScripts()
    {
        return myReport.scripts()
                       .stream()
                       .sorted(comparing(CoveredFile::getPath))
                       .collect(toList());
    }

    private SourceLocation[] sortedLocations(CoveredFile name)
    {
        SourceLocation[] locations = name.locations().toArray(new SourceLocation[0]);
        assert locations.length == name.getSummary().total()
            : "Number of locations doesn't match coverage summary";
        Arrays.sort(locations, SourceLocation::compareByLineColumn);
        return locations;
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


    private void renderSource(HtmlWriter sourceHtml,
                              CoveredFile name)
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
                Path file = name.getPath();
                if (file != null)
                {
                    path = file.toRealPath().toString();
                }
                else
                {
                    // TODO improve this rendering
                    path = name.getUri().toString();
                }
                sourceHtml.append(sourceHtml.escapeString(path));
            }
            else
            {
                sourceHtml.append("File ");
                sourceHtml.append(name.getPath().toString());
                sourceHtml.append("</h1>\n");
            }
        }

        SourceLocation[] locations = sortedLocations(name);
        assert locations.length != 0;

        int locationIndex = 0;

        sourceHtml.append("\n<hr/>\n");
        sourceHtml.append("<pre>");

        // Copy the document in chunks separated by coverage state changes.
        // At each change, we insert appropriate HTML <span> markup.
        try (InputStream ionBytes = name.readSource())
        {
            myIonBytesRead = 0;

            try (IonReader ionReader =
                     IonReaderBuilder.standard().build(name.readSource()))
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
                    assert compareByLineColumn(currentLoc, coverageLoc) <= 0;

                    if (compareByLineColumn(currentLoc, coverageLoc) == 0)
                    {
                        boolean covered = name.isLocationCovered(coverageLoc);
                        setCoverageState(sourceHtml, ionBytes, spanProvider,
                                         covered);
                        locationIndex++;
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

                // Copy the rest of the Ion source.
                copySourceThroughOffset(sourceHtml, ionBytes, Long.MAX_VALUE);

                sourceHtml.append("</span>");
            }
        }

        sourceHtml.append("</pre>\n");
        sourceHtml.append("<hr/>");
        name.getSummary().renderCoveragePercentage(sourceHtml);
    }


    private String relativeName(CoveredEntity name)
    {
        String resource;
        Path file = name.getPath();
        if (file != null)
        {
            resource = myRelativeNamesForSources.get(file);
        }
        else
        {
            URI uri = name.getUri();
            assert (uri.getScheme().equalsIgnoreCase("jar"));
            String path = uri.getSchemeSpecificPart();
            assert path != null : "null path in " + uri;
            int    bang = path.indexOf("!/");
            assert bang > 1;
            resource = path.substring(bang + 2);
            assert !resource.isEmpty();
            assert !resource.startsWith("/");
        }
        return resource + ".html";
    }


    private void renderSourceFiles(Path outputDir)
        throws IOException
    {
        for (CoveredFile name : myReport.sourceFiles())
        {
            Path outFile = outputDir.resolve(relativeName(name));
            try (StreamWriter sourceHtml = new StreamWriter(outFile))
            {
                renderSource(new HtmlWriter(sourceHtml), name);
            }
        }
    }


    private <T extends CoveredEntity> void renderRows(HtmlWriter indexHtml,
                                                      String     category,
                                                      List<T>    rows)
        throws IOException
    {
        long totalExpressions = 0;
        long unloadedCount = 0;

        boolean first = true;
        for (CoveredEntity row : rows)
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

            CoverageInfoPair pair = row.getSummary();
            if (pair.total() != 0)
            {
                totalExpressions += pair.total();
            }
            else // The source was never loaded
            {
                pair = new EstimatedCoverageInfoPair();

                unloadedCount++;
            }

            indexHtml.append("<tr><td>");
            pair.renderPercentageGraph(indexHtml);
            indexHtml.append("</td><td>");

            String descr = row.describe();
            URI    uri   = row.getUri();
            if (uri != null)
            {
                // TODO Exclude the common prefix when key is a script File.
                indexHtml.append("<a href=\"" + relativeName(row) + "\">");
                indexHtml.append(descr);
                indexHtml.append("</a>");
            }
            else
            {
                indexHtml.append(descr);
            }

            indexHtml.append("</td></tr>\n");
        }

        if (unloadedCount != 0)
        {
            long loadedCount = rows.size() - unloadedCount;
            long average = (loadedCount == 0
                               ? 500                        // TODO Totally made up
                               : totalExpressions / loadedCount);

            myReport.globalSummary().uncoveredExpressions += (unloadedCount * average);

            myUnloadedEntries += unloadedCount;
        }
    }


    private void renderIndex(Path indexFile)
        throws IOException
    {
        try (StreamWriter out = new StreamWriter(indexFile))
        {
            HtmlWriter indexHtml = new HtmlWriter(out);
            indexHtml.renderHeadWithInlineCss("Fusion Code Coverage", CSS);

            indexHtml.append("<p>Report generated at ");
            indexHtml.append(Timestamp.now().toString());
            indexHtml.append("</p>\n");

            indexHtml.append("<table class='report'>\n");

            renderRows(indexHtml, "Module", sortedModules());
            renderRows(indexHtml, "File",   sortedScripts());

            indexHtml.append("</table>\n<br/>\n");

            // We can't render this earlier since the result is affected by
            // unloaded rows.
            myReport.globalSummary().renderCoveragePercentage(indexHtml);
            if (myUnloadedEntries != 0)
            {
                indexHtml.append(" (estimate since ");
                indexHtml.append(Long.toString(myUnloadedEntries));
                indexHtml.append(" files were not loaded)");
            }
        }
    }


    /**
     * @return the path of the index file.
     */
    public Path renderFullReport(Path outputDir)
        throws IOException
    {
        prepareRelativeNames();

        renderSourceFiles(outputDir);

        Path indexFile = outputDir.resolve("index.html");
        renderIndex(indexFile);
        return indexFile;
    }
}
