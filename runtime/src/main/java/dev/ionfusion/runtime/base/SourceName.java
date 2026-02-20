// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Identifies a source of Fusion code or other data: a file, URL, <em>etc.</em>
 * <p>
 * The primary purpose of this class is to display a suitable message fragment
 * for error reporting to users.
 */
public class SourceName
{
    /**
     * The standard extension for Fusion source code files.
     */
    public static final String FUSION_SOURCE_EXTENSION = ".fusion";

    private final String myDisplay;


    private SourceName(String display)
    {
        myDisplay = display;
    }


    /**
     * Returns the human-readable source name, for display in messages.
     *
     * @return the displayable name of this source
     */
    public String display()
    {
        return myDisplay;
    }


    /**
     * Returns the absolute path of the source file if one is known.
     * This is the case for instances created by {@link #forFile(File)} or
     * {@link #forFile(String)}.
     *
     * @return null if this source is not an actual file.
     */
    public File getFile()
    {
        return null;
    }

    /**
     * Returns the absolute path of the source file if one is known.
     *
     * @return null if this source is not an actual file.
     */
    public Path getPath()
    {
        return null;
    }

    /**
     * Returns a URL for the source. The protocol can vary; at least {@code file}
     * and {@code jar} are possible. In general, {@link URL#openStream()} is
     * expected to work.
     *
     * @return null if this source cannot be identified as a URL.
     */
    public URL getUrl()
    {
        return null;
    }

    public URI getUri()
    {
        return null;
    }


    /**
     * It is not guaranteed that the module declaration is the only content of
     * the file or URL.
     * The resource could be a script with several modules inside, and modules
     * declarations will eventually nest.
     *
     * @return the module associated with this source, if any.
     */
    public ModuleIdentity getModuleIdentity()
    {
        return null;
    }


    /**
     * Returns a view of this object suitable for debugging.
     * For displaying messages to users, use {@link #display()} instead.
     */
    @Override
    public String toString()
    {
        return myDisplay;
    }


    public boolean equals(SourceName other)
    {
        return (other != null && myDisplay.equals(other.myDisplay));
    }

    @Override
    public boolean equals(Object other)
    {
        return (other instanceof SourceName && equals((SourceName) other));
    }

    private static final int HASH_SEED = SourceName.class.hashCode();

    @Override
    public int hashCode()
    {
        int result = HASH_SEED + myDisplay.hashCode();
        result ^= (result << 29) ^ (result >> 3);
        return result;
    }


    /**
     * Compares sources by their {@linkplain #display() display form}.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     *
     * @return a negative integer, zero, or a positive integer as the first argument is
     * less than, equal to, or greater than the second.
     */
    public static int compareByDisplay(SourceName o1, SourceName o2)
    {
        return o1.display().compareTo(o2.display());
    }


    //=========================================================================


    private static class FileSourceName
        extends SourceName
    {
        private final File myFile;

        FileSourceName(File file)
        {
            super(file.getAbsolutePath());
            myFile = file;
        }

        @Override
        public File getFile() { return myFile; }

        @Override
        public Path getPath() { return myFile.toPath(); }

        @Override
        public URI getUri() { return myFile.toURI(); }
    }


    //=========================================================================


    private static class ModuleSourceName
        extends SourceName
    {
        private final ModuleIdentity myId;
        private final File           myFile;

        ModuleSourceName(ModuleIdentity id, File file)
        {
            super(id + " (at file:" + file + ")");
            assert file.isAbsolute();
            myId   = id;
            myFile = file;
        }

        @Override
        public File getFile() { return myFile; }

        @Override
        public Path getPath() { return myFile.toPath(); }

        public URI getUri() { return myFile.toURI(); }

        @Override
        public ModuleIdentity getModuleIdentity() { return myId; }
    }


    //=========================================================================


    /**
     * Identifies a data source using a URL.
     */
    private static class UrlSourceName
        extends SourceName
    {
        private final ModuleIdentity myId;
        private final URL            myUrl;

        private UrlSourceName(ModuleIdentity id, URL url)
        {
            super(id + " (at " + url.toExternalForm() + ")");
            assert !url.getProtocol().equalsIgnoreCase("file")
                : "Use FileSourceName for local files";
            myId  = id;
            myUrl = url;
        }

        @Override
        public URL getUrl() { return myUrl; }

        @Override
        public URI getUri()
        {
            try
            {
                return myUrl.toURI();
            }
            catch (URISyntaxException e)
            {
                throw new AssertionError(e); // should not happen
            }
        }

        @Override
        public ModuleIdentity getModuleIdentity() { return myId; }
    }


    //=========================================================================
    // Factory methods

    /**
     * Creates a {@link SourceName} that will simply display the given text.
     *
     * @param display must not be null.
     *
     * @return a new {@link SourceName} instance
     */
    public static SourceName forDisplay(String display)
    {
        if (display.isEmpty()) {
            throw new IllegalArgumentException("display must not be empty");
        }
        return new SourceName(display);
    }


    /**
     * Creates a {@link SourceName} representing a file at the given path.
     *
     * @param path must not be null or empty, and is converted to an absolute path.
     *
     * @return a new {@link SourceName} instance
     *
     * @see #forFile(File)
     */
    public static SourceName forFile(String path)
    {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        return new FileSourceName(new File(path).getAbsoluteFile());
    }

    /**
     * Creates a {@link SourceName} representing a file. The {@link File}'s absolute
     * path will be displayed.
     *
     * @param path is converted to an absolute path.
     *
     * @return a new {@link SourceName} instance
     *
     * @see #forFile(String)
     */
    public static SourceName forFile(File path)
    {
        return new FileSourceName(path.getAbsoluteFile());
    }


    public static SourceName forModule(ModuleIdentity id, File sourceFile)
    {
        requireNonNull(id, "id must not be null");
        return new ModuleSourceName(id, sourceFile);
    }


    public static SourceName forUrl(ModuleIdentity id, URL url)
    {
        requireNonNull(id, "id must not be null");
        return new UrlSourceName(id, url);
    }
}
