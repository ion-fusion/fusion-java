// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Identifies a persistent, external resource, such as a file or a JAR entry.
 * <p>
 * On the whole, this offers a Fusion-oriented alternative to {@link URI}.
 * <p>
 * Two identifiers are equal if they have equal URIs.
 */
public abstract class ResourceIdentifier
{
    /**
     * Prevent instantiation from other places.
     */
    private ResourceIdentifier() {}


    /**
     * Returns a URI for the resource. The protocol can vary; at least {@code file} and
     * {@code jar} are possible. In general, {@code toUrl().openStream()} is expected to
     * work.
     *
     * @return not null.
     */
    public abstract URI getUri();

    /**
     * Returns the absolute, normalized path of the resource if one is known.
     *
     * @return null if this resource is not a file system resource.
     */
    public abstract Path getPath();

    /**
     * Opens an input stream for reading the resource.
     *
     * @return a new input stream.
     *
     * @throws IOException if there's a problem opening the stream.
     */
    public abstract InputStream openStream()
        throws IOException;


    /**
     * Returns the resource location as text.
     */
    public String toString()
    {
        return getUri().toString();
    }


    public boolean equals(ResourceIdentifier that)
    {
        return this.getUri().equals(that.getUri());
    }

    @Override
    public boolean equals(Object that)
    {
        return that instanceof ResourceIdentifier &&
               this.equals((ResourceIdentifier) that);
    }

    @Override
    public int hashCode()
    {
        return getUri().hashCode();
    }


    //=========================================================================
    // Factory methods

    /**
     * Creates a {@link ResourceIdentifier} representing a file.
     *
     * @param path is converted to a normalized absolute path.
     *
     * @return a new {@link ResourceIdentifier}.
     *
     * @see #forFile(String)
     */
    public static ResourceIdentifier forFile(Path path)
    {
        return new FileResource(path);
    }

    /**
     * Creates a {@link ResourceIdentifier} representing a file.
     *
     * @param file is converted to a normalized absolute path.
     *
     * @return a new {@link ResourceIdentifier}.
     *
     * @see #forFile(String)
     */
    public static ResourceIdentifier forFile(File file)
    {
        return new FileResource(file.toPath());
    }

    /**
     * Creates a {@link ResourceIdentifier} representing a file at the given path.
     *
     * @param path is converted to a normalized absolute path.
     *
     * @return a new {@link ResourceIdentifier}.
     *
     * @see #forFile(File)
     */
    public static ResourceIdentifier forFile(String path)
    {
        return forFile(Paths.get(path));
    }


    /**
     * Creates a {@link ResourceIdentifier} representing a URI.
     *
     * @param uri must be a valid URI.
     *
     * @return a new {@link ResourceIdentifier}.
     */
    public static ResourceIdentifier forUri(URI uri)
    {
        // We may want to handle jar: URLs specially, so we can distinguish
        //  the Jar's file-system path and the internal resource path.

        if (uri.getScheme().equals("file"))
        {
            return new FileResource(Paths.get(uri));
        }

        return new UriResource(uri);
    }

    /**
     * Creates a {@link ResourceIdentifier} representing a URI.
     *
     * @param uri must be a valid URI.
     *
     * @return a new {@link ResourceIdentifier} instance.
     *
     * @throws IllegalArgumentException if the URI is not valid.
     */
    public static ResourceIdentifier forUri(String uri)
    {
        return forUri(URI.create(uri));
    }


    /**
     * Creates a {@link ResourceIdentifier} representing a URL.
     *
     * @param url is converted to a URI; must not be null.
     *
     * @return a new {@link ResourceIdentifier}.
     */
    public static ResourceIdentifier forUrl(URL url)
    {
        try
        {
            return forUri(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new AssertionError(e); // should not happen
        }
    }


    //=========================================================================
    // Implementations

    private static class UriResource
        extends ResourceIdentifier
    {
        private final URI myUri;

        private UriResource(URI url)
        {
            myUri = url;
        }

        @Override
        public URI getUri()
        {
            return myUri;
        }

        @Override
        public Path getPath()
        {
            return null;
        }

        public InputStream openStream()
            throws IOException
        {
            return myUri.toURL().openStream();
        }
    }


    /**
     * Specialization avoids Path/URI conversion and fast-tracks {@link #openStream()}.
     */
    private static class FileResource
        extends UriResource
    {
        private final Path myPath;

        // Awkward constructor to avoid normalizing twice.
        FileResource(Path normalizedPath, Object dummy)
        {
            super(normalizedPath.toUri());
            myPath = normalizedPath;
        }

        FileResource(Path path)
        {
            this(path.toAbsolutePath().normalize(), null);
        }

        @Override
        public Path getPath() { return myPath; }


        @Override
        public InputStream openStream()
            throws IOException
        {
            return Files.newInputStream(getPath());
        }

        @Override
        public String toString()
        {
            return myPath.toString();
        }
    }
}
