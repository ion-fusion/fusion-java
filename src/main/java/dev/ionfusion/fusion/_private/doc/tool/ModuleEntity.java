// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static java.util.Comparator.comparing;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Holds aggregated documentation and cross-references for a module.
 */
public class ModuleEntity
{
    private final DocIndex                   myIndex;
    private final ModuleIdentity             myModuleIdentity;
    private       ModuleDocs                 myModuleDocs;
    private final Map<String, ModuleEntity>  myChildren;
    private final SortedSet<ExportedBinding> myExports;


    ModuleEntity(DocIndex index, ModuleIdentity id)
    {
        myIndex = index;
        myModuleIdentity = id;
        myChildren = new HashMap<>();
        myExports = new TreeSet<>(comparing(ExportedBinding::getName,
                                            index.getBoundNameComparator()));
    }


    public DocIndex getIndex()
    {
        return myIndex;
    }

    public ModuleIdentity getIdentity()
    {
        return myModuleIdentity;
    }


    void setModuleDocs(ModuleDocs docs)
    {
        assert myModuleDocs == null;
        assert docs.getIdentity() == myModuleIdentity;
        myModuleDocs = docs;

        docs.getBindingDocs().forEach(this::addExport);
    }

    /**
     * @return not null.
     */
    public ModuleDocs getModuleDocs()
    {
        return (myModuleDocs != null ? myModuleDocs : new ModuleDocs(myModuleIdentity));
    }


    //=========================================================================
    // Submodules

    void addChild(ModuleEntity child)
    {
        myChildren.put(child.getIdentity().baseName(), child);
    }

    public ModuleEntity getChild(String name)
    {
        return myChildren.get(name);
    }

    /**
     * Not sorted.
     */
    public Set<String> getChildNames()
    {
        return myChildren.keySet();
    }


    //=========================================================================
    // Exported bindings

    private void addExport(String name, BindingDoc export)
    {
        myExports.add(new ExportedBinding(this, name, export));
    }

    /**
     * Gets the docs for exported bindings, sorted by name.
     *
     * @return not null
     */
    public Collection<ExportedBinding> exports()
    {
        return myExports;
    }
}
