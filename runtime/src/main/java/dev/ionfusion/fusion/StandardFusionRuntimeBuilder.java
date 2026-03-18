// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.runtime._private.cover.CoverageCollectorFactory.fromDirectory;
import static dev.ionfusion.runtime._private.cover.CoverageConfiguration.forConfigFile;
import static dev.ionfusion.runtime._private.util.PropertiesFiles.readProperties;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidAbsoluteModulePath;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import com.amazon.ion.IonCatalog;
import dev.ionfusion.runtime._private.cover.CoverageCollector;
import dev.ionfusion.runtime._private.cover.CoverageCollectorImpl;
import dev.ionfusion.runtime._private.cover.CoverageConfiguration;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.embed.FusionRuntime;
import dev.ionfusion.runtime.embed.FusionRuntimeBuilder;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;


class StandardFusionRuntimeBuilder
    implements FusionRuntimeBuilder
{
    private static final String STANDARD_DEFAULT_LANGUAGE = "/fusion";


    //=========================================================================


    private OutputStream myCurrentOutputPort;
    private File         myCurrentDirectory;
    private File[]       myRepositoryDirectories;
    private String       myDefaultLanguage = STANDARD_DEFAULT_LANGUAGE;
    private IonCatalog   myDefaultIonCatalog;

    private File              myCoverageDataDirectory;
    private Path              myCoverageConfigFile;
    private CoverageCollector myCollector;

    private boolean myDocumenting;


    private StandardFusionRuntimeBuilder() { }

    private StandardFusionRuntimeBuilder(StandardFusionRuntimeBuilder that)
    {
        this.myCurrentOutputPort     = that.myCurrentOutputPort;
        this.myCurrentDirectory      = that.myCurrentDirectory;
        this.myRepositoryDirectories = that.myRepositoryDirectories;
        this.myDefaultLanguage       = that.myDefaultLanguage;
        this.myDefaultIonCatalog     = that.myDefaultIonCatalog;
        this.myCoverageDataDirectory = that.myCoverageDataDirectory;
        this.myCoverageConfigFile    = that.myCoverageConfigFile;
        this.myCollector             = that.myCollector;
        this.myDocumenting           = that.myDocumenting;
    }


    //=========================================================================

    @Override
    public final StandardFusionRuntimeBuilder copy()
    {
        return new Mutable(this);
    }

    @Override
    public StandardFusionRuntimeBuilder immutable()
    {
        return this;
    }

    @Override
    public StandardFusionRuntimeBuilder mutable()
    {
        return copy();
    }

    void mutationCheck()
    {
        throw new UnsupportedOperationException("This builder is immutable");
    }


    //=========================================================================


    @Override
    public FusionRuntimeBuilder withConfigProperties(Properties props)
    {
        FusionRuntimeBuilder b = this;

        String path = props.getProperty(PROPERTY_COVERAGE_DATA_DIR);
        if (path != null)
        {
            File f = new File(path);
            b = b.withCoverageDataDirectory(f);
        }

        path = props.getProperty(PROPERTY_COVERAGE_CONFIG);
        if (path != null)
        {
            b = b.withCoverageConfig(Paths.get(path));
        }

        return b;
    }


    @Override
    public FusionRuntimeBuilder withConfigProperties(URL resource)
        throws IOException
    {
        if (resource == null) return this;

        Properties props = readProperties(resource);
        return withConfigProperties(props);
    }


    @Override
    public FusionRuntimeBuilder withConfigProperties(Class<?> classForLoading,
                                                     String resourceName)
        throws IOException
    {
        URL url = classForLoading.getResource(resourceName);
        return withConfigProperties(url);
    }


    //=========================================================================


    @Override
    public String getDefaultLanguage()
    {
        return myDefaultLanguage;
    }


    @Override
    public void setDefaultLanguage(String absoluteModulePath)
    {
        mutationCheck();

        if (! isValidAbsoluteModulePath(absoluteModulePath))
        {
            String message =
                "Not a valid absolute module path: " + absoluteModulePath;
            throw new IllegalArgumentException(message);
        }

        myDefaultLanguage = absoluteModulePath;
    }


    @Override
    public FusionRuntimeBuilder withDefaultLanguage(String absoluteModulePath)
    {
        FusionRuntimeBuilder b = mutable();
        b.setDefaultLanguage(absoluteModulePath);
        return b;
    }


    //=========================================================================


    @Override
    public IonCatalog getDefaultIonCatalog()
    {
        return myDefaultIonCatalog;
    }


    @Override
    public void setDefaultIonCatalog(IonCatalog catalog)
    {
        mutationCheck();

        myDefaultIonCatalog = catalog;
    }


    @Override
    public FusionRuntimeBuilder withDefaultIonCatalog(IonCatalog catalog)
    {
        FusionRuntimeBuilder b = mutable();
        b.setDefaultIonCatalog(catalog);
        return b;
    }


    //=========================================================================


    @Override
    public OutputStream getInitialCurrentOutputPort()
    {
        return myCurrentOutputPort;
    }


    @Override
    public void setInitialCurrentOutputPort(OutputStream out)
    {
        mutationCheck();
        myCurrentOutputPort = out;
    }


    @Override
    public final FusionRuntimeBuilder withInitialCurrentOutputPort(OutputStream out)
    {
        FusionRuntimeBuilder b = mutable();
        b.setInitialCurrentOutputPort(out);
        return b;
    }


    //=========================================================================


    @Override
    public File getInitialCurrentDirectory()
    {
        return myCurrentDirectory;
    }


    @Override
    public void setInitialCurrentDirectory(File directory)
    {
        mutationCheck();

        if (directory != null)
        {
            if (! directory.isAbsolute())
            {
                directory = directory.getAbsoluteFile();
            }

            if (! directory.isDirectory())
            {
                String message = "Argument is not a directory: " + directory;
                throw new IllegalArgumentException(message);
            }
        }

        myCurrentDirectory = directory;
    }


    @Override
    public final FusionRuntimeBuilder withInitialCurrentDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setInitialCurrentDirectory(directory);
        return b;
    }


    //=========================================================================


    @Override
    public final File[] getRepositoryDirectories()
    {
        return myRepositoryDirectories == null ? null : myRepositoryDirectories.clone();
    }


    @Override
    public final void addRepositoryDirectory(File directory)
    {
        mutationCheck();

        File original = directory;

        if (! directory.isAbsolute())
        {
            directory = directory.getAbsoluteFile();
        }

        if (! directory.isDirectory())
        {
           String message = "Repository is not a directory: " + original;
           throw new IllegalArgumentException(message);
        }

        if (myRepositoryDirectories == null)
        {
            myRepositoryDirectories = new File[] { directory };
        }
        else
        {
            int len = myRepositoryDirectories.length;
            myRepositoryDirectories =
                Arrays.copyOf(myRepositoryDirectories, len + 1);
            myRepositoryDirectories[len] = directory;
        }
    }


    @Override
    public final FusionRuntimeBuilder withRepositoryDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.addRepositoryDirectory(directory);
        return b;
    }


    //=========================================================================
    // Code Coverage instrumentation


    CoverageCollector getCoverageCollector()
    {
        return myCollector;
    }


    void setCoverageCollector(CoverageCollector collector)
    {
        mutationCheck();
        myCollector = collector;
    }


    @Override
    public File getCoverageDataDirectory()
    {
        return myCoverageDataDirectory;
    }


    @Override
    public void setCoverageDataDirectory(File directory)
    {
        mutationCheck();

        if (directory != null)
        {
            File original = directory;

            if (! directory.isAbsolute())
            {
                directory = directory.getAbsoluteFile();
            }

            if (directory.exists() && ! directory.isDirectory())
            {
                String message = "Not a directory: " + original;
                throw new IllegalArgumentException(message);
            }
        }

        myCoverageDataDirectory = directory;
    }


    @Override
    public final FusionRuntimeBuilder withCoverageDataDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setCoverageDataDirectory(directory);
        return b;
    }


    @Override
    public Path getCoverageConfig()
    {
        return myCoverageConfigFile;
    }


    @Override
    public void setCoverageConfig(Path configFile)
    {
        mutationCheck();

        if (configFile != null)
        {
            Path original = configFile;

            if (! configFile.isAbsolute())
            {
                configFile = configFile.toAbsolutePath();
            }

            if (! isRegularFile(configFile) || ! isReadable(configFile))
            {
                String message = "Not a readable file: " + original;
                throw new IllegalArgumentException(message);
            }
        }

        myCoverageConfigFile = configFile;
    }


    @Override
    public final FusionRuntimeBuilder withCoverageConfig(Path configFile)
    {
        FusionRuntimeBuilder b = mutable();
        b.setCoverageConfig(configFile);
        return b;
    }

    //=========================================================================


    /** NOT FOR APPLICATION USE */
    boolean isDocumenting()
    {
        return myDocumenting;
    }

    /** NOT FOR APPLICATION USE */
    void setDocumenting(boolean documenting)
    {
        mutationCheck();
        myDocumenting = documenting;
    }


    //=========================================================================


    private StandardFusionRuntimeBuilder fillDefaults()
        throws IOException
    {
        // Ensure that we don't modify the user's builder.
        StandardFusionRuntimeBuilder b = copy();

        if (b.getInitialCurrentOutputPort() == null)
        {
            b.setInitialCurrentOutputPort(System.out);
        }

        if (b.getInitialCurrentDirectory() == null)
        {
            String userDir = System.getProperty("user.dir", "");
            if (userDir.isEmpty())
            {
                String message =
                    "Unable to determine working directory: " +
                    "the JDK system property user.dir is not set.";
                throw new IllegalStateException(message);
            }

            // Don't change the caller's instance
            b.setInitialCurrentDirectory(new File(userDir));
        }

        if (b.getCoverageCollector() == null)
        {
            if (b.getCoverageConfig() == null)
            {
                String property = PROPERTY_COVERAGE_CONFIG;
                String path = System.getProperty(property);
                if (path != null)
                {
                    Path file = Paths.get(path);
                    if (! isRegularFile(file) || ! isReadable(file))
                    {
                        String message =
                            "Value of system property " + property +
                            " is not a readable file: " + path;
                        throw new IllegalStateException(message);
                    }

                    b.setCoverageConfig(file);
                }
            }

            if (b.myCoverageDataDirectory == null)
            {
                String property = PROPERTY_COVERAGE_DATA_DIR;
                String path = System.getProperty(property);
                if (path != null)
                {
                    File file = new File(path);
                    if (file.exists() && ! file.isDirectory())
                    {
                        String message =
                            "Value of system property " + property +
                            " is not a directory: " + path;
                        throw new IllegalStateException(message);
                    }

                    b.setCoverageDataDirectory(file);
                }
            }

            if (b.myCoverageDataDirectory != null)
            {
                CoverageCollectorImpl c;

                if (b.myCoverageConfigFile != null)
                {
                    CoverageConfiguration config = forConfigFile(b.myCoverageConfigFile);
                    c = fromDirectory(config, b.myCoverageDataDirectory.toPath());
                }
                else
                {
                    c = fromDirectory(b.myCoverageDataDirectory.toPath());
                }

                // Register the active repositories with the collector.
                // These are persisted in the coverage session, so the reporter
                // can scan them and identify files that haven't been covered.
                if (b.myRepositoryDirectories != null)
                {
                    for (File f : b.myRepositoryDirectories)
                    {
                        c.noteRepository(f);
                    }
                }

                b.setCoverageCollector(c);
            }
        }

        return b.immutable();
    }


    @Override
    public FusionRuntime build()
        throws IOException, IllegalStateException, FusionException
    {
        StandardFusionRuntimeBuilder b = fillDefaults();

        try
        {
            return new StandardRuntime(b);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    //=========================================================================


    static FusionRuntimeBuilder makeMutable()
    {
        return new Mutable();
    }


    private static final class Mutable
        extends StandardFusionRuntimeBuilder
    {
        public Mutable() { }

        public Mutable(StandardFusionRuntimeBuilder that)
        {
            super(that);
        }

        @Override
        public StandardFusionRuntimeBuilder immutable()
        {
            return new StandardFusionRuntimeBuilder(this);
        }

        @Override
        public StandardFusionRuntimeBuilder mutable()
        {
            return this;
        }

        @Override
        void mutationCheck()
        {
        }
    }
}
