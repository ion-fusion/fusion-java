// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;

/**
 * Eval-time storage for modules.
 */
final class ModuleStore
    implements NamespaceStore
{
    private final ModuleRegistry myRegistry;
    private final ModuleStore[]  myRequiredModules;
    private final Object[]       myValues;
    private final BaseSymbol[]   myDefinedNames;
    private final BindingDoc[]   myBindingDocs;


    ModuleStore(ModuleRegistry registry,
                ModuleStore[]  requiredModules,
                BaseSymbol[]   definedNames,
                BindingDoc[]   bindingDocs)
    {
        int variableCount = definedNames.length;

        myRegistry = registry;
        myRequiredModules = requiredModules;
        myValues = new Object[variableCount];
        myDefinedNames = definedNames;
        myBindingDocs = bindingDocs;
    }

    ModuleStore(ModuleRegistry registry,
                Object[]       values,
                BaseSymbol[]   definedNames,
                BindingDoc[]   bindingDocs)
    {
        assert values.length == definedNames.length;

        myRegistry = registry;
        myRequiredModules = new ModuleStore[0];
        myValues = values;
        myDefinedNames = definedNames;
        myBindingDocs  = bindingDocs;
    }

    @Override
    public Object lookup(int rib, int address)
    {
        throw new IllegalStateException("Rib not found");
    }

    @Override
    public void set(int rib, int address, Object value)
    {
        throw new IllegalStateException("Rib not found");
    }


    @Override
    public ModuleRegistry getRegistry()
    {
        return myRegistry;
    }

    @Override
    public NamespaceStore namespace()
    {
        return this;
    }

    @Override
    public void set(int address, Object value)
    {
        myValues[address] = value;
    }

    @Override
    public Object lookup(int address)
    {
        return myValues[address];
    }

    @Override
    public ModuleStore lookupRequiredModule(int moduleAddress)
    {
        return myRequiredModules[moduleAddress];
    }

    @Override
    public Object lookupImport(int moduleAddress, int bindingAddress)
    {
        return myRequiredModules[moduleAddress].myValues[bindingAddress];
    }

    @Override
    public BaseSymbol getDefinedName(int address)
    {
        return myDefinedNames[address];
    }

    BindingDoc document(int address)
    {
        if (address < myBindingDocs.length)
        {
            BindingDoc doc = myBindingDocs[address];
            if (doc != null && doc.getKind() == null)
            {
                Object value = lookup(address);
                if (value instanceof Procedure)
                {
                    doc.setKind(BindingDoc.Kind.PROCEDURE);
                }
                else if (value instanceof SyntacticForm)
                {
                    doc.setKind(BindingDoc.Kind.SYNTAX);
                }
            }
            return doc;
        }
        return null;
    }
}
