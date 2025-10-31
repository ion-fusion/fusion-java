// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;


public final class DocIndex
{
    private final Predicate<ModuleIdentity>            myModuleSelector;
    private final TreeMap<String, Set<ModuleIdentity>> myNameMap;

    DocIndex(Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myNameMap = new TreeMap<>();
    }

    public TreeMap<String, Set<ModuleIdentity>> getNameMap()
    {
        return myNameMap;
    }


    //========================================================================


    void addModule(ModuleEntity module)
    {
        addEntriesForModule(module.getModuleDocs());
    }

    private void addEntriesForModule(ModuleDocs model)
    {
        if (model == null) return;

        ModuleIdentity id = model.getIdentity();
        assert myModuleSelector.test(id);

        model.getProvidedNames().forEach(name -> addExport(name, id));
    }


    private void addExport(String name, ModuleIdentity exportingModule)
    {
        Set<ModuleIdentity> ids = myNameMap.computeIfAbsent(name, k -> new TreeSet<>());
        ids.add(exportingModule);
    }


    public Stream<ModuleIdentity> exportsOf(String exportName)
    {
        assert myNameMap.containsKey(exportName);
        return myNameMap.get(exportName).stream();
    }


    public PermutedIndex permute()
    {
        PermutedIndex permuted = new PermutedIndex();
        myNameMap.forEach(permuted::addEntries);
        return permuted;
    }
}
