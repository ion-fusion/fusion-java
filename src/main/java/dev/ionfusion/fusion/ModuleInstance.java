// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.DefinedProvidedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ModuleDefinedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.io.IOException;
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
    extends NamedValue
{
    // TODO This should retain the Namespace, perhaps, so we can evaluate
    //  "inside" the module namespace

    private final ModuleIdentity myIdentity;
    private final String         myDocs;
    private final ModuleStore    myStore;

    /**
     * Not all of these bindings are for this module; names that are imported
     * and exported have their bindings passed through.
     */
    private final Map<BaseSymbol,ProvidedBinding> myProvidedBindings;


    private ModuleInstance(ModuleIdentity identity,
                           String         docs,
                           ModuleStore    store,
                           int            bindingCount)
        throws FusionException
    {
        myIdentity = identity;
        myDocs     = docs;
        myStore = store;

        // Use object identity since symbols are interned.
        myProvidedBindings = new IdentityHashMap<>(bindingCount);

        inferName(identity.toString());
    }

    /**
     * Creates a module that {@code provide}s the given bindings.
     * This is for use by the {@link ModuleBuilder}.
     */
    ModuleInstance(ModuleIdentity identity, ModuleStore namespace,
                   Collection<NsDefinedBinding> bindings)
        throws FusionException
    {
        this(identity, /* docs */ null, namespace, bindings.size());

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
    ModuleInstance(ModuleIdentity    identity,
                   String            docs,
                   ModuleStore       namespace,
                   ProvidedBinding[] providedBindings)
        throws FusionException
    {
        this(identity, docs, namespace, providedBindings.length);

        for (ProvidedBinding binding : providedBindings)
        {
            myProvidedBindings.put(binding.getName(), binding);
        }
    }


    ModuleIdentity getIdentity()
    {
        return myIdentity;
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
    ProvidedBinding resolveProvidedName(String name)
    {
        return resolveProvidedName(BaseSymbol.internSymbol(name));
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
        Set<BaseSymbol> names = providedNames();
        Map<String, BindingDoc> bindings =
            (names.isEmpty()
                ? Collections.emptyMap()
                : new HashMap<>(names.size()));

        for (BaseSymbol name : names)
        {
            String text = name.stringValue();
            BindingDoc doc = documentProvidedName(text);
            bindings.put(text, doc);
        }

        return new ModuleDocs(myIdentity, myDocs, bindings);
    }

    /**
     * @return may be null.
     */
    private BindingDoc documentProvidedName(String name)
    {
        ModuleDefinedBinding binding = resolveProvidedName(name).target();

        return documentProvidedName(binding);
    }

    private BindingDoc documentProvidedName(ModuleDefinedBinding binding)
    {
        BindingDoc doc;

        if (binding.myModuleId == myIdentity)
        {
            doc = myStore.document(binding.myAddress);
        }
        else
        {
            ModuleInstance module =
                myStore.getRegistry().lookup(binding.myModuleId);
            assert module != null
                : "Module not found: " + binding.myModuleId;
            doc = module.myStore.document(binding.myAddress);
        }

        if (doc != null)
        {
            doc.addProvidingModule(myIdentity);
        }

        return doc;
    }

    //========================================================================


    @Override
    final void identify(Appendable out)
        throws IOException
    {
        out.append("module ");
        out.append(myIdentity.absolutePath());
    }
}
