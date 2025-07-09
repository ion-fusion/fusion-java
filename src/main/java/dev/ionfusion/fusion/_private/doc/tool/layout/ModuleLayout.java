// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.Template;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * The HTML layout used for module reference pages.
 * <p>
 * The body of the page is rendered by a {@link ModuleWriter}.
 */
public final class ModuleLayout
    extends CommonLayout<ModuleEntity>
{
    private final Predicate<ModuleIdentity> mySelector;

    public ModuleLayout(Artifact<ModuleEntity> artifact, Predicate<ModuleIdentity> selector)
    {
        super(artifact);
        mySelector = selector;
    }

    public static Template<ModuleEntity, StreamWriter> template(Predicate<ModuleIdentity> selector)
    {
        return artifact -> new ModuleLayout(artifact, selector);
    }

    @Override
    void addCssUrls(ArrayList<String> urls)
        throws IOException
    {
        super.addCssUrls(urls);
        urls.add("module.css");
    }

    @Override
    String getTitle()
    {
        return getEntity().getIdentity().absolutePath();
    }

    @Override
    void renderContent(StreamWriter out)
        throws IOException
    {
        new ModuleWriter(mySelector, out, getEntity()).renderModule();
    }
}
