// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;


public final class DocIndex
{
    private final Predicate<ModuleIdentity>            myModuleSelector;
    private final TreeMap<String, Set<ModuleIdentity>> myNameMap;

    private DocIndex(Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myNameMap = new TreeMap<>();
    }

    public TreeMap<String, Set<ModuleIdentity>> getNameMap()
    {
        return myNameMap;
    }


    //========================================================================


    static DocIndex buildDocIndex(Predicate<ModuleIdentity> selector,
                                  Set<ModuleEntity> modules)
    {
        DocIndex index = new DocIndex(selector);

        modules.stream()
                .map(ModuleEntity::getModuleDocs)
                .forEach(index::addEntriesForModule);

        return index;
    }


    private void addEntriesForModule(ModuleDocs model)
    {
        if (model == null) return;

        ModuleIdentity id = model.getIdentity();

        Map<String, BindingDoc> bindingMap = model.getBindingDocs();
        for (Map.Entry<String, BindingDoc> entry : bindingMap.entrySet())
        {
            String name = entry.getKey();
            Set<ModuleIdentity> ids = myNameMap.computeIfAbsent(name, k -> new TreeSet<>());

            BindingDoc bindingDoc = entry.getValue();
            if (bindingDoc == null)
            {
                ids.add(id);
            }
            else
            {
                // Binding cross-references are collected by the compiler and
                // not pre-filtered.
                bindingDoc.getProvidingModules()
                          .stream()
                          .filter(myModuleSelector)
                          .forEach(ids::add);
            }
        }
    }


    public PermutedIndex permute()
    {
        PermutedIndex permuted = new PermutedIndex();
        myNameMap.forEach(permuted::addEntries);
        return permuted;
    }
}
