// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds aggregated documentation and cross-references for a module.
 */
public class ModuleEntity
{
    private final DocIndex                  myIndex;
    private final ModuleIdentity            myModuleIdentity;
    private       ModuleDocs                myModuleDocs;
    private final Map<String, ModuleEntity> myChildren;

    ModuleEntity(DocIndex index, ModuleIdentity id)
    {
        myIndex = index;
        myModuleIdentity = id;
        myChildren = new HashMap<>();
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
    }

    /**
     * @return not null.
     */
    public ModuleDocs getModuleDocs()
    {
        return (myModuleDocs != null ? myModuleDocs : new ModuleDocs(myModuleIdentity));
    }


    void addChild(ModuleEntity child)
    {
        myChildren.put(child.getIdentity().baseName(), child);
    }

    public ModuleEntity getChild(String name)
    {
        return myChildren.get(name);
    }

    public Set<String> getChildNames()
    {
        return myChildren.keySet();
    }
}
