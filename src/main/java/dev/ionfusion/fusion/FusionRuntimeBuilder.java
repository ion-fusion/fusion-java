// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.ModuleIdentity.isValidAbsoluteModulePath;
import static dev.ionfusion.fusion._Private_CoverageCollectorImpl.fromDirectory;

import com.amazon.ion.IonCatalog;
import com.amazon.ion.system.SimpleCatalog;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Builder for acquiring a {@link FusionRuntime}.
 * <p>
 * <b>Instances of this class are not thread-safe unless
 * they are {@linkplain #immutable() immutable}.</b>
 *
 * <h2>Configuration Properties</h2>
 *
 * This builder provides several configuration points that determine the
 * capabilities of the resulting runtime system.
 * <p>
 * Configuration properties follow the standard JavaBeans idiom in order to be
 * friendly to dependency injection systems. They also provide alternative
 * mutation methods that enable a more fluid style:
 *<pre>
 *    FusionRuntime runtime =
 *        FusionRuntimeBuilder.standard()
 *                            .withRepositoryDirectory(repo)
 *                            .build();
 *</pre>
 *
 * <h3 id="bootrepo">Bootstrap Repository (Deprecated)</h3>
 *
 * Previous releases of this package required configuring a bootstrap repository
 * housing the primary resources needed by the runtime.  This is no longer
 * needed.
 * <p>
 * If a bootstrap repository is configured, either explicitly via this class or
 * implicitly via the {@value #PROPERTY_BOOTSTRAP_REPOSITORY} system property,
 * that directory will instead be used as the first user repository.
 *
 * <h3>User Repositories</h3>
 *
 * The repositories containing application and library modules can be
 * configured via {@link #addRepositoryDirectory(File)}. This gives the runtime
 * additional places to look for modules and other resources. In general,
 * resources are discovered by searching the bootstrap repository first, then
 * searching other repositories in the order they were declared.
 *
 * <h3>Default Language</h3>
 *
 * One of the runtime's main responsibilities is creation of {@link TopLevel}
 * namespaces in which evaluation can occur. The baseline semantics of such
 * evaluation is defined by the language used to create the namespace.
 * By default, all such namespaces are created with the bindings from the
 * {@code /fusion} language, but this default is controlled by the
 * configuration declared here.
 *
 * <h3>Default Ion Catalog</h3>
 *
 * An {@link IonCatalog} provides shared symbol tables used to encode and read
 * Ion binary data streams.  Applications can provide a default catalog that's
 * populated, or can be populated on-demand, with any necessary shared symbol
 * tables.  If not configured when {@link #build()} is called, the builder
 * creates an empty {@link SimpleCatalog}.
 *
 * <h3>Initial Current Directory</h3>
 *
 * Fusion's {@code current_directory} parameter holds the current working
 * directory used by many IO operations. The runtime can be configured with a
 * specific default value. If not configured when {@link #build()} is called,
 * the builder uses the value of Java's {@code "user.dir"} system property.
 *
 * <h3>Initial Current Output Port</h3>
 *
 * Fusion's various output procedures ({@code write}, {@code display},
 * {@code ionize}, <em>etc.</em>) send their data to a byte-oriented
 * <em>output port</em>.  By default, this goes to {@link System#out}, but the
 * runtime can be configured to use another {@link OutputStream}.
 *
 * <h3 id="coverage">Code Coverage Instrumentation</h3>
 *
 * To instruct the runtime to collect code coverage metrics, you must use
 * {@link #setCoverageDataDirectory(File)} to declare a directory storing the
 * collected data along with relevant configuration.  If the directory already
 * contains coverage metrics, it is loaded and updated by the runtime (as
 * opposed to being replaced by fresh metrics).
 * <p>
 * If the property is not configured when {@link #build()} is called,
 * a default value is read from the {@linkplain System#getProperties()
 * system properties} using the key {@value #PROPERTY_COVERAGE_DATA_DIR}.
 * If no such system property is configured, then no coverage metrics will be
 * collected.
 */
public class FusionRuntimeBuilder
{
    /**
     * The property used to configure the <a href="#bootrepo">bootstrap
     * repository</a>: {@value}.
     *
     * @deprecated Bootstrap configuration is no longer needed.
     */
    @Deprecated
    public static final String PROPERTY_BOOTSTRAP_REPOSITORY =
        "dev.ionfusion.fusion.BootstrapRepository";

    /**
     * The property used to configure the <a href="#coverage">code coverage
     * data directory</a>: {@value}.
     */
    public static final String PROPERTY_COVERAGE_DATA_DIR =
        "dev.ionfusion.fusion.coverage.DataDir";

    private static final String STANDARD_DEFAULT_LANGUAGE = "/fusion";


    /**
     * The standard builder of {@link FusionRuntime}s, with all configuration
     * properties having their default values.
     *
     * @return a new, mutable builder instance.
     */
    public static FusionRuntimeBuilder standard()
    {
        return new FusionRuntimeBuilder.Mutable();
    }


    //=========================================================================


    private OutputStream myCurrentOutputPort;
    private File         myCurrentDirectory;
    private File         myBootstrapRepository;
    private File[]       myRepositoryDirectories;
    private String       myDefaultLanguage = STANDARD_DEFAULT_LANGUAGE;
    private IonCatalog   myDefaultIonCatalog;

    private File                       myCoverageDataDirectory;
    private _Private_CoverageCollector myCollector;

    private boolean myDocumenting;


    private FusionRuntimeBuilder() { }

    private FusionRuntimeBuilder(FusionRuntimeBuilder that)
    {
        this.myCurrentOutputPort     = that.myCurrentOutputPort;
        this.myCurrentDirectory      = that.myCurrentDirectory;
        this.myBootstrapRepository   = that.myBootstrapRepository;
        this.myRepositoryDirectories = that.myRepositoryDirectories;
        this.myDefaultLanguage       = that.myDefaultLanguage;
        this.myDefaultIonCatalog     = that.myDefaultIonCatalog;
        this.myCoverageDataDirectory = that.myCoverageDataDirectory;
        this.myCollector             = that.myCollector;
        this.myDocumenting           = that.myDocumenting;
    }


    //=========================================================================

    /**
     * Creates a mutable copy of this builder.
     *
     * @return a new builder with the same configuration as {@code this}.
     */
    public final FusionRuntimeBuilder copy()
    {
        return new FusionRuntimeBuilder.Mutable(this);
    }

    /**
     * Returns an immutable builder configured exactly like this one.
     *
     * @return this instance, if immutable;
     * otherwise an immutable copy of this instance.
     */
    public FusionRuntimeBuilder immutable()
    {
        return this;
    }

    /**
     * Returns a mutable builder configured exactly like this one.
     *
     * @return this instance, if mutable;
     * otherwise a mutable copy of this instance.
     */
    public FusionRuntimeBuilder mutable()
    {
        return copy();
    }

    void mutationCheck()
    {
        throw new UnsupportedOperationException("This builder is immutable");
    }


    //=========================================================================


    /**
     * Configures a builder from the given properties.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY} (deprecated)
     *       invokes {@link #setBootstrapRepository(File)}.
     *   <li>{@value #PROPERTY_COVERAGE_DATA_DIR}
     *       invokes {@link #setCoverageDataDirectory(File)}.
     * </ul>
     *
     * @param props must not be null.
     *
     * @return this builder, if it's mutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem applying the properties.
     */
    public FusionRuntimeBuilder withConfigProperties(Properties props)
        throws FusionException
    {
        FusionRuntimeBuilder b = this;

        try
        {
            String path = props.getProperty(PROPERTY_BOOTSTRAP_REPOSITORY);
            if (path != null)
            {
                File f = new File(path);
                b = b.withBootstrapRepository(f);
            }

            path = props.getProperty(PROPERTY_COVERAGE_DATA_DIR);
            if (path != null)
            {
                File f = new File(path);
                b = b.withCoverageDataDirectory(f);
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new FusionException(e.getMessage());
        }

        return b;
    }


    /**
     * Configures a builder from properties at the given URL.
     * If no such resource exists, then no configuration changes are made.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY} (deprecated)
     *       invokes {@link #setBootstrapRepository(File)}.
     *   <li>{@value #PROPERTY_COVERAGE_DATA_DIR}
     *       invokes {@link #setCoverageDataDirectory(File)}.
     * </ul>
     *
     * @param resource may be null, in which case no configuration happens.

     * @return this builder, if it's mutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem reading the resource or
     * applying the properties.
     */
    public FusionRuntimeBuilder withConfigProperties(URL resource)
        throws FusionException
    {
        if (resource == null) return this;

        Properties props = FusionUtils.readProperties(resource);
        return withConfigProperties(props);
    }


    /**
     * Configures a builder from properties in the given classloader resource.
     * The resource is located as follows:
     *<pre>
     *    classForLoading.getResource(resourceName)
     *</pre>
     * If no such resource exists, then no configuration changes are made.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_BOOTSTRAP_REPOSITORY}
     *       invokes {@link #setBootstrapRepository(File)}.
     *   <li>{@value #PROPERTY_COVERAGE_DATA_DIR}
     *       invokes {@link #setCoverageDataDirectory(File)}.
     * </ul>
     *
     * @param classForLoading the class to use for loading the properties file
     * @param resourceName the name of the properties file
     *
     * @return this builder, if it's mutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws FusionException if there's a problem reading the resource or
     * applying the properties.
     *
     * @see Class#getResource(String)
     */
    public FusionRuntimeBuilder withConfigProperties(Class<?> classForLoading,
                                                     String resourceName)
        throws FusionException
    {
        URL url = classForLoading.getResource(resourceName);
        return withConfigProperties(url);
    }


    //=========================================================================


    /**
     * Gets the default language used to bootstrap the runtime's
     * {@link TopLevel} namespaces.
     * The standard value of this property is {@code "/fusion"}.
     *
     * @return an absolute module path.
     *
     * @see #setDefaultLanguage(String)
     * @see #withDefaultLanguage(String)
     */
    public String getDefaultLanguage()
    {
        return myDefaultLanguage;
    }


    /**
     * Sets the default language used to bootstrap the runtime's
     * {@link TopLevel} namespaces.
     *
     * @param absoluteModulePath identifies the language; must not be null.
     *
     * @see #getDefaultLanguage()
     * @see #withDefaultLanguage(String)
     */
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


    /**
     * Declares the default language used to bootstrap the runtime's
     * {@link TopLevel} namespaces.
     *
     * @param absoluteModulePath identifies the language; must not be null.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #getDefaultLanguage()
     * @see #setDefaultLanguage(String)
     */
    public FusionRuntimeBuilder withDefaultLanguage(String absoluteModulePath)
    {
        FusionRuntimeBuilder b = mutable();
        b.setDefaultLanguage(absoluteModulePath);
        return b;
    }


    //=========================================================================


    /**
     * Gets the default Ion symbol table catalog used with Ion binary data.
     * By default, this property is null.
     *
     * @return an Ion symbol table catalog. May be null, which means the builder
     * will create a new {@link SimpleCatalog} when {@link #build()} is called.
     *
     * @see #setDefaultIonCatalog(IonCatalog)
     * @see #withDefaultIonCatalog(IonCatalog)
     */
    public IonCatalog getDefaultIonCatalog()
    {
        return myDefaultIonCatalog;
    }


    /**
     * Sets the default Ion symbol table catalog used with Ion binary data.
     *
     * @param catalog may be null, which causes the builder to create a new
     * {@link SimpleCatalog} when {@link #build()} is called.
     *
     * @see #getDefaultIonCatalog()
     * @see #withDefaultIonCatalog(IonCatalog)
     */
    public void setDefaultIonCatalog(IonCatalog catalog)
    {
        mutationCheck();

        myDefaultIonCatalog = catalog;
    }


    /**
     * Declares the default Ion symbol table catalog used with Ion binary data,
     * returning a new mutable builder if this is immutable.
     *
     * @param catalog may be null, which causes the builder to create a new
     * {@link SimpleCatalog} when {@link #build()} is called.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #getDefaultIonCatalog()
     * @see #setDefaultIonCatalog(IonCatalog)
     */
    public FusionRuntimeBuilder withDefaultIonCatalog(IonCatalog catalog)
    {
        FusionRuntimeBuilder b = mutable();
        b.setDefaultIonCatalog(catalog);
        return b;
    }


    //=========================================================================


    /**
     * Gets the default output stream used by various output procedures.
     * By default, this property is null.
     *
     * @return an output stream. May be null, which means the builder
     * will use {@link System#out}.
     *
     * @see #setInitialCurrentOutputPort(OutputStream)
     * @see #withInitialCurrentOutputPort(OutputStream)
     */
    public OutputStream getInitialCurrentOutputPort()
    {
        return myCurrentOutputPort;
    }


    /**
     * Sets the default output stream used by various output procedures.
     *
     * @param out may be null, which causes the builder to use
     * {@link System#out}.
     *
     * @see #getInitialCurrentOutputPort()
     * @see #withInitialCurrentOutputPort(OutputStream)
     */
    public void setInitialCurrentOutputPort(OutputStream out)
    {
        mutationCheck();
        myCurrentOutputPort = out;
    }


    /**
     * Declares the default output stream used by various output procedures,
     * returning a new mutable builder if this is immutable.
     *
     * @param out may be null, which causes the builder to use
     * {@link System#out}.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #getInitialCurrentOutputPort()
     * @see #setInitialCurrentOutputPort(OutputStream)
     */
    public final FusionRuntimeBuilder withInitialCurrentOutputPort(OutputStream out)
    {
        FusionRuntimeBuilder b = mutable();
        b.setInitialCurrentOutputPort(out);
        return b;
    }


    //=========================================================================


    /**
     * Gets the initial value of the {@code current_directory} parameter,
     * which is the working directory for Fusion code.
     *
     * @return an absolute path. May be null, which means the builder will use
     * the {@code "user.dir"} JVM system property when {@link #build()} is
     * called.
     *
     * @see #setInitialCurrentDirectory(File)
     * @see #withInitialCurrentDirectory(File)
     */
    public File getInitialCurrentDirectory()
    {
        return myCurrentDirectory;
    }


    /**
     * Sets the initial value of the {@code current_directory} parameter,
     * which is the working directory for Fusion code.
     *
     * @param directory may be null, which causes the builder to use the
     * {@code "user.dir"} JVM system property when {@link #build()} is called.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @see #getInitialCurrentDirectory()
     * @see #withInitialCurrentDirectory(File)
     */
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


    /**
     * Declares the initial value of the {@code current_directory} parameter,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory may be null, which causes the builder to use the
     * {@code "user.dir"} JVM system property when {@link #build()} is called.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #getInitialCurrentDirectory()
     * @see #setInitialCurrentDirectory(File)
     */
    public final FusionRuntimeBuilder
    withInitialCurrentDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setInitialCurrentDirectory(directory);
        return b;
    }


    //=========================================================================


    /**
     * Gets the directory configured for use as the first user repository,
     * independent of other calls to {@link #addRepositoryDirectory},
     * {@link #withRepositoryDirectory}, and so on.
     * By default, this property is null.
     *
     * @return the configured directory, or null if one has not been
     * configured.
     *
     * @see #setBootstrapRepository(File)
     * @see #withBootstrapRepository(File)
     *
     * @deprecated A bootstrap repository is no longer needed.
     */
    @Deprecated
    public File getBootstrapRepository()
    {
        return myBootstrapRepository;
    }


    /**
     * Sets the directory to be used as the first user repository.
     *
     * @param directory the desired repository.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @throws UnsupportedOperationException if this is immutable.
     * @throws IllegalArgumentException if the directory isn't a valid
     * repository.
     *
     * @see #getBootstrapRepository()
     * @see #withBootstrapRepository(File)
     *
     * @deprecated A bootstrap repository is no longer needed.
     *   Use {@link #addRepositoryDirectory(File)} instead.
     */
    @Deprecated
    public void setBootstrapRepository(File directory)
    {
        mutationCheck();

        if (directory != null)
        {
            File original = directory;

            if (! directory.isAbsolute())
            {
                directory = directory.getAbsoluteFile();
            }

            if (! directory.isDirectory())
            {
                String message = "Not a directory: " + original;
                throw new IllegalArgumentException(message);
            }
        }

        myBootstrapRepository = directory;
    }


    /**
     * Declares the directory to be used as the first user repository,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory the desired repository.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @throws IllegalArgumentException if the directory isn't a valid
     * repository.
     *
     * @see #getBootstrapRepository()
     * @see #setBootstrapRepository(File)
     *
     * @deprecated A bootstrap repository is no longer needed.
     *   Use {@link #withRepositoryDirectory(File)} instead.
     */
    @Deprecated
    public final FusionRuntimeBuilder withBootstrapRepository(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setBootstrapRepository(directory);
        return b;
    }


    //=========================================================================


    /**
     * Declares a repository from which Fusion modules are loaded.
     * Repositories are searched in the order they are declared.
     *
     * @param directory must be a path to a readable directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @throws UnsupportedOperationException if this is immutable.
     *
     * @see #withRepositoryDirectory(File)
     */
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

        File src = new File(directory, "src");
        if (! src.isDirectory())
        {
           String message = "Repository has no src directory: " + original;
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


    /**
     * Declares a repository from which Fusion modules are loaded,
     * returning a new mutable builder if this is immutable.
     * Repositories are searched in the order they are declared.
     *
     * @param directory must be a path to a readable directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #addRepositoryDirectory(File)
     */
    public final FusionRuntimeBuilder withRepositoryDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.addRepositoryDirectory(directory);
        return b;
    }


    //=========================================================================
    // Code Coverage instrumentation


    /**
     * Gets the directory to which code-coverage metrics will be added.
     * By default, this property is null.
     *
     * @return an absolute path, or null if a directory has not been
     * configured.
     *
     * @see #setCoverageDataDirectory(File)
     * @see #withCoverageDataDirectory(File)
     */
    public File getCoverageDataDirectory()
    {
        return myCoverageDataDirectory;
    }


    /**
     * Sets the directory to which code-coverage metrics will be added.
     *
     * @param directory the desired coverage data directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @throws UnsupportedOperationException if this is immutable.
     * @throws IllegalArgumentException if the file exists but isn't a
     * directory.
     *
     * @see #getCoverageDataDirectory()
     * @see #withCoverageDataDirectory(File)
     */
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


    /**
     * Declares the directory to which code-coverage metrics will be added,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory the desired coverage data directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously-configured directory.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @throws IllegalArgumentException if the file exists but isn't a
     * directory.
     *
     * @see #getCoverageDataDirectory()
     * @see #setCoverageDataDirectory(File)
     */
    public final FusionRuntimeBuilder withCoverageDataDirectory(File directory)
    {
        FusionRuntimeBuilder b = mutable();
        b.setCoverageDataDirectory(directory);
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



    /** NOT FOR APPLICATION USE */
    _Private_CoverageCollector getCoverageCollector()
    {
        return myCollector;
    }

    /**
     * NOT FOR APPLICATION USE!
     * Used by unit tests to inject a mock collector.
     */
    void setCoverageCollector(_Private_CoverageCollector collector)
    {
        mutationCheck();
        myCollector = collector;
    }


    //=========================================================================


    private FusionRuntimeBuilder fillDefaults()
        throws FusionException
    {
        // Ensure that we don't modify the user's builder.
        FusionRuntimeBuilder b = copy();

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

        if (b.getBootstrapRepository() == null)
        {
            String property = PROPERTY_BOOTSTRAP_REPOSITORY;
            String bootstrap = System.getProperty(property);
            if (bootstrap != null)
            {
                File file = new File(bootstrap);
                // Directory-ness is verified below by setBootstrapRepository,
                // but we want to include the property in the error message.
                if (! file.isDirectory())
                {
                    String message =
                        "Value of system property " + property +
                        " is not a directory: " + bootstrap;
                    throw new IllegalStateException(message);
                }

                b.setBootstrapRepository(file);
            }
        }

        if (b.getCoverageCollector() == null)
        {
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

            // TODO Writing into a private coverageCollector property is wierd.
            //   This should move into build(), or in getCoverageCollector()?
            //   Note that the property exists so tests can inject a mock.
            if (b.myCoverageDataDirectory != null)
            {
                _Private_CoverageCollectorImpl c =
                    fromDirectory(b.myCoverageDataDirectory);

                // Register the active repositories with the collector.
                // These are persisted in the coverage.ion file, so we can later
                // scan them and identify files that haven't been covered.
                if (b.getBootstrapRepository() != null)
                {
                    c.noteRepository(b.getBootstrapRepository());
                }

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


    /**
     * Builds a new runtime based on the current configuration of this builder.
     *
     * @return a new builder instance.
     *
     * @throws IllegalStateException if the builder's configuration is
     * incomplete, inconsistent, or otherwise unusable.
     * @throws FusionException if there's a problem bootstrapping the runtime.
     */
    public FusionRuntime build()
        throws IllegalStateException, FusionException
    {
        try
        {
            FusionRuntimeBuilder b = fillDefaults();
            return new StandardRuntime(b);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    /**
     * NOT PUBLIC!
     *
     * @throws IllegalStateException if the builder's configuration is
     * incomplete, inconsistent, or otherwise unusable.
     */
    ModuleRepository[] buildModuleRepositories()
    {
        ArrayList<ModuleRepository> repos = new ArrayList<>();

        // When our own unit tests are running in an IDE, this is nonfunctional;
        // we rely on the test setup configuring the bootstrap explicitly.
        // TODO Need a better way to handle this for our own test cases so we
        //   can delete the bootstrap repo configuration point.
        repos.add(new ClassLoaderModuleRepository(getClass().getClassLoader(),
                                                  "FUSION-REPO"));

        // This supports legacy cases commingling user code with the bootstrap
        // and that only configure the bootstrap.
        if (myBootstrapRepository != null)
        {
            repos.add(new FileSystemModuleRepository(myBootstrapRepository));
        }

        if (myRepositoryDirectories != null)
        {
            for (File f : myRepositoryDirectories)
            {
                repos.add(new FileSystemModuleRepository(f));
            }
        }

        return repos.toArray(new ModuleRepository[0]);
    }


    //=========================================================================


    private static final class Mutable extends FusionRuntimeBuilder
    {
        public Mutable() { }

        public Mutable(FusionRuntimeBuilder that)
        {
            super(that);
        }

        @Override
        public FusionRuntimeBuilder immutable()
        {
            return new FusionRuntimeBuilder(this);
        }

        @Override
        public FusionRuntimeBuilder mutable()
        {
            return this;
        }

        @Override
        void mutationCheck()
        {
        }
    }
}
