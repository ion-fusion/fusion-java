// Copyright (c) 2012-2017 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.GlobalState.DEFINE;
import static com.amazon.fusion.GlobalState.REQUIRE;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.util.Iterator;
import java.util.Set;


/**
 * Extended prepare-time {@link Namespace} that knows it's a module.
 * This exists to create special bindings that can refer module variables that
 * are not exported (but that are accessible through macro-generated code).
 */
final class ModuleNamespace
    extends Namespace
{
    static abstract class ProvidedBinding
        extends Binding
    {
        private final BaseSymbol     myName;
        private final SourceLocation myLoc;
        private BindingInformation   myInfo;

        ProvidedBinding(BaseSymbol name, SourceLocation sourceLocation)
        {
            myName = name;
            myLoc  = sourceLocation;
        }

        @Override
        final BaseSymbol getName() { return myName; }

        @Override
        BindingInformation getBindingInformation()
        {
            if (myInfo == null)
            {
                myInfo = BindingInformation.makeBindingInfo(
                             myLoc,
                             target().getBindingInformation());
            }
            return myInfo;
        }

        @Override
        final ProvidedBinding provideAs(BaseSymbol name,
                                        SourceLocation sourceLocation)
        {
            return new ImportedProvidedBinding(name, sourceLocation, this);
        }

        @Override
        abstract ModuleDefinedBinding target();
        abstract ModuleIdentity getTargetModule();

        @Override
        final Object lookup(Namespace ns)
        {
            return target().lookup(ns);
        }
    }

    /**
     * A provided binding that was defined in the providing module.
     */
    static final class DefinedProvidedBinding
        extends ProvidedBinding
    {
        private final ModuleDefinedBinding myDefinition;

        DefinedProvidedBinding(BaseSymbol name,
                               SourceLocation sourceLocation,
                               ModuleDefinedBinding binding)
        {
            super(name, sourceLocation);

            assert binding.target() == binding;
            myDefinition = binding;
        }

        DefinedProvidedBinding(ModuleDefinedBinding binding)
        {
            this(binding.getName(),
                 binding.getBindingInformation().getSourceLocation(),
                 binding);
        }

        @Override
        ModuleDefinedBinding target()
        {
            return myDefinition;
        }

        @Override
        ModuleIdentity getTargetModule()
        {
            return myDefinition.myModuleId;
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{DefinedProvidedBinding " + getName()
                + " -> "  + myDefinition + "}}}";
        }
    }

    /**
     * A provided binding that was imported into the providing module.
     */
    static final class ImportedProvidedBinding
        extends ProvidedBinding
    {
        private final ProvidedBinding myImport;

        ImportedProvidedBinding(BaseSymbol name,
                                SourceLocation sourceLocation,
                                ProvidedBinding imported)
        {
            super(name, sourceLocation);
            myImport = imported;
        }

        @Override
        ModuleDefinedBinding target()
        {
            return myImport.target();
        }

        @Override
        ModuleIdentity getTargetModule()
        {
            return myImport.getTargetModule();
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{ImportedProvidedBinding " + getName()
                + " -> "  + myImport + "}}}";
        }
    }


    /**
     * Denotes a module-level binding imported into a module via
     * {@code require}.
     *
     * @see LanguageBinding
     */
    static class ModuleRequiredBinding
        extends RequiredBinding
    {
        private ModuleRequiredBinding(SyntaxSymbol identifier,
                                      ProvidedBinding target)
        {
            super(identifier, target);
        }

        @Override
        NsDefinedBinding redefine(SyntaxSymbol identifier,
                                  SyntaxValue formForErrors)
            throws AmbiguousBindingFailure
        {
            String name = identifier.stringValue();
            throw new AmbiguousBindingFailure(DEFINE, name, formForErrors);
        }

        @Override
        RequiredBinding require(SyntaxSymbol localId,
                                ProvidedBinding provided,
                                SyntaxValue formForErrors)
            throws AmbiguousBindingFailure
        {
            if (this.sameTarget(provided)) return this;

            String name = localId.stringValue();
            throw new AmbiguousBindingFailure(REQUIRE, name, formForErrors);
        }

        @Override
        NsDefinedBinding definition()
        {
            return null;
        }

        @Override
        ProvidedBinding provideAs(BaseSymbol name,
                                  SourceLocation sourceLocation)
        {
            return new ImportedProvidedBinding(name, sourceLocation, myTarget);
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{ModuleRequiredBinding " + getDebugName()
                 + " -> " + target().myModuleId.absolutePath()
                 + '#' + getName() + "}}}";
        }
    }


    /**
     * Denotes a binding imported into a module via the language declared at
     * its creation. This is a special kind of required-binding that can be
     * overwritten by a require or a module-level definition.
     */
    static final class LanguageBinding
        extends ModuleRequiredBinding
    {
        private LanguageBinding(SyntaxSymbol identifier,
                                ProvidedBinding target)
        {
            super(identifier, target);
        }

        @Override
        NsDefinedBinding redefine(SyntaxSymbol identifier,
                                  SyntaxValue formForErrors)
        {
            // We can redefine an id that came from our language.
            return null;
        }

        @Override
        RequiredBinding require(SyntaxSymbol localId,
                                ProvidedBinding provided,
                                SyntaxValue formForErrors)
        {
            // Replace the language binding with the required one. We must do
            // this even if it leads to the same target, since a language
            // binding can be redefined, but a required binding cannot.
            return new ModuleRequiredBinding(localId, provided);
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{LanguageBinding " + getDebugName()
                 + " -> " + target().myModuleId.absolutePath()
                 + '#' + getName() + "}}}";
        }
    }


    /**
     * Denotes a binding defined (not imported) at module-level.
     * Instances are one-to-one with each {@code define} at module-level.
     * <p>
     * Unlike top-level bindings, module-level bindings are immutable.
     * <p>
     * When imported into another namespace, a {@code ModuleDefinedBinding} is
     * wrapped by either a {@link LanguageBinding} or a
     * {@link Namespace.RequiredBinding}.
     */
    final class ModuleDefinedBinding
        extends NsDefinedBinding
    {
        final ModuleIdentity myModuleId;

        private ModuleDefinedBinding(SyntaxSymbol identifier, int address,
                                     ModuleIdentity moduleId)
        {
            super(identifier, address);
            myModuleId = moduleId;
        }

        @Override
        NsDefinedBinding redefine(SyntaxSymbol identifier,
                                  SyntaxValue formForErrors)
            throws AmbiguousBindingFailure
        {
            // A definition already exists, we can't redefine.
            String name = identifier.stringValue();
            throw new AmbiguousBindingFailure(null, name, formForErrors);
        }

        @Override
        RequiredBinding require(SyntaxSymbol localId,
                                ProvidedBinding provided,
                                SyntaxValue formForErrors)
            throws AmbiguousBindingFailure
        {
            // A definition already exists, we can't require the same id.
            String name = localId.stringValue();
            throw new AmbiguousBindingFailure(null, name, formForErrors);
        }

        @Override
        ProvidedBinding provideAs(BaseSymbol name, SourceLocation sourceLocation)
        {
            return new DefinedProvidedBinding(name, sourceLocation, this);
        }

        @Override
        Object lookup(Namespace localNamespace)
        {
            if (localNamespace.getModuleId() != myModuleId)
            {
                // The local context is a different module, so we must ignore
                // it and go directly to our own namespace.

                ModuleInstance module =
                    localNamespace.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;

                ModuleStore store = module.getStore();
                return store.lookup(myAddress);
            }

            // We can't use our address directly, since we may be compiling
            // and the binding may not have a location allocated yet.
            return localNamespace.lookupDefinition(this);
        }

        Object lookup(ModuleInstance module)
        {
            ModuleStore store = module.getStore();

            if (module.getIdentity() != myModuleId)
            {
                module = store.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;
                store = module.getStore();
            }

            return store.lookup(myAddress);
        }

        @Override
        String mutationSyntaxErrorMessage()
        {
             return "mutation of module-level variables is not yet supported";
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{ModuleDefinedBinding " + myModuleId.absolutePath()
                + ' ' + getDebugName() + "}}}";
        }
    }


    /**
     * Exposes the bindings visible at module-level.
     */
    private static final class ModuleWrap
        extends NamespaceWrap
    {
        ModuleWrap(ModuleNamespace ns)
        {
            super(ns);
        }

        @Override
        Binding resolve(BaseSymbol name,
                        Iterator<SyntaxWrap> moreWraps,
                        Set<MarkWrap> returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                // Prior bindings never "leak through" a module, so we won't
                // return this binding.
                Binding b = nextWrap.resolve(name, moreWraps, returnMarks);
                if (b != null)
                {
                    return ((Namespace)getEnvironment()).resolve(b, returnMarks);
                }
            }

            return getEnvironment().substituteFree(name, returnMarks);
        }
    }


    /**
     * Constructs a module with a given language.  Bindings provided by the
     * language can be shadowed by {@code require} or {@code define}.
     *
     * @param moduleId identifies this module.
     */
    ModuleNamespace(Evaluator eval,
                    ModuleRegistry registry,
                    SyntaxSymbol lexicalContext,
                    final ModuleInstance language,
                    ModuleIdentity moduleId)
        throws FusionException
    {
        super(registry, moduleId,
              new Function<Namespace, SyntaxWraps>()
              {
                  @Override
                  public SyntaxWraps apply(Namespace _this) {
                      ModuleNamespace __this = (ModuleNamespace) _this;
                      return SyntaxWraps.make(new ModuleWrap(__this));
                  }
              });

        for (ProvidedBinding provided : language.providedBindings())
        {
            BaseSymbol name = provided.getName();
            SyntaxSymbol id =
                (SyntaxSymbol) name.datumToSyntaxMaybe(eval, lexicalContext, null);
            id.resolve(); // This needs to resolve "outside" this module.

            NsBinding prior = installBinding(id, new LanguageBinding(id, provided));
            assert prior == null;
        }
    }

    /**
     * Constructs a module that uses no other module. Any bindings will need to
     * be created via {@link #bind(String, Object)}.
     *
     * @param moduleId identifies this module.
     */
    ModuleNamespace(ModuleRegistry registry, ModuleIdentity moduleId)
    {
        super(registry, moduleId,
              new Function<Namespace, SyntaxWraps>()
              {
                  @Override
                  public SyntaxWraps apply(Namespace _this) {
                      return SyntaxWraps.make(new EnvironmentWrap(_this));
                  }
              });
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    NsDefinedBinding newDefinedBinding(SyntaxSymbol identifier, int address)
    {
        return new ModuleDefinedBinding(identifier, address, getModuleId());
    }

    @Override
    public void setDoc(int address, BindingDoc doc)
    {
        doc.addProvidingModule(getModuleId());

        super.setDoc(address, doc);
    }



    @Override
    RequiredBinding newRequiredBinding(SyntaxSymbol    localId,
                                       ProvidedBinding target)
    {
        return new ModuleRequiredBinding(localId, target);
    }


    //========================================================================


    /**
     * A reference to a module-level binding in a namespace that is not the one
     * in our lexical context.
     */
    static final class CompiledImportedVariableReference
        implements CompiledForm
    {
        final int myModuleAddress;
        final int myBindingAddress;

        CompiledImportedVariableReference(int moduleAddress,
                                          int bindingAddress)
        {
            myModuleAddress  = moduleAddress;
            myBindingAddress = bindingAddress;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            NamespaceStore ns = store.namespace();
            Object result = ns.lookupImport(myModuleAddress, myBindingAddress);
            assert result != null
                : "No value for " + myModuleAddress + "@" + myBindingAddress;
            return result;
        }
    }

}
