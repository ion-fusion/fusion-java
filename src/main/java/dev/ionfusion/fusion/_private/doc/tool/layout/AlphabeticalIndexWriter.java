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

final class AlphabeticalIndexWriter
    extends HtmlWriter
{
    private final DocIndex myIndex;


    AlphabeticalIndexWriter(DocIndex index, StreamWriter out)
    {
        super(out);
        myIndex = index;
    }

    void renderIndex()
        throws IOException
    {
        renderHeader1("Binding Index");

        append("<table><tbody>");
        for (Map.Entry<String, Set<ModuleIdentity>> entry : myIndex.getNameMap().entrySet())
        {
            String escapedName = escapeString(entry.getKey());
            append("<tr><td class='bound'>");
            append(escapedName);
            append("</td><td>");

            boolean printedOne = false;
            for (ModuleIdentity id : entry.getValue())
            {
                if (printedOne)
                {
                    append(", ");
                }
                linkToBindingAsModulePath(id, escapedName);
                printedOne = true;
            }

            append("</td></tr>\n");
        }
        append("</tbody></table>\n");
    }
}
