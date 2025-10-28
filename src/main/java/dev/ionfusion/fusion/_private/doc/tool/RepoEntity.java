// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._Private_Trampoline.discoverModulesInRepository;
import static dev.ionfusion.fusion._Private_Trampoline.instantiateModuleDocs;
import static dev.ionfusion.fusion._Private_Trampoline.loadModule;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion.TopLevel;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Holds aggregated documentation and cross-reference for a repository.
 */
public class RepoEntity
{
    private final DocIndex                          myIndex;
    private final Path                              myRepoDir;
    private final Predicate<ModuleIdentity>         mySelector;
    private final TopLevel                          myTopLevel;
    private       Map<ModuleIdentity, ModuleEntity> myModules;


    public RepoEntity(DocIndex index, Path repoDir, Predicate<ModuleIdentity> selector, TopLevel top)
    {
        myIndex = index;
        myRepoDir = repoDir;
        mySelector = selector;
        myTopLevel = top;
    }

    private void discoverModules()
        throws FusionException
    {
        // Discover everything first; the Consumer won't propagate exceptions.
        Set<ModuleIdentity> moduleIds = new HashSet<>();
        discoverModulesInRepository(myRepoDir, mySelector, moduleIds::add);

        for (ModuleIdentity id : moduleIds)
        {
            // TODO Handle exceptions.
            ModuleIdentity loadedId = loadModule(myTopLevel, id.absolutePath());
            assert id.equals(loadedId);

            ModuleDocs docs = instantiateModuleDocs(myTopLevel, id);
            assert docs != null;

            addModuleDocs(docs);
        }
    }

    private ModuleEntity ensureEntityForModule(ModuleIdentity id)
    {
        if (id == null) return null;

        ModuleEntity module = myModules.get(id);
        if (module != null) return module;

        ModuleEntity parent = ensureEntityForModule(id.parent());

        module = new ModuleEntity(myIndex, id);
        if (parent != null) parent.addSubmodule(module);
        myModules.put(id, module);

        return module;
    }

    private void addModuleDocs(ModuleDocs module)
    {
        ModuleIdentity id     = module.getIdentity();
        ModuleEntity   entity = ensureEntityForModule(id);
        entity.setModuleDocs(module);
    }


    /**
     * Returns the modules selected from this repository.
     *
     * @return not null.
     *
     * @throws FusionException if there's a problem during discovery.
     */
    public Set<ModuleEntity> getSelectedModules()
        throws FusionException
    {
        if (myModules == null)
        {
            myModules = new HashMap<>();
            discoverModules();
        }

        HashSet<ModuleEntity> set = new HashSet<>(myModules.values());
        assert set.size() == myModules.size();
        return set;
    }
}
