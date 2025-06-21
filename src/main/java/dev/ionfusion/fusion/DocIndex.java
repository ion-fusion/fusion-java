// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


final class DocIndex
{
    private final TreeMap<String, Set<ModuleIdentity>> myNameMap;

    private DocIndex()
    {
        myNameMap = new TreeMap<>();
    }

    TreeMap<String, Set<ModuleIdentity>> getNameMap()
    {
        return myNameMap;
    }


    //========================================================================


    static DocIndex buildDocIndex(ModuleDoc doc)
    {
        DocIndex index = new DocIndex();
        index.addEntriesForTree(doc);
        return index;
    }


    private void addEntriesForTree(ModuleDoc doc)
    {
        // Skip the (anonymous) repo root and "modules" that are directories
        if (doc.myModuleId != null)
        {
            addEntriesForModule(doc);
        }

        Collection<ModuleDoc> submodules = doc.submodules();
        for (ModuleDoc submodule : submodules)
        {
            addEntriesForTree(submodule);
        }
    }


    private void addEntriesForModule(ModuleDoc doc)
    {
        Map<String, BindingDoc> bindingMap = doc.bindingMap();
        if (bindingMap != null)
        {
            for (Map.Entry<String, BindingDoc> entry : bindingMap.entrySet())
            {
                String name = entry.getKey();
                Set<ModuleIdentity> ids = myNameMap.get(name);
                if (ids == null)
                {
                    ids = new TreeSet<>();
                    myNameMap.put(name, ids);
                }

                BindingDoc bindingDoc = entry.getValue();
                if (bindingDoc == null)
                {
                    ids.add(doc.myModuleId);
                }
                else
                {
                    ids.addAll(bindingDoc.getProvidingModules());
                }
            }
        }
    }
}
