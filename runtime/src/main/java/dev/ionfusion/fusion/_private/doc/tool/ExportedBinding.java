// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static java.util.Comparator.comparing;

import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.runtime.base.ModuleIdentity;
import java.util.Comparator;
import java.util.List;

/**
 * Denotes a name exported from a specific module, holding the docs for its
 * associated binding (that is, unique definition site).
 */
public class ExportedBinding
{
    // TODO A module can export one binding under multiple names, so this
    //  should sort secondarily by name, using the right comparator.
    //  (But that shouldn't be static, since the index determines the sort.)
    public static final Comparator<ExportedBinding> COMPARE_BY_MODULE =
        comparing(ExportedBinding::getModuleId);

    private final ModuleEntity myModule;
    private final String       myName;
    private final BindingDoc   myDocs;

    public ExportedBinding(ModuleEntity module, String name, BindingDoc docs)
    {
        assert module != null;
        assert name != null;

        myModule = module;
        myName = name;
        myDocs = docs;
    }


    public ModuleIdentity getModuleId()
    {
        return myModule.getIdentity();
    }

    public String getName()
    {
        return myName;
    }

    /**
     * Get any documentation associated with this export.
     *
     * @return may be null.
     */
    public BindingDoc getDocs()
    {
        return myDocs;
    }

    public List<ExportedBinding> alsoProvidedBy()
    {
        return myModule.getIndex().otherExportsOf(this);
    }
}
