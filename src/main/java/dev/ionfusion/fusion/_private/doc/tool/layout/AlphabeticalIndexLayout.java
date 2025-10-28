// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.tool.AlphaIndex;
import java.io.IOException;
import java.util.ArrayList;

public final class AlphabeticalIndexLayout
    extends CommonLayout<AlphaIndex>
{
    public AlphabeticalIndexLayout(Artifact<AlphaIndex> artifact)
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
        return "Fusion Binding Index";
    }

    @Override
    void renderContent(StreamWriter out)
        throws IOException
    {
        new AlphabeticalIndexWriter(getEntity(), out).renderIndex();
    }
}
