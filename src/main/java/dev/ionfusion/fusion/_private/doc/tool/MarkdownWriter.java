// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import com.petebevin.markdown.MarkdownProcessor;
import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import java.io.IOException;

public class MarkdownWriter
    extends HtmlWriter
{
    // WARNING: These are stateful.
    private final MarkdownProcessor myMarkdown = new MarkdownProcessor();

    public MarkdownWriter(StreamWriter out)
    {
        super(out);
    }


    public void markdown(String text)
        throws IOException
    {
        String md = myMarkdown.markdown(text);
        append(md);
    }
}
