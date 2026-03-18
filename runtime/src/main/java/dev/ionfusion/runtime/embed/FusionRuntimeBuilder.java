// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.embed;

import com.amazon.ion.IonCatalog;
import com.amazon.ion.system.SimpleCatalog;
import dev.ionfusion.fusion._Private_Trampoline;
import dev.ionfusion.runtime.base.FusionException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
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
 * Configuration properties follow standard JavaBeans idioms to be
 * friendly to dependency injection systems. They also provide alternative
 * mutation methods that enable a more fluid style:
 *<pre>
 *    FusionRuntime runtime =
 *        FusionRuntimeBuilder.standard()
 *                            .withRepositoryDirectory(repo)
 *                            .build();
 *</pre>
 *
 * <h3>Repositories</h3>
 *
 * The repositories containing application and library modules can be
 * configured via {@link #addRepositoryDirectory(File)}. This gives the runtime
 * additional places to look for modules and other resources. In general,
 * resources are discovered by searching the runtime repository first, then
 * searching other repositories in the order they were declared.
 *
 * <h3>Default Language</h3>
 *
 * One of the runtime's main responsibilities is creation of {@link TopLevel}
 * namespaces in which evaluation can occur. The baseline semantics of such
 * evaluation are defined by the language used to create the namespace.
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
 * <p>
 * The extent of coverage instrumentation is controlled by a configuration file
 * that can be independent of the coverage database.
 *
 */
public interface FusionRuntimeBuilder
{
    /**
     * The property used to configure the <a href="#coverage">code coverage
     * data directory</a>: {@value}.
     */
    String PROPERTY_COVERAGE_DATA_DIR = "dev.ionfusion.fusion.coverage.DataDir";

    /**
     * The property used to configure the extent of <a href="#coverage">code
     * coverage instrumentation</a>: {@value}.
     */
    String PROPERTY_COVERAGE_CONFIG   = "dev.ionfusion.fusion.coverage.Config";

    /**
     * The standard builder of {@link FusionRuntime}s, with all configuration
     * properties having their default values.
     *
     * @return a new, mutable builder instance.
     */
    public static FusionRuntimeBuilder standard()
    {
        return _Private_Trampoline.makeStandardRuntimeBuilder();
    }


    //=========================================================================


    /**
     * Creates a mutable copy of this builder.
     *
     * @return a new builder with the same configuration as {@code this}.
     */
    FusionRuntimeBuilder copy();

    /**
     * Returns an immutable builder configured exactly like this one.
     *
     * @return this instance, if immutable;
     * otherwise an immutable copy of this instance.
     */
    FusionRuntimeBuilder immutable();

    /**
     * Returns a mutable builder configured exactly like this one.
     *
     * @return this instance, if mutable;
     * otherwise a mutable copy of this instance.
     */
    FusionRuntimeBuilder mutable();


    //=========================================================================


    /**
     * Configures a builder from the given properties.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_COVERAGE_DATA_DIR}
     *       invokes {@link #setCoverageDataDirectory(File)}.
     * </ul>
     *
     * @param props must not be null.
     *
     * @return this builder, if it's mutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws IllegalArgumentException if there's a problem applying the properties.
     */
    FusionRuntimeBuilder withConfigProperties(Properties props);


    /**
     * Configures a builder from properties at the given URL.
     * If no such resource exists, then no configuration changes are made.
     * <p>
     * These properties are observed:
     * <ul>
     *   <li>{@value #PROPERTY_COVERAGE_DATA_DIR}
     *       invokes {@link #setCoverageDataDirectory(File)}.
     * </ul>
     *
     * @param resource may be null, in which case no configuration happens.

     * @return this builder, if it's mutable or if no properties were
     * recognized; otherwise a new mutable builder.
     *
     * @throws IOException if there's a problem reading the resource.
     * @throws IllegalArgumentException if there's a problem applying the properties.
     */
    FusionRuntimeBuilder withConfigProperties(URL resource)
        throws IOException;


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
     * @throws IOException if there's a problem reading the resource.
     * @throws IllegalArgumentException if there's a problem applying the properties.
     *
     * @see Class#getResource(String)
     */
    FusionRuntimeBuilder withConfigProperties(Class<?> classForLoading,
                                              String resourceName)
        throws IOException;


    //=========================================================================


    /**
     * Gets the default language used to initialize the runtime's
     * {@link TopLevel} namespaces.
     * The standard value of this property is {@code "/fusion"}.
     *
     * @return an absolute module path.
     *
     * @see #setDefaultLanguage(String)
     * @see #withDefaultLanguage(String)
     */
    String getDefaultLanguage();


    /**
     * Sets the default language used to initialize the runtime's
     * {@link TopLevel} namespaces.
     *
     * @param absoluteModulePath identifies the language; must not be null.
     *
     * @see #getDefaultLanguage()
     * @see #withDefaultLanguage(String)
     */
    void setDefaultLanguage(String absoluteModulePath);


    /**
     * Declares the default language used to initialize the runtime's
     * {@link TopLevel} namespaces.
     *
     * @param absoluteModulePath identifies the language; must not be null.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @see #getDefaultLanguage()
     * @see #setDefaultLanguage(String)
     */
    FusionRuntimeBuilder withDefaultLanguage(String absoluteModulePath);


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
    IonCatalog getDefaultIonCatalog();


    /**
     * Sets the default Ion symbol table catalog used with Ion binary data.
     *
     * @param catalog may be null, which causes the builder to create a new
     * {@link SimpleCatalog} when {@link #build()} is called.
     *
     * @see #getDefaultIonCatalog()
     * @see #withDefaultIonCatalog(IonCatalog)
     */
    void setDefaultIonCatalog(IonCatalog catalog);


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
    FusionRuntimeBuilder withDefaultIonCatalog(IonCatalog catalog);


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
    OutputStream getInitialCurrentOutputPort();


    /**
     * Sets the default output stream used by various output procedures.
     *
     * @param out may be null, which causes the builder to use {@link System#out}.
     *
     * @see #getInitialCurrentOutputPort()
     * @see #withInitialCurrentOutputPort(OutputStream)
     */
    void setInitialCurrentOutputPort(OutputStream out);


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
    FusionRuntimeBuilder withInitialCurrentOutputPort(OutputStream out);


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
    File getInitialCurrentDirectory();


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
    void setInitialCurrentDirectory(File directory);


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
    FusionRuntimeBuilder withInitialCurrentDirectory(File directory);


    //=========================================================================


    /**
     * Gets the directories from which Fusion modules can be loaded.
     *
     * @return the array of directories, or null if none are configured.
     *
     * @see #addRepositoryDirectory(File)
     * @see #withRepositoryDirectory(File)
     */
    File[] getRepositoryDirectories();


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
    void addRepositoryDirectory(File directory);


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
    FusionRuntimeBuilder withRepositoryDirectory(File directory);


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
    File getCoverageDataDirectory();


    /**
     * Sets the directory to which code-coverage metrics will be added.
     *
     * @param directory the desired coverage data directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously configured directory.
     *
     * @throws UnsupportedOperationException if this is immutable.
     * @throws IllegalArgumentException if the file exists but isn't a
     * directory.
     *
     * @see #getCoverageDataDirectory()
     * @see #withCoverageDataDirectory(File)
     */
    void setCoverageDataDirectory(File directory);


    /**
     * Declares the directory to which code-coverage metrics will be added,
     * returning a new mutable builder if this is immutable.
     *
     * @param directory the desired coverage data directory.
     * If a relative path is given, it is immediately resolved as per
     * {@link File#getAbsolutePath()}.
     * May be null to clear a previously configured directory.
     *
     * @return this builder, if it's mutable; otherwise a new mutable builder.
     *
     * @throws IllegalArgumentException if the file exists but isn't a
     * directory.
     *
     * @see #getCoverageDataDirectory()
     * @see #setCoverageDataDirectory(File)
     */
    FusionRuntimeBuilder withCoverageDataDirectory(File directory);


    Path getCoverageConfig();

    void setCoverageConfig(Path configFile);

    FusionRuntimeBuilder withCoverageConfig(Path configFile);


    //=========================================================================


    /**
     * Builds a new runtime based on the current configuration of this builder.
     *
     * @return a new builder instance.
     *
     * @throws IOException if there are problems reading the coverage database
     * or other configuration files.
     * @throws IllegalStateException if the builder's configuration is
     * incomplete, inconsistent, or otherwise unusable.
     * @throws FusionException if there's a problem bootstrapping the runtime.
     */
    FusionRuntime build()
        throws IOException, IllegalStateException, FusionException;
}
