// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.io.File;
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

    /**
     * Creates a {@link SourceName} representing a file at the given path.
     *
     * @param path must not be null or empty.
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
        return new FileSourceName(new File(path));
    }

    /**
     * Creates a {@link SourceName} representing a file.
     * The {@link File}'s absolute path will be displayed.
     *
     * @param path must not be null or empty.
     *
     * @see #forFile(String)
     *
     * @return a new {@link SourceName} instance
     */
    public static SourceName forFile(File path)
    {
        return new FileSourceName(path);
    }

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
     * Returns the associated source file, if one is known.
     * This is the case for instances created by {@link #forFile(File)} or
     * {@link #forFile(String)}.
     *
     * @return the source file, or null.
     */
    public File getFile()
    {
        return null;
    }

    public Path getPath()
    {
        File f = getFile();
        return f == null ? null : f.toPath();
    }

    URL getUrl()
    {
        return null;
    }

    /**
     * It is not guaranteed that the module declaration is the only content of
     * the file or URL.
     * The resource could be a script with several modules inside, and modules
     * declarations will eventually nest.
     */
    ModuleIdentity getModuleIdentity()
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


    boolean equals(SourceName other)
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
    }


    //=========================================================================


    private static class ModuleSourceName
        extends SourceName
    {
        private final ModuleIdentity myId;
        private final File myFile;

        ModuleSourceName(ModuleIdentity id, File file)
        {
            super(id + " (at file:" + file + ")");
            myId   = id;
            myFile = file;
        }

        @Override
        public File getFile() { return myFile; }

        @Override
        ModuleIdentity getModuleIdentity() { return myId; }
    }


    static SourceName forModule(ModuleIdentity id, File sourceFile)
    {
        assert sourceFile != null;
        return new ModuleSourceName(id, sourceFile);
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
            myId  = id;
            myUrl = url;
        }

        @Override
        URL getUrl() { return myUrl; }

        @Override
        ModuleIdentity getModuleIdentity() { return myId; }
    }


    /**
     * NOT FOR APPLICATION USE
     */
    static SourceName forUrl(ModuleIdentity id, URL url)
    {
        return new UrlSourceName(id, url);
    }
}
