// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class IndexWriter
    extends HtmlWriter
{
    private final Predicate<ModuleIdentity> myFilter;


    public IndexWriter(Predicate<ModuleIdentity> filter, File outputFile)
        throws IOException
    {
        super(outputFile);
        myFilter = filter;
    }


    void renderIndex(DocIndex index)
        throws IOException
    {
        openHtml();
        {
            renderHead("Fusion Binding Index", null, "common.css", "index.css");

            openBody();
            {
                append("<div class='indexlink'>" + "<a href='index.html'>Top</a> " +
                       "<a href='permuted-index.html'>Permuted Index</a>" + "</div>\n");

                renderHeader1("Binding Index");

                append("<table><tbody>");
                for (Map.Entry<String, Set<ModuleIdentity>> entry : index.getNameMap().entrySet())
                {
                    String escapedName = escapeString(entry.getKey());
                    append("<tr><td class='bound'>");
                    append(escapedName);
                    append("</td><td>");

                    boolean printedOne = false;
                    for (ModuleIdentity id : entry.getValue())
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
            closeBody();
        }
        closeHtml();
    }
}
