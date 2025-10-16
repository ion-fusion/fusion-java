// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.DocIndex.buildDocIndex;
import static java.nio.file.Files.isDirectory;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.MarkdownArticle;
import dev.ionfusion.fusion._private.doc.model.ModuleEntity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import dev.ionfusion.fusion._private.doc.site.FileCopyTemplate;
import dev.ionfusion.fusion._private.doc.site.Site;
import dev.ionfusion.fusion._private.doc.site.Template;
import dev.ionfusion.fusion._private.doc.tool.layout.AlphabeticalIndexLayout;
import dev.ionfusion.fusion._private.doc.tool.layout.MarkdownArticleLayout;
import dev.ionfusion.fusion._private.doc.tool.layout.ModuleLayout;
import dev.ionfusion.fusion._private.doc.tool.layout.PermutedIndexLayout;
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

        for (ModuleEntity module : myRepo.getSelectedModules())
        {
            ModuleIdentity id = module.getIdentity();
            Path file = Paths.get(".", id.absolutePath() + ".html");

            placePage(module, file, template);
        }
    }


    /**
     * Discover Markdown ({@code *.md}) files and place corresponding pages
     * within the site at the same path.
     *
     * @param fromDir the directory to traverse recursively.
     */
    public void placeArticles(Path fromDir)
    {
        placeArticles(fromDir, Paths.get(""));
    }

    /**
     * Discover Markdown ({@code *.md}) files and place corresponding pages
     * within the site.
     *
     * @param fromDir the directory to traverse recursively.
     * @param toDir the base path for corresponding HTML pages, relative to the
     * site root.
     */
    public void placeArticles(Path fromDir, Path toDir)
    {
        String[] fileNames = fromDir.toFile().list();
        if (fileNames == null) return;

        for (String fileName : fileNames)
        {
            Path fromPath = fromDir.resolve(fileName);

            if (fileName.endsWith(".md"))
            {
                String baseName = fileName.substring(0, fileName.length() - 3);
                Path toPath = toDir.resolve(baseName + ".html");

                MarkdownArticle article = new MarkdownArticle(fromPath);
                placePage(article, toPath, MarkdownArticleLayout::new);
            }
            else if (isDirectory(fromPath))
            {
                placeArticles(fromPath, toDir.resolve(fileName));
            }
        }
    }


    public void placeAssets(Path fromDir)
    {
        placeAssets(fromDir, Paths.get(""));
    }

    public void placeAssets(Path fromDir,  Path toDir)
    {
        String[] fileNames = fromDir.toFile().list();
        if (fileNames == null) return;

        FileCopyTemplate template = new FileCopyTemplate();
        for (String fileName : fileNames)
        {
            Path fromPath = fromDir.resolve(fileName);
            Path toPath = toDir.resolve(fileName);

            if (isDirectory(fromPath))
            {
                placeAssets(fromPath, toPath);
            }
            else
            {
                mySite.placeArtifact(fromPath, toPath, template);
            }
        }
    }


    public void prepareIndexes()
        throws FusionException
    {
        // The two index artifacts are different layouts of the same entity.
        DocIndex docIndex = buildDocIndex(myModuleSelector, myRepo.getSelectedModules());

        placePage(docIndex, "binding-index.html", AlphabeticalIndexLayout::new);
        placePage(docIndex, "permuted-index.html", PermutedIndexLayout::new);
    }
}
