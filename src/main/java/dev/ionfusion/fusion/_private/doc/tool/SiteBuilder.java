// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import dev.ionfusion.fusion._private.doc.site.Site;
import dev.ionfusion.fusion._private.doc.site.Template;
import dev.ionfusion.fusion._private.doc.tool.layout.ModuleLayout;
import dev.ionfusion.fusion._private.doc.tool.layout.StreamingTemplate;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class SiteBuilder
{
    private final Site                      mySite = new Site();
    private final RepoEntity                myRepo;
    private final Predicate<ModuleIdentity> myModuleSelector;

    /**
     *
     * @param repo
     * @param selector predicate determining which modules will be documented.
     */
    public SiteBuilder(RepoEntity repo,
                       Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myRepo = repo;
    }

    public Site build()
        throws FusionException
    {
        return mySite;
    }


    private <E> void placePage(E entity, Path path, Template<E, StreamWriter> template)
    {
        mySite.placeArtifact(entity, path, new StreamingTemplate<E>(template));
    }

    private <E> void placePage(E entity, String path, Template<E, StreamWriter> template)
    {
        mySite.placeArtifact(entity, path, new StreamingTemplate<E>(template));
    }


    /**
     * Discover selected modules in our {@link RepoEntity}, placing a page
     * for each at the same path within the site.
     *
     * @throws FusionException for problems discovering modules.
     */
    public void placeModules()
        throws FusionException
    {
        Template<ModuleEntity, StreamWriter> template = ModuleLayout.template(myModuleSelector);

        for (ModuleEntity module : myRepo.getModules())
        {
            ModuleIdentity id = module.getIdentity();
            Path file = Paths.get(".", id.absolutePath() + ".html");

            placePage(module, file, template);
        }
    }
}
