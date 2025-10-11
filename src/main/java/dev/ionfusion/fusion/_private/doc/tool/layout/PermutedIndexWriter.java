// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.tool.DocIndex;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

final class PermutedIndexWriter
    extends HtmlWriter
{
    private final Predicate<ModuleIdentity> myFilter;
    private final DocIndex                  myIndex;

    /**
     * Maps keywords to the lines in which they exist.
     */
    private final TreeSet<Line> myLines;


    /**
     * An index line.
     */
    private static final class Line
        implements Comparable<Line>
    {
        private final String                                 myPrefix;
        private final String                                 myKeyword;
        private final Map.Entry<String, Set<ModuleIdentity>> myEntry;


        Line(Map.Entry<String, Set<ModuleIdentity>> entry, int keywordStartPos, int keywordLimitPos)
        {
            String name = entry.getKey();
            myPrefix = name.substring(0, keywordStartPos);
            myKeyword = name.substring(keywordStartPos, keywordLimitPos);
            myEntry = entry;
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


    PermutedIndexWriter(Predicate<ModuleIdentity> filter, DocIndex index, StreamWriter out)
    {
        super(out);

        myFilter = filter;
        myIndex = index;
        myLines = new TreeSet<>();
    }


    private void permute()
    {
        for (Map.Entry<String, Set<ModuleIdentity>> entry : myIndex.getNameMap().entrySet())
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

        renderHeader1("Permuted Binding Index");

        append("<table><tbody>");
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
                if (myFilter.test(id))
                {
                    if (printedOne)
                    {
                        append(", ");
                    }
                    linkToBindingAsModulePath(id, escapedName);
                    printedOne = true;
                }
            }

            append("</td></tr>\n");
        }
        append("</tbody></table>\n");
    }
}
