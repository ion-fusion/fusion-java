// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonString;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonValue;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.BindingDoc.Kind;
import java.util.Collection;


final class ModuleBuilderImpl
    implements ModuleBuilder
{
    private final ModuleNameResolver myResolver;
    private final ModuleRegistry     myRegistry;
    private final ModuleNamespace    myNamespace;
    private final IonStruct          myDocs;

    /**
     * Prepares to build a module with no language.
     */
    ModuleBuilderImpl(ModuleNameResolver resolver,
                      ModuleRegistry registry,
                      ModuleIdentity moduleId)
    {
        this(resolver, registry, moduleId, null);
    }

    /**
     * @param docs will be modified by the builder.
     */
    ModuleBuilderImpl(ModuleNameResolver resolver,
                      ModuleRegistry registry,
                      ModuleIdentity moduleId,
                      IonStruct docs)
    {
        myResolver = resolver;
        myRegistry = registry;
        myNamespace = new ModuleNamespace(registry, moduleId);
        myDocs = docs;
    }

    @Override
    public void define(String name, Object value)
        throws FusionException
    {
        myNamespace.bind(name, value);

        String docs = docsFor(name);
        if (docs != null)
        {
            myNamespace.setDoc(name, kindOf(value), docs);
        }
        else if (value instanceof Procedure)
        {
            BindingDoc doc = ((Procedure) value).document();
            if (doc != null)
            {
                assert false;
                myNamespace.setDoc(name, doc);
            }
        }
    }

    @Override
    public void instantiate()
        throws FusionException
    {
        if (myDocs != null && ! myDocs.isEmpty())
        {
            String message =
                "Unused documentation: " + myDocs.toPrettyString();
            throw new IllegalStateException(message);
        }

        ModuleStore store = new ModuleStore(myRegistry,
                                            myNamespace.extractValues(),
                                            myNamespace.extractDefinedNames(),
                                            myNamespace.extractBindingDocs());
        Collection<NsDefinedBinding> bindings = myNamespace.getDefinedBindings();

        ModuleIdentity id = myNamespace.getModuleId();

        ModuleInstance module = new ModuleInstance(id, store, bindings);

        myRegistry.register(myResolver, module, null);
    }


    private Kind kindOf(Object value)
    {
        if (value instanceof Procedure)
        {
            return Kind.PROCEDURE;
        }
        else if (value instanceof SyntacticForm)
        {
            return Kind.SYNTAX;
        }
        else
        {
            return Kind.CONSTANT;
        }
    }

    private String docsFor(String name)
    {
        if (myDocs != null)
        {
            IonValue maybeDocs = myDocs.remove(name);
            if (maybeDocs != null)
            {
                return ((IonString) maybeDocs).stringValue();
            }
        }
        return null;
    }
}
