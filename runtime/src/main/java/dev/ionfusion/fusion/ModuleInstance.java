// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.DefinedProvidedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ModuleDefinedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import dev.ionfusion.runtime._private.doc.BindingDoc;
import dev.ionfusion.runtime._private.doc.ModuleDocs;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.embed.ModuleBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A module that's been instantiated for use by one or more other modules.
 * A module has a unique {@link ModuleIdentity} and a {@link ModuleStore}
 * holding its top-level bindings.
 */
final class ModuleInstance
{
    private final Namespace      myNamespace;
    private final String         myDocs;
    private final ModuleStore    myStore;

    /**
     * Not all of these bindings are for this module; names that are imported
     * and exported have their bindings passed through.
     */
    private final Map<BaseSymbol,ProvidedBinding> myProvidedBindings;


    private ModuleInstance(Namespace      namespace,
                           String         docs,
                           ModuleStore    store,
                           int            bindingCount)
    {
        myNamespace = namespace;
        myDocs = docs;
        myStore = store;

        // Use object identity since symbols are interned.
        myProvidedBindings = new IdentityHashMap<>(bindingCount);
    }

    /**
     * Creates a module that {@code provide}s the given bindings.
     * This is for use by the {@link ModuleBuilder}.
     */
    ModuleInstance(Namespace namespace, ModuleStore store,
                   Collection<NsDefinedBinding> bindings)
        throws FusionException
    {
        this(namespace, /* docs */ null, store, bindings.size());

        for (NsDefinedBinding binding : bindings)
        {
            BaseSymbol name = binding.getName();
            ProvidedBinding out =
                new DefinedProvidedBinding((ModuleDefinedBinding) binding);
            myProvidedBindings.put(name, out);
        }
    }

    /**
     * Creates a module that {@code provide}s the given bindings.
     */
    ModuleInstance(Namespace         namespace,
                   String            docs,
                   ModuleStore       store,
                   ProvidedBinding[] providedBindings)
        throws FusionException
    {
        this(namespace, docs, store, providedBindings.length);

        for (ProvidedBinding binding : providedBindings)
        {
            myProvidedBindings.put(binding.getName(), binding);
        }
    }


    Namespace getNamespace()
    {
        return myNamespace;
    }

    ModuleIdentity getIdentity()
    {
        return myNamespace.getModuleId();
    }


    ModuleStore getStore()
    {
        return myStore;
    }

    //========================================================================

    Collection<ProvidedBinding> providedBindings()
    {
        return Collections.unmodifiableCollection(myProvidedBindings.values());
    }

    Set<BaseSymbol> providedNames()
    {
        return Collections.unmodifiableSet(myProvidedBindings.keySet());
    }

    /**
     * @return null if the name isn't provided by this module.
     */
    ProvidedBinding resolveProvidedName(BaseSymbol name)
    {
        return myProvidedBindings.get(name);
    }


    //========================================================================
    // Documentation metadata

    ModuleDocs getDocs()
    {
        // TODO This map should be constructed downstream.
        Set<BaseSymbol> names = providedNames();
        Map<String, BindingDoc> bindings =
            (names.isEmpty()
                ? Collections.emptyMap()
                : new HashMap<>(names.size()));

        for (BaseSymbol name : names)
        {
            bindings.put(name.stringValue(), documentProvidedName(name));
        }

        return new ModuleDocs(getIdentity(), myDocs, bindings);
    }

    /**
     * @return can be null.
     */
    private BindingDoc documentProvidedName(BaseSymbol name)
    {
        ModuleDefinedBinding binding = resolveProvidedName(name).target();
        if (binding.myModuleId == getIdentity())
        {
            return documentBinding(binding.myAddress);
        }

        ModuleInstance module = myStore.getRegistry().lookup(binding.myModuleId);
        assert module != null : "Module not found: " + binding.myModuleId;

        return module.documentBinding(binding.myAddress);
    }

    BindingDoc documentBinding(int address)
    {
        BindingDoc doc = myNamespace.document(address);
        if (doc != null && doc.getKind() == null)
        {
            Object value = myStore.lookup(address);
            if (value instanceof Procedure)
            {
                doc.setKind(BindingDoc.Kind.PROCEDURE);
            }
            else if (value instanceof SyntacticForm)
            {
                doc.setKind(BindingDoc.Kind.SYNTAX);
            }
            else
            {
                doc.setKind(BindingDoc.Kind.CONSTANT);
            }
        }
        return doc;
    }
}
