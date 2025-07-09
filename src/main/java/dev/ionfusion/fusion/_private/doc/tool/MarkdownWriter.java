// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import com.petebevin.markdown.MarkdownProcessor;
import dev.ionfusion.fusion._private.HtmlWriter;
import java.io.File;
import java.io.IOException;

public class MarkdownWriter
    extends HtmlWriter
{
    private final MarkdownProcessor myMarkdown = new MarkdownProcessor();

    public MarkdownWriter(File outputFile)
        throws IOException
    {
        super(outputFile);
    }

    protected void markdown(String text)
        throws IOException
    {
        String md = myMarkdown.markdown(text);
        append(md);
    }
}
