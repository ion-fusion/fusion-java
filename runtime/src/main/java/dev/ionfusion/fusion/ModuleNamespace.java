// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.BindingSite.makeExportBindingSite;
import static dev.ionfusion.fusion.GlobalState.REQUIRE;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Extended prepare-time {@link Namespace} that knows it's a module.
 * This exists to create special bindings that can refer to module variables that
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
        private BindingSite          mySite;

        ProvidedBinding(BaseSymbol name, SourceLocation sourceLocation)
        {
            myName = name;
            myLoc  = sourceLocation;
        }

        @Override
        final BaseSymbol getName() { return myName; }

        @Override
        BindingSite getBindingSite()
        {
            if (mySite == null)
            {
                mySite = makeExportBindingSite(myLoc, inside().getBindingSite());
            }
            return mySite;
        }

        @Override
        final ProvidedBinding provideAs(BaseSymbol name,
                                        SourceLocation sourceLocation)
        {
            return new ImportedProvidedBinding(name, sourceLocation, this);
        }

        /**
         * Gets the binding that's being exported, either a definition or
         * import.
         */
        abstract Binding inside();

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
                 binding.getBindingSite().getSourceLocation(),
                 binding);
        }

        @Override
        Binding inside()
        {
            return myDefinition;
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
        Binding inside()
        {
            return myImport;
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
     * {@code require} or a language declaration.
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
            throw new AmbiguousBindingFailure("module-level definition",
                                              name, formForErrors);
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
            return new ImportedProvidedBinding(name, sourceLocation, getProvided());
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
        ProvidedBinding provideAs(BaseSymbol name, SourceLocation idLocation)
        {
            return new DefinedProvidedBinding(name, idLocation, this);
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
        Binding resolveMaybe(BaseSymbol name,
                             Iterator<SyntaxWrap> moreWraps,
                             Set<MarkWrap> returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                // Prior bindings never "leak through" a module, so we won't
                // return this binding.
                Binding b = nextWrap.resolveMaybe(name, moreWraps, returnMarks);
                if (b != null)
                {
                    return ((Namespace)getEnvironment()).resolveMaybe(b, returnMarks);
                }
            }

            return getEnvironment().substituteFree(name, returnMarks);
        }
    }


    /**
     * Helper to work around inability of constructors to invoke virtual methods.
     */
    private static final Function<Namespace, SyntaxWraps> MAKE_SYNTAX_WRAPS =
        new Function<Namespace, SyntaxWraps>() {
            @Override
            public SyntaxWraps apply(Namespace ns)
            {
                return SyntaxWraps.make(new ModuleWrap((ModuleNamespace) ns));
            }
        };


    private final List<BaseSymbol> myDefinedNames = new ArrayList<>();

    /**
     * Expansion-time namespaces are bootstrapped with bindings from a language.
     * Language bindings can be shadowed by {@code require} or {@code define}.
     * <p>
     * When expansion of a {@code (module ...)} form is complete, the populated
     * namespace is discarded. A compile-time namespace is later created to
     * replace it.
     * </p>
     * @param moduleId identifies this module.
     */
    ModuleNamespace(Evaluator eval,
                    ModuleRegistry registry,
                    SyntaxSymbol lexicalContext,
                    ModuleIdentity moduleId,
                    ModuleIdentity languageId)
        throws FusionException
    {
        super(registry, moduleId, MAKE_SYNTAX_WRAPS);

        ModuleInstance language = instantiateRequiredModule(eval, languageId);
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
     * Compile-time namespaces don't track bindings.
     */
    ModuleNamespace(Evaluator eval,
                    ModuleRegistry registry,
                    ModuleIdentity moduleId,
                    ModuleIdentity[] requiredModules)
        throws FusionException
    {
        super(registry, moduleId, MAKE_SYNTAX_WRAPS);

        for (ModuleIdentity m : requiredModules)
        {
            instantiateRequiredModule(eval, m);
        }
        assert Arrays.equals(requiredModuleIds(), requiredModules);
    }


    /**
     * Constructs a module that uses no other module.
     *
     * @param moduleId identifies this module.
     */
    ModuleNamespace(ModuleRegistry registry, ModuleIdentity moduleId)
    {
        super(registry, moduleId, MAKE_SYNTAX_WRAPS);
    }



    @Override
    ModuleIdentity getResolutionBase()
    {
        // TODO https://github.com/ion-fusion/fusion-java/issues/166
        //  Starting a relative path at the parent module will result in
        //  future inconsistency referencing nested modules, compared to
        //  referencing modules declared at top-level.

        return getModuleId().parent();
    }

    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    NsDefinedBinding newDefinedBinding(SyntaxSymbol identifier, int address)
    {
        assert myDefinedNames.size() == address;
        myDefinedNames.add(identifier.getName());
        return new ModuleDefinedBinding(identifier, address, getModuleId());
    }

    @Override
    public BaseSymbol getDefinedName(int address)
    {
        return myDefinedNames.get(address);
    }

    BaseSymbol[] extractDefinedNames()
    {
        assert definitionCount() == myDefinedNames.size();
        return myDefinedNames.toArray(BaseSymbol.EMPTY_ARRAY);
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


    /**
     * A reference to a module-level variable in the lexically enclosing
     * namespace.
     */
    static final class CompiledModuleVariableReference
        implements CompiledForm
    {
        private final int            myAddress;
        private final SourceLocation myLocation;

        CompiledModuleVariableReference(int address, SourceLocation location)
        {
            myAddress  = address;
            myLocation = location;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            NamespaceStore ns = store.namespace();
            Object result = ns.lookup(myAddress);

            // Check for forward references.
            if (result != null) return result;

            // Synthesize an identifier for stack traces.
            BaseSymbol name = ns.getDefinedName(myAddress);
            SyntaxSymbol id = SyntaxSymbol.make(myLocation, name);

            throw new UnboundIdentifierException(id);
        }
    }
}
