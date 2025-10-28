// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.tool.ExportedBinding;
import dev.ionfusion.fusion._private.doc.tool.PermutedIndex;
import dev.ionfusion.fusion._private.doc.tool.PermutedIndex.PermutedEntry;
import java.io.IOException;

final class PermutedIndexWriter
    extends HtmlWriter
{
    private final PermutedIndex myIndex;

    PermutedIndexWriter(PermutedIndex index, StreamWriter out)
    {
        super(out);
        myIndex = index;
    }


    void renderIndex()
        throws IOException
    {
        renderHeader1("Permuted Binding Index");

        append("<table><tbody>");
        for (PermutedEntry line : myIndex)
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
            for (ExportedBinding export : line.exports())
            {
                if (printedOne)
                {
                    append(", ");
                }
                linkToBindingAsModulePath(export.getModuleId(), escapedName);
                printedOne = true;
            }

            append("</td></tr>\n");
        }
        append("</tbody></table>\n");
    }
}
