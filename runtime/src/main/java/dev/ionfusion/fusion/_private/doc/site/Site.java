// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/**
 * Generates a number of files under a root directory, each one based on the
 * information in a particular <em>entity</em>.
 * <p>
 * This acts rather like a file system, and perhaps would be better as a tree
  of {@link Artifact}s.
 * <p>
 * To build a site, you first <em>place</em> all the desired artifacts at paths
 * relative to the (still unknown) root directory, then call
 * {@link #generate(Path)} with the root output directory.
 * <p>
 * Each artifact is generated from a single arbitrary <em>entity</em> by way of
 * a {@link Template}.
 */
public class Site
{
    private static final class Placement <E>
    {
        private final Artifact<E>       myArtifact;
        private final Template<E, Path> myTemplate;

        private Placement(Artifact<E> artifact, Template<E, Path> template)
        {
            myArtifact = artifact;
            myTemplate = template;
        }

        void generate(Path siteDir)
            throws IOException
        {
            Path file = siteDir.resolve(myArtifact.getRelativePath());
            myTemplate.populate(myArtifact).generate(file);
        }
    }

    private final Map<Path, Placement<?>> myPlacements = new HashMap<>();


    /**
     * Declares an artifact placement within this site.
     * <p>
     * Each placement must be at a unique path.
     *
     * @param entity provides content for the artifact; not null.
     * @param path where the artifact will be generated, relative to the site
     * root directory.
     * @param template determines how the artifact is created.
     */
    public <E> void placeArtifact(E entity, Path path, Template<E, Path> template)
    {
        // We care about Entities so that we can compute links between them.

        if (myPlacements.containsKey(path))
        {
            throw new IllegalArgumentException("Duplicate artifact path: " + path);
        }

        Artifact<E> artifact = new Artifact<>(entity, path);
        myPlacements.put(path, new Placement<>(artifact, template));
    }


    /**
     * Declares an artifact placement within this site.
     * <p>
     * Each placement must be at a unique path.
     *
     * @param entity provides content for the artifact; not null.
     * @param path is turned into a path as by {@link Paths#get(String, String...)}.
     * @param template determines how the artifact is created.
     */
    public <E> void placeArtifact(E entity, String path, Template<E, Path> template)
    {
        placeArtifact(entity, Paths.get(path), template);
    }


    /**
     * Generates files for all artifacts.
     *
     * @param siteDir must be an absolute path.
     *
     * @throws IOException if any part of site generation does so.
     */
    public void generate(Path siteDir)
        throws IOException
    {
        assert siteDir.isAbsolute();
        // TODO check that we aren't commingling with old data.

        for (Placement<?> placement : myPlacements.values())
        {
            placement.generate(siteDir);
        }
    }
}
