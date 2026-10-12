// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

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


    ModuleStore(ModuleRegistry registry,
                ModuleStore[]  requiredModules,
                BaseSymbol[]   definedNames)
    {
        int variableCount = definedNames.length;

        myRegistry = registry;
        myRequiredModules = requiredModules;
        myValues = new Object[variableCount];
        myDefinedNames = definedNames;
    }

    ModuleStore(ModuleRegistry registry,
                Object[]       values,
                BaseSymbol[]   definedNames)
    {
        assert values.length == definedNames.length;

        myRegistry = registry;
        myRequiredModules = new ModuleStore[0];
        myValues = values;
        myDefinedNames = definedNames;
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
    public Object lookupImport(int moduleAddress, int bindingAddress)
    {
        return myRequiredModules[moduleAddress].myValues[bindingAddress];
    }

    @Override
    public BaseSymbol getDefinedName(int address)
    {
        return myDefinedNames[address];
    }
}
