// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.cover;

import static dev.ionfusion.fusion._Private_Trampoline.discoverModulesInRepository;
import static dev.ionfusion.runtime.base.SourceName.FUSION_SOURCE_EXTENSION;
import static java.nio.file.Files.walkFileTree;
import static java.util.stream.Collectors.toList;

import dev.ionfusion.runtime._private.cover.CoverageConfiguration;
import dev.ionfusion.runtime._private.cover.CoverageDatabase;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CoverageReport
{
    private final CoverageConfiguration myConfig;

    private final Map<URI, CoveredFile>              myCoveredFiles;
    private final Map<ModuleIdentity, CoveredModule> myCoveredModules;
    private final CoverageInfoPair                   myGlobalSummary;

    public CoverageReport(CoverageConfiguration config, CoverageDatabase db)
        throws FusionException, IOException
    {
        myConfig = config;
        myCoveredFiles = new HashMap<>();
        myCoveredModules = new HashMap<>();
        myGlobalSummary = new CoverageInfoPair();

        // WARNING: We currently can't do this more than once per report.  Doing so may
        // change preferred module sources, at the very least.
        loadDatabase(db);

        // Compute each entity's summary, as well as the global summary.
        summarize();
    }


    private void loadDatabase(CoverageDatabase db)
        throws FusionException, IOException
    {
        // Identify all the modules in recorded repositories.
        noteModulesInRepositories(db.getRepositories());

        // Identify all the scripts in the configured directories.
        // TODO Database should record covered dirs, like it does for modules.
        noteScripts(myConfig.getIncludedSourceDirs());

        // Traverse the database's instrumented sources, noting source files and
        // determining the preferred source of each module.
        db.sourceNames().forEach(this::noteSourceName);

        // With all module sources collected, we can note a single one for each module.
        myCoveredModules.values().forEach(this::notePreferredSourceOfModule);

        // With all source files identified, we can collect location metrics.
        db.forEachLocationCoverage(this::noteLocationCoverage);
    }


    public Collection<CoveredFile> sourceFiles()
    {
        return myCoveredFiles.values();
    }

    /**
     * Gets the source files that are not defining modules in repositories.
     */
    public Collection<CoveredFile> scripts()
    {
        return myCoveredFiles.values()
                             .stream()
                             .filter(CoveredFile::isScript)
                             .collect(toList());
    }

    public Collection<CoveredModule> modules()
    {
        return myCoveredModules.values();
    }


    //==================================================================================
    // Initial source discovery

    /**
     * Collect all the modules the repositories can discover, so we can find modules
     * that are not used and don't appear in the database.
     */
    private void noteModulesInRepositories(Set<File> repos)
        throws FusionException
    {
        for (File f : repos)
        {
            discoverModulesInRepository(f.toPath(),
                                        myConfig::moduleIsSelected,
                                        this::noteModule);
        }
    }


    /**
     * Ensures that we have a {@link CoveredModule} for the module.
     */
    private CoveredModule noteModule(ModuleIdentity id)
    {
        return myCoveredModules.computeIfAbsent(id, CoveredModule::new);
    }


    /**
     * Notes the file if it's selected by our configuration.
     */
    private void noteScript(Path file)
    {
        file = file.normalize();
        if (myConfig.fileIsSelected(file))
        {
            noteFile(file);
        }
    }

    /**
     * Ensures that we have a {@link CoveredFile} for the file.
     */
    private CoveredFile noteFile(Path file)
    {
        // TODO canonicalize?
        URI uri = file.toUri();
        return noteFile(uri);
    }

    /**
     * Ensures that we have a {@link CoveredFile} for the source.
     */
    private CoveredFile noteFile(URI uri)
    {
        assert uri != null;
        return myCoveredFiles.computeIfAbsent(uri, CoveredFile::forUri);
    }


    /**
     * Notes (via {@link #noteScript(Path)} all the {@code .fusion} files
     * within the configured script directories.
     */
    private void noteScripts(Set<Path> scriptDirs)
        throws IOException
    {
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path entry, BasicFileAttributes attrs)
            {
                if (entry.getFileName().toString().endsWith(FUSION_SOURCE_EXTENSION))
                {
                    noteScript(entry);
                }
                return FileVisitResult.CONTINUE;
            }
        };

        for (Path dir : scriptDirs)
        {
            walkFileTree(dir, visitor);
        }
    }


    private void noteSourceName(SourceName name)
    {
        ModuleIdentity id = name.getModuleIdentity();
        if (myConfig.moduleIsSelected(id))
        {
            // A module can occur in different sessions with different locations,
            // one with a file Path and the other with a jar URI.  Here we look for
            // such duplicates and normalize to the file Path.

            noteModule(id).noteSourceName(name);
            // We'll come back later to note the preferred source file.
        }
        else
        {
            // TODO Warn about instrumented scripts that are not selected by our config?
            Path path = name.getPath();
            if (path != null)
            {
                noteScript(path);
            }
            // Assuming that Jar-packed scripts are not a thing.
        }
    }


    private void notePreferredSourceOfModule(CoveredModule module)
    {
        // Some modules are "directories" with no source file.
        URI uri = module.getUri();
        if (uri != null)
        {
            // Not filtered by configuration
            noteFile(uri).containsModule(module);
        }
    }


    /**
     * Can be called multiple times for the same (effective) location; if any
     * invocation passes true, then the location is considered covered.
     */
    private void noteLocationCoverage(SourceLocation loc, Boolean covered)
    {
        SourceName name = loc.getSourceName();
        ModuleIdentity id = name.getModuleIdentity();
        if (id != null)
        {
            CoveredModule module = myCoveredModules.get(id);

            // Use a SourceLocation with the module's preferred SourceName.
            loc = module.normalizeLocation(loc);

            module.noteLocationCoverage(loc, covered);
        }

        // WARNING: `loc` may have been normalized above.
        URI uri = loc.getSourceName().getUri();
        CoveredFile file = myCoveredFiles.get(uri);
        assert file != null : "No CoveredFile for " + loc.getSourceName();

        file.noteLocationCoverage(loc, covered);
    }


    private void summarize()
    {
        myCoveredModules.values().forEach(CoveredModule::summarize);
        myCoveredFiles.values().forEach(CoveredFile::summarize);

        // The modules are a subset of the files; don't apply them twice to the globals.
        myCoveredFiles.values().forEach(f -> f.summarizeInto(myGlobalSummary));
    }


    /**
     * Aggregated coverage metrics for the entire report.
     *
     * @return not null.
     */
    CoverageInfoPair globalSummary()
    {
        return myGlobalSummary;
    }
}
