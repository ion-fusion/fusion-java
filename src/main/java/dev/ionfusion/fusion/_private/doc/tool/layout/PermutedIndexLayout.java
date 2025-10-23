// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.tool.DocIndex;
import java.io.IOException;
import java.util.ArrayList;

public final class PermutedIndexLayout
    extends CommonLayout<DocIndex>
{
    public PermutedIndexLayout(Artifact<DocIndex> artifact)
    {
        super(artifact);
    }

    @Override
    void addCssUrls(ArrayList<String> urls)
        throws IOException
    {
        super.addCssUrls(urls);
        urls.add("index.css");
    }

    @Override
    String getTitle()
        throws IOException
    {
        return "Fusion Binding Index (Permuted)";
    }

    @Override
    void renderContent(StreamWriter out)
        throws IOException
    {
        new PermutedIndexWriter(getEntity(), out).renderIndex();
    }
}
