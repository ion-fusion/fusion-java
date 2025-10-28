// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.tool.AlphaIndex;
import dev.ionfusion.fusion._private.doc.tool.ExportedBinding;
import java.io.IOException;

final class AlphabeticalIndexWriter
    extends HtmlWriter
{
    private final AlphaIndex myIndex;


    AlphabeticalIndexWriter(AlphaIndex index, StreamWriter out)
    {
        super(out);
        myIndex = index;
    }

    void renderIndex()
        throws IOException
    {
        renderHeader1("Binding Index");

        append("<table><tbody>");
        for (AlphaIndex.AlphaEntry entry : myIndex)
        {
            String escapedName = escapeString(entry.bindingName());
            append("<tr><td class='bound'>");
            append(escapedName);
            append("</td><td>");

            boolean printedOne = false;
            for (ExportedBinding export : entry.exports())
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
