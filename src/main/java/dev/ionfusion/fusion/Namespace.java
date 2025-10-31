// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.BindingSite.makeDefineBindingSite;
import static dev.ionfusion.fusion.BindingSite.makeImportBindingSite;
import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionVoid.voidValue;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.ModuleDefinedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion.TopLevelNamespace.TopLevelDefinedBinding;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Expand- and compile-time environment for all top-level sequences, and
 * eval-time store for non-module top levels.
 * <p>
 * Since top-levels and modules have different behavior around imports and
 * defines, that responsibility is delegated to subclasses.
 *
 * @see TopLevelNamespace
 * @see ModuleNamespace
 */
abstract class Namespace
    implements Environment, NamespaceStore
{
    /**
     * Denotes a binding installed at namespace-level.
     */
    abstract static class NsBinding
        extends Binding
    {
        /**
         * Attempts to bind a definition atop this effective binding.
         *
         * @return the definition to use.  Null indicates that a new definition
         * is to be installed.
         *
         * @throws AmbiguousBindingFailure if this binding cannot be replaced
         *   with a definition.
         */
        abstract NsDefinedBinding redefine(SyntaxSymbol identifier,
                                           SyntaxValue formForErrors)
            throws AmbiguousBindingFailure;

        /**
         * Attempts to bind an import atop this effective binding.
         *
         * @return the binding to use.
         *
         * @throws AmbiguousBindingFailure if this binding cannot be replaced
         *   with an import.
         */
        abstract RequiredBinding require(SyntaxSymbol localId,
                                         ProvidedBinding provided,
                                         SyntaxValue formForErrors)
            throws AmbiguousBindingFailure;

        /**
         * Returns the namespace-level definition that this binding <em>is</em>
         * or <em>shadows</em>.
         *
         * @return null if there's no namespace-level definition.
         */
        abstract NsDefinedBinding definition();
    }



    /**
     * Denotes a namespace-level definition binding, either top-level or
     * module-level.
     *
     * @see TopLevelDefinedBinding
     * @see ModuleDefinedBinding
     */
    abstract class NsDefinedBinding
        extends NsBinding
    {
        private final BaseSymbol  myName;
        private final String      myDebugName;
        private final BindingSite mySite;
        final int myAddress;

        NsDefinedBinding(SyntaxSymbol identifier, int address)
        {
            myName      = identifier.getName();
            myDebugName = identifier.debugString();
            mySite      = makeDefineBindingSite(identifier.getLocation());
            myAddress   = address;
        }

        @Override
        final BaseSymbol getName()
        {
            return myName;
        }

        @Override
        BindingSite getBindingSite()
        {
            return mySite;
        }

        final String getDebugName()
        {
            return myDebugName;
        }

        @Override
        final NsDefinedBinding definition()
        {
            return this;
        }

        final boolean isOwnedBy(Namespace ns)
        {
            // Would prefer to compare the Namespaces, but different instances
            // are used during compilation.
            return Namespace.this.myModuleId == ns.myModuleId;
        }

        @Override
        public boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public final int hashCode()
        {
            return System.identityHashCode(this);
        }

        @Override
        public abstract String toString(); // Force subclasses to implement
    }


    /**
     * A binding added to a namespace via {@code require} or a language
     * declaration.
     */
    abstract static class RequiredBinding
        extends NsBinding
    {
        private final BaseSymbol      myName;
        private final String          myDebugName;
        private final SourceLocation  myLoc;
        private final ProvidedBinding myProvide;
        private BindingSite           mySite;

        RequiredBinding(SyntaxSymbol identifier, ProvidedBinding provide)
        {
            assert provide != null;
            myName      = identifier.getName();
            myDebugName = identifier.debugString();
            myLoc       = identifier.getLocation();
            myProvide   = provide;
        }

        @Override
        final BaseSymbol getName()
        {
            return myName;
        }

        @Override
        BindingSite getBindingSite()
        {
            if (mySite == null)
            {
                mySite = makeImportBindingSite(myLoc,
                                               myProvide.getBindingSite());
            }
            return mySite;
        }

        final String getDebugName()
        {
            return myDebugName;
        }

        /** Gets the binding that was {@code provide}d. */
        final ProvidedBinding getProvided()
        {
            return myProvide;
        }

        @Override
        final ModuleDefinedBinding target()
        {
            return myProvide.target();
        }

        @Override
        final Object lookup(Namespace ns)
        {
            return myProvide.lookup(ns);
        }

        @Override
        final String mutationSyntaxErrorMessage()
        {
             return "cannot mutate an imported variable";
        }

        @Override
        public final boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * Exposes the bindings visible at namespace-level.
     */
    static abstract class NamespaceWrap
        extends EnvironmentWrap
    {
        NamespaceWrap(Namespace ns)
        {
            super(ns);
        }

        @Override
        Binding resolveTopMaybe(BaseSymbol           name,
                                Iterator<SyntaxWrap> moreWraps,
                                Set<MarkWrap>        returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                // The base implementation calls resolveTop here.
                return nextWrap.resolveMaybe(name, moreWraps, returnMarks);
            }
            return null;
        }

        @Override
        public String toString()
        {
            ModuleIdentity id =
                ((Namespace) getEnvironment()).getModuleId();
            return "{{{NS " + id.absolutePath() + "}}}";
        }
    }


    private final ModuleRegistry myRegistry;
    private final ModuleIdentity myModuleId;

    /**
     * Assigns required modules to integer addresses, for use in compiled
     * forms.
     */
    private final HashMap<ModuleIdentity,Integer> myRequiredModules =
        new HashMap<>();
    private final ArrayList<ModuleStore> myRequiredModuleStores =
        new ArrayList<>();

    private final SyntaxWraps          myWraps;
    private final BoundIdMap<NsBinding> myBindings = new BoundIdMap<>();
    private       int                   myDefinitionCount;
    private final ArrayList<Object>    myValues   = new ArrayList<>();
    private ArrayList<BindingDoc> myBindingDocs;


    /**
     * @param registry must not be null.
     * @param id must not be null.
     * @param wraps generates the {@link SyntaxWraps} for this namespace, given
     *   a reference to {@code this}.
     */
    Namespace(ModuleRegistry                   registry,
              ModuleIdentity                   id,
              Function<Namespace, SyntaxWraps> wraps)
    {
        myRegistry = registry;
        myModuleId = id;
        myWraps    = wraps.apply(this);
    }

    @Override
    public String toString()
    {
        return myModuleId.toString();
    }

    @Override
    public final ModuleRegistry getRegistry()
    {
        return myRegistry;
    }

    @Override
    public final Namespace namespace()
    {
        return this;
    }

    @Override
    public final int getDepth()
    {
        return 0;
    }

    @Override
    public final Object lookup(int rib, int address)
    {
        String message = "Rib not found: " + rib + ',' + address;
        throw new IllegalStateException(message);
    }

    @Override
    public final void set(int rib, int address, Object value)
    {
        String message = "Rib not found: " + rib + ',' + address;
        throw new IllegalStateException(message);
    }

    /**
     * Gets the identity of the module associated with this namespace.
     * For non-module namespaces, this returns a synthetic identity that's
     * unique amongst all namespaces.
     *
     * @return not null.
     */
    final ModuleIdentity getModuleId()
    {
        return myModuleId;
    }

    /**
     * Returns a base module to use with {@link ModuleIdentity#forPath} to
     * resolve relative paths.
     */
    ModuleIdentity getResolutionBase()
    {
        return myModuleId;
    }

    /**
     * How many namespace-level definitions do we have?
     * This doesn't count imports.
     */
    final int definitionCount()
    {
        return myDefinitionCount;
    }

    /**
     * Collects the bindings defined in this namespace; does not include imported
     * bindings.
     *
     * @return not null.
     */
    final Collection<NsDefinedBinding> getDefinedBindings()
    {
        ArrayList<NsDefinedBinding> definitions =
            new ArrayList<>(definitionCount());

        for (NsBinding b : myBindings.values())
        {
            NsDefinedBinding def = b.definition();
            if (def != null)
            {
                definitions.add(def);
            }
        }

        assert definitions.size() == definitionCount();
        return definitions;
    }

    //========================================================================

    /**
     * Adds wraps to the syntax object to give it the bindings of this
     * namespace and of required modules.
     *
     * @return a copy of the given syntax value, with the additional context.
     */
    final SyntaxValue syntaxIntroduce(SyntaxValue source)
        throws FusionException
    {
        // TODO there's a case where we are applying the same wraps that are
        // already on the source.  This happens when expand-ing (and maybe when
        // eval-ing at top-level source that's from that same context.
        source = source.addWraps(myWraps);
        return source;
    }


    final NsBinding installBinding(SyntaxSymbol boundIdentifier,
                                   NsBinding    binding)
    {
        return myBindings.put(boundIdentifier, binding);
    }

    /**
     * Resolve as with {@code bound_identifier_equal}.
     *
     * @return null if there's no binding in this namespace.
     */
    private NsBinding resolveBoundMaybe(SyntaxSymbol identifier)
    {
        return myBindings.get(identifier);
    }

    /**
     * @param binding must not be null.
     * @param marks must not be null.
     *
     * @return null if identifier isn't bound here.
     */
    final NsBinding resolveMaybe(Binding binding, Set<MarkWrap> marks)
    {
        return myBindings.get(binding, marks);
    }

    @Override
    public final NsBinding substituteFree(BaseSymbol name, Set<MarkWrap> marks)
    {
        Binding b = new FreeBinding(name);
        return resolveMaybe(b, marks);
    }

    @Override
    public final Binding substitute(Binding binding, Set<MarkWrap> marks)
    {
        Binding subst = resolveMaybe(binding, marks);
        if (subst == null) subst = binding;
        return subst;
    }

    /**
     * Resolves an identifier to a namespace-level definition (not an import).
     *
     * @return null if identifier isn't defined here.
     */
    final NsDefinedBinding resolveDefinition(SyntaxSymbol identifier)
    {
        identifier = identifier.copyAndResolveTop();
        NsBinding nsb = resolveBoundMaybe(identifier);
        return (nsb == null ? null : nsb.definition());
    }


    /**
     * @param name must be non-empty.
     *
     * @return null is equivalent to a {@link FreeBinding}.
     */
    final Binding resolveMaybe(BaseSymbol name)
    {
        return myWraps.resolveMaybe(name);
    }

    /**
     * @param name must be non-empty.
     *
     * @return null is equivalent to a {@link FreeBinding}.
     */
    final Binding resolveMaybe(String name)
    {
        BaseSymbol symbol = FusionSymbol.makeSymbol(null, name);
        return resolveMaybe(symbol);
    }


    abstract NsDefinedBinding newDefinedBinding(SyntaxSymbol identifier,
                                                int          address);


    /**
     * Creates a definition binding, but no value, for a name.
     * Used during expansion phase, before evaluating the right-hand side.
     *
     * @return a copy of the identifier that has the new binding attached.
     */
    SyntaxSymbol predefine(SyntaxSymbol identifier, SyntaxValue formForErrors)
        throws FusionException
    {
        NsDefinedBinding newDefinition = null;

        identifier = identifier.copyAndResolveTop();

        NsBinding entry = resolveBoundMaybe(identifier);
        if (entry != null)
        {
            newDefinition = entry.redefine(identifier, formForErrors);
        }
        if (newDefinition == null)
        {
            newDefinition = newDefinedBinding(identifier, myDefinitionCount);
            myDefinitionCount++;
        }

        installBinding(identifier, newDefinition);

        return identifier.copyReplacingBinding(newDefinition);
    }

    /**
     * Creates or updates a namespace-level binding.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     */
    final void bind(NsDefinedBinding binding, Object value)
    {
        set(binding.myAddress, value);

        if (value instanceof NamedValue)
        {
            String inferredName = binding.getName().stringValue();
            if (inferredName != null)
            {
                ((NamedValue)value).inferName(inferredName);
            }
        }
    }


    private <T> void set(ArrayList<T> list, int address, T value)
    {
        int size = list.size();
        if (address < size)
        {
            list.set(address, value);
        }
        else // We need to grow the list. Annoying lack of API to do this.
        {
            list.ensureCapacity(myDefinitionCount);        // Grow all at once
            for (int i = size; i < address; i++)
            {
                list.add(null);
            }
            list.add(value);
        }
    }


    /**
     * Updates a pre-defined namespace-level variable.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     */
    @Override
    public final void set(int address, Object value)
    {
        set(myValues, address, value);
    }


    /**
     * Creates or updates a namespace-level binding.
     * Allows rebinding of existing names!
     *
     * @param value must not be null
     *
     * @throws IllegalArgumentException if the name is null or empty.
     */
    public final void bind(String name, Object value)
        throws FusionException
    {
        if (name == null || name.length() == 0)
        {
            String message = "bound name must be non-null and non-empty";
            throw new IllegalArgumentException(message);
        }

        // WARNING: We pass null evaluator because we know its not used.
        //          That is NOT SUPPORTED for user code!
        SyntaxSymbol identifier = SyntaxSymbol.make(null, name);

        identifier = predefine(identifier, null);
        NsDefinedBinding binding = (NsDefinedBinding) identifier.getBinding();
        bind(binding, value);
    }


    //========================================================================
    //  Require

    /**
     * Denotes a new binding created by {@code require}, possibly renaming the
     * exported symbol along the way.
     */
    static final class RequireRenameMapping
    {
        private final SyntaxSymbol myLocalIdentifier;
        private final BaseSymbol   myExportedIdentifier;

        RequireRenameMapping(SyntaxSymbol localIdentifier,
                             BaseSymbol exportedIdentifier)
        {
            myLocalIdentifier = localIdentifier;
            myExportedIdentifier = exportedIdentifier;
        }

        @Override
        public String toString()
        {
            return "RequireRenameMapping::{local:" + myLocalIdentifier
                + ",exported:" + myExportedIdentifier + '}';
        }
    }


    /**
     * Record a previously-instantiated module in our list of dependencies,
     * allocating it an address for runtime access to bindings.
     */
    final synchronized int recordRequiredModule(ModuleIdentity moduleId,
                                                ModuleInstance module)
    {
        Integer addr = myRequiredModules.get(moduleId);
        if (addr != null) return addr;

        if (module == null) module = myRegistry.lookup(moduleId);

        addr = myRequiredModules.size();
        myRequiredModules.put(moduleId, addr);
        myRequiredModuleStores.add(module.getStore());
        return addr;
    }

    final ModuleInstance instantiateRequiredModule(Evaluator eval,
                                                   ModuleIdentity moduleId)
        throws FusionException
    {
        ModuleInstance module = myRegistry.instantiate(eval, moduleId);
        recordRequiredModule(moduleId, module);
        return module;
    }


    /**
     * Loads a module definition into this namespace's registry, without
     * instantiating it.
     *
     * @param modulePath is an absolute or relative module path.
     */
    final ModuleIdentity resolveAndLoadModule(Evaluator eval, String modulePath)
        throws FusionException
    {
        // Make sure the resolver uses our registry.
        eval = eval.parameterizeCurrentNamespace(this);

        return eval.findResolver().resolveModulePath(eval,
                                                     getModuleId(),
                                                     modulePath,
                                                     true /* load */,
                                                     null /* stxForErrors */);
    }

    void attachModule(Evaluator eval, Namespace srcNamespace, String modulePath)
        throws FusionException
    {
        ModuleNameResolver resolver = eval.findResolver();

        // Resolve the path WRT the *source* registry, so we can locate
        // manually-declared modules loaded into it.
        // See the file rkt/ns-attach.rkt for demonstration test case.
        eval = eval.parameterizeCurrentNamespace(srcNamespace);

        ModuleIdentity id =
            resolver.resolveModulePath(eval, myModuleId, modulePath, false, null);

        myRegistry.attach(resolver, srcNamespace.getRegistry(), id);
    }


    /**
     * Instantiates a module into this namespace's registry, then imports all
     * exported bindings from it.
     *
     * @param modulePath is an absolute or relative module path.
     */
    final void require(Evaluator eval, String modulePath)
        throws FusionException
    {
        // Make sure the resolver uses our registry.
        eval = eval.parameterizeCurrentNamespace(this);

        ModuleIdentity id = resolveAndLoadModule(eval, modulePath);

        require(eval, null /* lexical context */, id);
    }

    /**
     * Imports all exported bindings from a module.
     * This is used by {@code (require module_path)}.
     */
    final void require(Evaluator      eval,
                       SyntaxText     context,
                       ModuleIdentity moduleId)
        throws FusionException
    {
        ModuleInstance module = instantiateRequiredModule(eval, moduleId);

        for (ProvidedBinding provided : module.providedBindings())
        {
            SyntaxSymbol id = SyntaxSymbol.make(eval, null, provided.getName());
            id = (SyntaxSymbol) Syntax.applyContext(eval, context, id);
            installRequiredBinding(eval, id, provided);
        }
    }


    final void require(Evaluator eval, ModuleIdentity moduleId,
                       Iterator<RequireRenameMapping> mappings)
        throws FusionException
    {
        ModuleInstance module = instantiateRequiredModule(eval, moduleId);

        while (mappings.hasNext())
        {
            RequireRenameMapping mapping = mappings.next();
            BaseSymbol exportedId = mapping.myExportedIdentifier;
            ProvidedBinding provided = module.resolveProvidedName(exportedId);

            // TODO this is horribly hacky. Ideally we'd raise a syntax exn on
            //   the original require spec
            if (provided == null)
            {
                // TODO Error reporting is bad here, it's lost the location of
                //   the unbound id so it reports the entire require form (when
                //   this gets wrapped.
                SyntaxSymbol sym = SyntaxSymbol.make(eval, null, exportedId);
                throw new UnboundIdentifierException(sym);
            }

            installRequiredBinding(eval, mapping.myLocalIdentifier, provided);
        }
    }

    /**
     * @param eval must not be null.
     * @param localId will be re-resolved to the outside edge of this namespace.
     * @param provided the binding to import.
     * @throws AmbiguousBindingFailure
     */
    final void installRequiredBinding(Evaluator       eval,
                                      SyntaxSymbol    localId,
                                      ProvidedBinding provided)
        throws AmbiguousBindingFailure
    {
        localId = localId.copyAndResolveTop();

        RequiredBinding required;
        NsBinding entry = resolveBoundMaybe(localId);
        if (entry == null)
        {
            required = newRequiredBinding(localId, provided);
        }
        else
        {
            required = entry.require(localId, provided, null);
        }

        installBinding(localId, required);
    }

    /**
     * Creates a binding for a require that has no prior binding.
     */
    abstract RequiredBinding newRequiredBinding(SyntaxSymbol    localId,
                                                ProvidedBinding target);


    //========================================================================


    final boolean ownsDefinedBinding(Binding binding)
    {
        if (binding instanceof NsDefinedBinding)
        {
            return ((NsDefinedBinding) binding).isOwnedBy(this);
        }
        return false;
    }


    /**
     * Looks for a definition's value in this namespace, ignoring imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookupDefinition(NsDefinedBinding binding)
    {
        if (binding.isOwnedBy(this))
        {
            int address = binding.myAddress;
            if (address < myValues.size())         // for prepare-time lookup
            {
                return myValues.get(address);
            }
        }
        return null;
    }


    /**
     * Looks for a binding's value in this namespace, finding both definitions
     * and imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookup(Binding binding)
    {
        return binding.lookup(this);
    }


    /**
     * Looks for a binding's value in this namespace, finding both definitions
     * and imports.
     *
     * @return the binding's value, or null if there is none.
     */
    final Object lookup(String name)
    {
        Binding b = resolveMaybe(name);
        if (b == null)
        {
            return b;
        }
        else
        {
            return lookup(b);
        }
    }


    @Override
    public final Object lookup(int address)
    {
        return myValues.get(address);
    }

    @Override
    public final Object lookupImport(int moduleAddress, int bindingAddress)
    {
        ModuleStore store = myRequiredModuleStores.get(moduleAddress);
        return store.lookup(bindingAddress);
    }

    final Object[] extractValues()
    {
        return myValues.toArray();
    }


    /**
     * Translates a required module identity into an integer address for use
     * by compiled forms.  Note that some required modules may not be
     * explicitly declared in the source of the module, since they may be needed
     * to access variables imported from one module but defined in another, and
     * since such variables may come in via macro expansion.
     * <p>
     * Also note that we may need to dereference variables from other modules
     * at somewhat-arbitrary points during expansion and compilation, and that
     * expansion may trigger partial compilation and execution (eg when handling
     * `define-syntax`).
     * </p>
     * @return a zero-based address for the module, valid only within this
     * namespace (or its compiled form).
     */
    final int requiredModuleAddress(ModuleIdentity moduleId)
    {
        return recordRequiredModule(moduleId, null);
    }

    final synchronized ModuleIdentity[] requiredModuleIds()
    {
        assert myRequiredModules.size() == myRequiredModuleStores.size();
        ModuleIdentity[] ids = new ModuleIdentity[myRequiredModules.size()];
        for (Map.Entry<ModuleIdentity, Integer> entry
                : myRequiredModules.entrySet())
        {
            int address = entry.getValue();
            assert ids[address] == null;
            ids[address] = entry.getKey();
        }
        return ids;
    }

    @Override
    public final ModuleStore lookupRequiredModule(int moduleAddress)
    {
        return myRequiredModuleStores.get(moduleAddress);
    }


    //========================================================================
    // Documentation


    final void setDoc(String name, BindingDoc.Kind kind, String doc)
    {
        BindingDoc bDoc = new BindingDoc(kind,
                                         null, // usage
                                         doc);
        setDoc(name, bDoc);
    }

    final void setDoc(String name, BindingDoc doc)
    {
        NsDefinedBinding binding = (NsDefinedBinding) resolveMaybe(name);
        setDoc(binding.myAddress, doc);
    }

    public void setDoc(int address, BindingDoc doc)
    {
        if (myBindingDocs == null)
        {
            myBindingDocs = new ArrayList<>();
        }
        set(myBindingDocs, address, doc);
    }

    final BindingDoc document(int address)
    {
        if (myBindingDocs != null && address < myBindingDocs.size())
        {
            return myBindingDocs.get(address);
        }
        return null;
    }

    /**
     * @return may be shorter than the number of provided variables.
     */
    final BindingDoc[] extractBindingDocs()
    {
        if (myBindingDocs == null) return BindingDoc.EMPTY_ARRAY;

        BindingDoc[] docs = myBindingDocs.toArray(BindingDoc.EMPTY_ARRAY);
        myBindingDocs = null;
        return docs;
    }


    //========================================================================
    // Visitation


    abstract Object visit(Visitor v) throws FusionException;


    static abstract class Visitor
    {
        Object accept(Namespace ns) throws FusionException
        {
            String msg = "Visitor doesn't accept " + getClass();
            throw new IllegalStateException(msg);
        }

        Object accept(TopLevelNamespace ns) throws FusionException
        {
            return accept((Namespace) ns);
        }

        Object accept(ModuleNamespace ns) throws FusionException
        {
            return accept((Namespace) ns);
        }
    }


    //========================================================================


    static class CompiledNsDefine
        implements CompiledForm
    {
        private final String       myName;
        private final int          myAddress;
        private final CompiledForm myValueForm;

        CompiledNsDefine(String name, int address, CompiledForm valueForm)
        {
            assert name != null;
            myName      = name;
            myAddress   = address;
            myValueForm = valueForm;
        }

        @Override
        public final Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm);

            value = processValue(eval, store, value);

            NamespaceStore ns = store.namespace();
            ns.set(myAddress, value);

            if (value instanceof NamedValue)
            {
                ((NamedValue)value).inferName(myName);
            }

            return voidValue(eval);
        }

        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            eval.checkSingleResult(value, "module-level definition");
            return value;
        }
    }


    static final class CompiledNsDefineSyntax
        extends CompiledNsDefine
    {
        CompiledNsDefineSyntax(String name, int address,
                               CompiledForm valueForm)
        {
            super(name, address, valueForm);
        }

        @Override
        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            if (value instanceof Procedure)
            {
                Procedure xformProc = (Procedure) value;
                value = new MacroForm(xformProc);
            }
            else if (! (value instanceof SyntacticForm))
            {
                String message =
                    "define_syntax value is not a transformer: " +
                    safeWriteToString(eval, value);
                throw new ContractException(message);
            }

            return value;
        }
    }


    /**
     * Interprets non-single-binding {@code define_values} at module-level.
     * Single-binding forms are interpreted by {@link CompiledNsDefine}.
     */
    static class CompiledNsDefineValues
        implements CompiledForm
    {
        private final String[]     myNames;
        private final int[]        myAddresses;
        private final CompiledForm myValuesForm;

        CompiledNsDefineValues(String[] names,
                               int[] addresses,
                               CompiledForm valuesForm)
        {
            assert names.length != 1;
            myNames      = names;
            myAddresses  = addresses;
            myValuesForm = valuesForm;
        }

        @Override
        public final Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            int expectedCount = myNames.length;

            Object values = eval.eval(store, myValuesForm);
            if (values instanceof Object[])
            {
                Object[] vals = (Object[]) values;
                int actualCount = vals.length;
                if (expectedCount != actualCount)
                {
                    String expectation =
                        expectedCount + " results but received " + actualCount;
                    throw new ResultFailure("module-level definition",
                                            expectation, -1, vals);
                }

                NamespaceStore ns = store.namespace();

                for (int i = 0; i < expectedCount; i++)
                {
                    Object value = vals[i];
                    ns.set(myAddresses[i], value);
                    if (value instanceof NamedValue)
                    {
                        ((NamedValue)value).inferName(myNames[i]);
                    }
                }
            }
            else
            {
                String expectation = expectedCount + " results but received 1";
                throw new ResultFailure("module-level definition",
                                        expectation, -1, values);
            }

            return voidValue(eval);
        }
    }
}
