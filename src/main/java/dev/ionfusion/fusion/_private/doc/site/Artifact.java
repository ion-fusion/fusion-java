// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.site;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a physical artifact to be produced during site generation.
 * It ties an entity to a path within the site root.
 *
 * @param <Entity> the type of entity providing artifact content.
 */
public class Artifact <Entity>
{
    private static final Path EMPTY_PATH = Paths.get("");

    private final Entity myEntity;
    private final Path   myPath;

    /**
     * Creates a new artifact.
     *
     * @param entity must not be null.
     * @param path must be relative to the site root.
     */
    public Artifact(Entity entity, Path path)
    {
        if (path.isAbsolute())
        {
            throw new IllegalArgumentException("File path must be relative to site root");
        }

        myEntity = requireNonNull(entity);
        myPath = requireNonNull(path);
    }


    /**
     * Provides the entity from which to generate an artifact.
     *
     * @return not null.
     */
    public Entity getEntity()
    {
        return myEntity;
    }


    /**
     * Returns the path to the file this Artifact will produce, relative to the site root.
     */
    public Path getRelativePath()
    {
        return myPath;
    }


    /**
     * Provides a relative path that leads "up" to the base; a series of {@code ".."} components of
     * the same depth as our file.
     *
     * @return not null.
     */
    public Path getPathToBase()
    {
        Path parent = myPath.getParent();
        return (parent == null ? EMPTY_PATH : parent.relativize(EMPTY_PATH));
    }
}
