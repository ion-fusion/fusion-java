// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import dev.ionfusion.fusion.ModuleIdentity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds aggregated documentation and cross-references for a module.
 */
public class ModuleEntity
{
    private final ModuleIdentity            myModuleIdentity;
    private       ModuleDocs                myModuleDocs;
    private final Map<String, ModuleEntity> myChildren;

    ModuleEntity(ModuleIdentity id)
    {
        myModuleIdentity = id;
        myChildren = new HashMap<>();
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
