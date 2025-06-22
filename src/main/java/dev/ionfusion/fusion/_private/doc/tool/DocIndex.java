// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.DocTreeNode;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
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


    static DocIndex buildDocIndex(DocTreeNode doc)
    {
        DocIndex index = new DocIndex();
        index.addEntriesForTree(doc);
        return index;
    }


    private void addEntriesForTree(DocTreeNode doc)
    {
        addEntriesForModule(doc.getModuleDocs());

        for (DocTreeNode submodule : doc.submodules())
        {
            addEntriesForTree(submodule);
        }
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
                ids.addAll(bindingDoc.getProvidingModules());
            }
        }
    }
}
