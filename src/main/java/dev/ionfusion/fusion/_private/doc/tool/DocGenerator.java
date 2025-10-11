// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import com.amazon.ion.Timestamp;
import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.RepoEntity;
import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * NOT FOR APPLICATION USE
 */
public final class DocGenerator
{
    private DocGenerator() {}


    public static void writeHtmlTree(FusionRuntime runtime,
                                     File outputDir,
                                     File repoDir,
                                     Predicate<ModuleIdentity> filter)
        throws IOException, FusionException
    {
        log("Building module docs");
        RepoEntity repo = new RepoEntity(repoDir.toPath(), filter, runtime.makeTopLevel());
        SiteBuilder site = new SiteBuilder(repo, filter);

        log("Discovering module docs");
        site.placeModules();

        log("Discovering Markdown pages");
        // TODO Move articles to a separate directory.
        site.placeArticles(repoDir.toPath().resolve("src"));

        log("Building indices");
        site.prepareIndexes();

        log("Writing HTML pages");
        site.build().generate(outputDir.toPath());

        log("DONE writing HTML docs to " + outputDir);
    }


    private static void log(String message)
    {
        System.out.print(Timestamp.now());
        System.out.print(" ");
        System.out.println(message);
    }
}
