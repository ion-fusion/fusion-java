// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.ModuleIdentity.isValidAbsoluteModulePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Holds configuration for code-coverage metrics collection and reporting.
 * <p>
 * Configuration is performed via a {@value #CONFIG_FILE_NAME} file
 * inside the data directory. This makes it easy to configure from external
 * build scripts.
 *
 * <h2>Coverage Filtering</h2>
 *
 * The collector can be configured to select what Fusion code is instrumented.
 * Coverage filtering is based on three configuration properties:
 * <ul>
 *   <li>{@value #PROPERTY_INCLUDED_MODULES} holds a list of absolute module
 *       paths, separated by ':'.
 *       Any repository-loaded module at or under a given path is instrumented
 *       (unless excluded).
 *       If not provided, all repository-loaded modules are included.
 *       If empty, then no repository-loaded modules are instrumented.
 *   <li>{@value #PROPERTY_EXCLUDED_MODULES} holds a list of absolute module
 *       paths, separated by ':'.
 *       Any repository-loaded module, included with respect to
 *       {@value #PROPERTY_INCLUDED_MODULES}, that is at or under a given path
 *       is <em>not</em> instrumented.
 *       If empty or not provided, all included modules are instrumented.
 *   <li>{@value #PROPERTY_INCLUDED_SOURCES} holds a list of directory paths,
 *       separated by the platform path separator.
 *       All code read from files under a given directory is instrumented.
 *       This is generally used to instrument non-repository scripts, such as
 *       unit tests, that are evaluated via {@code load}, as opposed to
 *       {@code require}d modules.
 * </ul>
 * Modules declared encountered while {@code load}ing scripts are assigned
 * synthetic, unresolvable module paths that cannot be selected via the
 * module-based properties above.
 */
final class CoverageConfiguration
{
    private static final String CONFIG_FILE_NAME          = "config.properties";
    private static final String PROPERTY_INCLUDED_MODULES = "IncludedModules";
    private static final String PROPERTY_EXCLUDED_MODULES = "ExcludedModules";
    private static final String PROPERTY_INCLUDED_SOURCES = "IncludedSources";


    final Predicate<ModuleIdentity> myModuleSelector;

    /**
     * Each element is an absolute path to a directory.
     */
    private final Set<Path> myIncludedSourceDirs = new HashSet<>();


    /**
     * Parses a property's value as a colon-separated set of absolute module paths.
     *
     * @param configFile where the {@code props} came from; used only for error messages.
     * @param props must not be null.
     * @param propertyName must not be null.
     *
     * @return a set of absolute module paths, or null if the {@code propertyName}
     * doesn't have an entry in the {@code props}.
     *
     * @throws FusionException if an entry is not an absolute module path.
     */
    private static Set<String> readModuleSet(File       configFile,
                                             Properties props,
                                             String     propertyName)
        throws FusionException
    {
        String mods = props.getProperty(propertyName);
        if (mods == null) return null;

        Set<String> modules = new HashSet<>();

        for (String mod : mods.split(":"))
        {
            if (mod.isEmpty()) continue;

            if (isValidAbsoluteModulePath(mod))
            {
                modules.add(mod);
            }
            else
            {
                String message =
                    "Configuration error in " + configFile
                    + ": " + propertyName
                    + " contains an invalid absolute module path: " + mod;
                throw new FusionException(message);
            }
        }

        return modules;
    }


    CoverageConfiguration(File dataDir)
        throws FusionException, IOException
    {
        File myConfigFile = new File(dataDir, CONFIG_FILE_NAME);
        if (myConfigFile.exists())
        {
            Properties props = FusionUtils.readProperties(myConfigFile);

            myModuleSelector =
                new SimpleModuleIdentitySelector(myConfigFile, props);

            String sources = props.getProperty(PROPERTY_INCLUDED_SOURCES);
            if (sources != null)
            {
                for (String source : sources.split(File.pathSeparator))
                {
                    if (source.isEmpty()) continue;

                    Path p = Paths.get(source).toAbsolutePath();
                    if (Files.isDirectory(p))
                    {
                        myIncludedSourceDirs.add(p);
                    }
                    else
                    {
                        String message =
                            "Configuration error in " + myConfigFile
                            + ": " + PROPERTY_INCLUDED_SOURCES
                            + " contains an invalid directory: " + source;
                        throw new FusionException(message);
                    }
                }
            }
        }
        else
        {
            myModuleSelector = new TrueModuleIdentitySelector();
        }
    }


    //=========================================================================


    /**
     * @return not null.
     */
    Set<Path> getIncludedSourceDirs()
    {
        return myIncludedSourceDirs;
    }


    private boolean moduleIsSelected(ModuleIdentity id)
    {
        // If no module identity, we must match a requested source directory.
        if (id == null) return false;

        return myModuleSelector.test(id);
    }


    /**
     * A file is selected for coverage if our {@value #PROPERTY_INCLUDED_SOURCES}
     * property includes it or a directory containing it.
     *
     * @param file may be null.
     */
    boolean fileIsSelected(Path file)
    {
        return (file != null) && myIncludedSourceDirs.stream().anyMatch(file::startsWith);
    }


    /**
     * A {@link SourceLocation} is selected for coverage if it has a
     * {@link SourceName} and either its {@link ModuleIdentity} or its file is
     * selected.
     *
     * @param loc must not be null.
     *
     * @return true iff the location should be instrumented.
     */
    boolean locationIsSelected(SourceLocation loc)
    {
        SourceName name = loc.getSourceName();
        if (name == null) return false;

        ModuleIdentity id   = name.getModuleIdentity();
        Path           file = name.getPath();

        return (moduleIsSelected(id) || fileIsSelected(file));
    }


    //=========================================================================


    // TODO JAVA8 makes this unnecessary; use a trivial lambda instead.
    private static class TrueModuleIdentitySelector
        implements Predicate<ModuleIdentity>
    {
        @Override
        public boolean test(ModuleIdentity id)
        {
            return true;
        }
    }


    /**
     * A predicate testing whether a given module should be instrumented for
     * code coverage, based on included and excluded sets.
     */
    private static class SimpleModuleIdentitySelector
        implements Predicate<ModuleIdentity>
    {
        /**
         * All elements must be absolute module paths.
         */
        private final Set<String> myIncludedModules;

        /**
         * All elements must be absolute module paths.
         */
        private final Set<String> myExcludedModules;


        /**
         * Reads the included and excluded module sets from the properties.
         *
         * @param configFile where the {@code props} came from; used only for error messages.
         * @param props must not be null.
         *
         * @throws FusionException if an entry is not an absolute module path.
         */
        public SimpleModuleIdentitySelector(File configFile, Properties props)
            throws FusionException
        {
            myIncludedModules =
                readModuleSet(configFile, props, PROPERTY_INCLUDED_MODULES);
            myExcludedModules =
                readModuleSet(configFile, props, PROPERTY_EXCLUDED_MODULES);
        }


        private static boolean setContainsModule(Set<String> set, String path)
        {
            for (String coverable : set)
            {
                if (path.equals(coverable) || path.startsWith(coverable + '/'))
                {
                    return true;
                }
            }

            return false;
        }


        @Override
        public boolean test(ModuleIdentity id)
        {
            String path = id.absolutePath();

            // By default, all repository-loaded modules are included.
            if (myIncludedModules != null)
            {
                if (! setContainsModule(myIncludedModules, path)) return false;
            }

            if (myExcludedModules != null)
            {
                if (setContainsModule(myExcludedModules, path)) return false;
            }

            return true;
        }
    }
}
