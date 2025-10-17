// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.MarkdownArticle;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.tool.MarkdownWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The HTML layout used for standalone pages.
 */
public class MarkdownArticleLayout
    extends CommonLayout<MarkdownArticle>
{
    public MarkdownArticleLayout(Artifact<MarkdownArticle> artifact)
    {
        super(artifact);
    }


    @Override
    void addCssUrls(ArrayList<String> urls)
        throws IOException
    {
        super.addCssUrls(urls);
        urls.add("doc.css");
    }

    @Override
    String getTitle()
        throws IOException
    {
        return getEntity().getTitle();
    }

    @Override
    void renderContent(StreamWriter out)
        throws IOException
    {
        new MarkdownWriter(out).markdown(getEntity().getContent());
    }
}
