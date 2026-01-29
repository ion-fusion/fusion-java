// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTailN;
import static dev.ionfusion.fusion.FusionString.isString;
import static dev.ionfusion.fusion.FusionString.unsafeStringToJavaString;
import static dev.ionfusion.fusion.FusionSyntax.unsafeSyntaxUnwrap;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.GlobalState.PROVIDE;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidAbsoluteModulePath;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidModuleName;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidModulePath;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.runtime.base.ModuleIdentity;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @see <a href="http://docs.racket-lang.org/reference/module.html">Racket
 * modules</a>
 */
final class ModuleForm
    extends SyntacticForm
{
    private static final String STX_PROP_MODULE_IDENTITY   = "module identity";
    private static final String STX_PROP_LANGUAGE_IDENTITY = "language identity";
    private static final String STX_PROP_REQUIRED_MODULES  = "required modules";
    private static final String STX_PROP_DEFINED_NAMES     = "defined names";


    private final DynamicParameter myCurrentModuleDeclareName;
    private final ModuleNameResolver myModuleNameResolver;

    ModuleForm(ModuleNameResolver moduleNameResolver,
               DynamicParameter currentModuleDeclareName)
    {
        myCurrentModuleDeclareName = currentModuleDeclareName;
        myModuleNameResolver = moduleNameResolver;
    }


    @Override
    SyntaxValue expand(Expander expander, Environment envOutsideModule,
                       SyntaxSexp source)
        throws FusionException
    {
        final Evaluator      eval     = expander.getEvaluator();
        final GlobalState    globals  = eval.getGlobalState();
        final ModuleRegistry registry = eval.findCurrentNamespace().getRegistry();

        SyntaxChecker check = check(eval, source);
        if (! expander.isTopLevelContext())
        {
            throw check.failure("`module` declaration not at top-level");
        }

        // Ensure there's a name and language. We check for a body below, but
        // want a different error message for that situation.
        if (source.size(eval) < 3)
        {
            throw check.failure("Malformed module declaration");
        }

        SyntaxSymbol name = check.requiredIdentifier("module name", 1);
        ModuleIdentity id = determineIdentity(eval, envOutsideModule, name);
        source = (SyntaxSexp)
            source.copyWithProperty(eval, STX_PROP_MODULE_IDENTITY, id);

        ModuleIdentity languageId;
        {
            String path = check.requiredText(eval, "initial module path", 2);
            SyntaxValue initialBindingsStx = source.get(eval, 2);

            if (! isValidModulePath(path))
            {
                String message = "Invalid module path for language: "
                    + initialBindingsStx.toString();
                throw new ModuleNotFoundException(message, initialBindingsStx);
            }
            if (! isValidAbsoluteModulePath(path))
            {
                String message = "Module path for language must be absolute: "
                    + initialBindingsStx.toString();
                throw new ModuleNotFoundException(message, initialBindingsStx);
            }

            try
            {
                languageId =
                    myModuleNameResolver.resolve(eval, id, initialBindingsStx,
                                                 true /*load it*/);
                // Force instantiation to tie any exception to the declaration.
                registry.instantiate(eval, languageId);
            }
            catch (FusionException e)
            {
                e.addContext(initialBindingsStx.getLocation());
                throw e;
            }

            source = (SyntaxSexp)
                source.copyWithProperty(eval,
                                        STX_PROP_LANGUAGE_IDENTITY, languageId);
        }


        if (source.size(eval) < 4)
        {
            throw check.failure("Module has no body");
        }

        // The new namespace shares the registry of current-namespace.
        // We use the module-name symbol as lexical context for bindings
        // introduced by the language, so they "match" references in the body
        // of the module. The leading 'module' symbol is enriched differently
        // when this is called via `eval` and top-level evaluation.
        ModuleNamespace moduleNamespace =
            new ModuleNamespace(eval, registry,
                                (SyntaxSymbol) source.get(eval, 1),
                                id, languageId);

        // TODO Implement #%module-begin and #%plain-module-begin
        //  https://github.com/ion-fusion/fusion-java/issues/78
        expander = expander.enterModuleContext();


        // Pass 1: locate definitions and install dummy bindings
        ArrayList<SyntaxSexp>  provideForms      = new ArrayList<>();
        ArrayList<SyntaxValue> otherForms        = new ArrayList<>();
        ArrayList<Boolean>     expandedFormFlags = new ArrayList<>();

        LinkedList<SyntaxValue> forms = new LinkedList<>();
        source.extract(eval, forms, 3);

        int formsAlreadyWrapped = 0;
        while (! forms.isEmpty())
        {
            SyntaxValue form = forms.pop();
            if (formsAlreadyWrapped == 0)
            {
                // This is ugly, but we don't want to do this twice. Forms
                // that were spliced by 'begin' have already had this done.
                form = moduleNamespace.syntaxIntroduce(form);
            }
            else
            {
                formsAlreadyWrapped--;
            }

            boolean formIsExpanded = false;

            SyntaxValue expanded =
                expander.partialExpand(moduleNamespace, form);

            if (expanded instanceof SyntaxSexp)
            {
                SyntaxSexp sexp = (SyntaxSexp)expanded;
                Binding binding = sexp.firstTargetBinding(eval);

                if (binding == globals.myKernelDefineValuesBinding)
                {
                    expanded = DefineValuesForm.predefine(eval, moduleNamespace,
                                                          sexp, form);
                }
                else if (binding == globals.myKernelDefineSyntaxBinding)
                {
                    try
                    {
                        expanded = expander.expand(moduleNamespace, expanded);
                        // TODO this is getting compiled twice
                        eval.evalExpandedStx(moduleNamespace, expanded);
                    }
                    catch (FusionException e)
                    {
                        e.addContext(form.getLocation());
                        throw e;
                    }
                    formIsExpanded = true;
                }
                else if (binding == globals.myKernelRequireBinding)
                {
                    try
                    {
                        expanded = expander.expand(moduleNamespace, expanded);
                        // TODO This needs to visit, not instantiate, modules.
                        // TODO this is getting compiled twice
                        eval.evalExpandedStx(moduleNamespace, expanded);
                    }
                    catch (FusionException e)
                    {
                        e.addContext(form.getLocation());
                        throw e;
                    }
                    formIsExpanded = true;
                }
                else if (binding == globals.myKernelProvideBinding)
                {
                    provideForms.add(sexp);
                    expanded = null;
                }
                else if (binding == globals.myKernelBeginBinding)
                {
                    // Splice 'begin' into the module-begin sequence
                    SyntaxSymbol beginId = (SyntaxSymbol) sexp.get(eval, 0);
                    int last = sexp.size(eval) - 1;
                    for (int i = last; i != 0;  i--)
                    {
                        SyntaxValue expr = sexp.get(eval, i);
                        expr = expr.trackOrigin(eval, expanded, beginId);
                        forms.push(expr);
                    }
                    formsAlreadyWrapped += last;
                    expanded = null;
                }
            }

            if (expanded != null)
            {
                otherForms.add(expanded);
                expandedFormFlags.add(formIsExpanded);
            }
        }

        source = (SyntaxSexp)
            source.copyWithProperty(eval,
                                    STX_PROP_REQUIRED_MODULES,
                                    moduleNamespace.requiredModuleIds())
                  .copyWithProperty(eval,
                                    STX_PROP_DEFINED_NAMES,
                                    moduleNamespace.extractDefinedNames());


        // Pass 2: Expand the expressions. We also rearrange the forms,
        // but that's not really for any functional reason.

        SyntaxValue[] subforms =
            new SyntaxValue[3 + otherForms.size() + provideForms.size()];

        int i = 0;
        subforms[i++] = source.get(eval, 0); // module
        subforms[i++] = source.get(eval, 1); // name
        subforms[i++] = source.get(eval, 2); // language

        Iterator<Boolean> expanded = expandedFormFlags.iterator();
        for (SyntaxValue stx : otherForms)
        {
            if (! expanded.next())
            {
                Binding firstBinding = firstTargetBindingOfSexp(eval, stx);
                if (firstBinding == globals.myKernelDefineValuesBinding)
                {
                    assert expander.isModuleContext();
                    stx = expander.expand(moduleNamespace, stx);
                }
                else
                {
                    stx = expander.expandExpression(moduleNamespace, stx);
                }
            }
            subforms[i++] = stx;
        }

        // Push all the `provide` forms to the end of the module.
        for (SyntaxValue stx : provideForms)
        {
            stx = expander.expand(moduleNamespace, stx);
            subforms[i++] = stx;
        }

        return source.copyReplacingChildren(eval, subforms);
    }

    /**
     * @return null if stx isn't a sexp or doesn't start with an identifier.
     * Null is also equivalent to a {@link FreeBinding} on a lead identifier.
     */
    private static Binding firstTargetBindingOfSexp(Evaluator eval,
                                                    SyntaxValue stx)
        throws FusionException
    {
        if (stx instanceof SyntaxSexp)
        {
            return ((SyntaxSexp) stx).firstTargetBinding(eval);
        }
        return null;
    }


    //========================================================================


    @Override
    void evalCompileTimePart(Compiler comp,
                             TopLevelNamespace topNs,
                             SyntaxSexp topStx)
        throws FusionException
    {
        CompiledForm compiledForm = compile(comp, topNs, topStx);

        // Evaluation of `module` simply registers it. We don't want to visit
        // or instantiate the module here; a later `require` would visit it.
        comp.getEvaluator().eval(topNs, compiledForm);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment envOutsideModule,
                         SyntaxSexp moduleStx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        Object moduleDatum = moduleStx.unwrap(eval);

        // TODO We repeat work here that was done during expansion.
        // Racket stashes the necessary data in syntax properties.
        // See Racket reference 11.9.1
        // http://docs.racket-lang.org/reference/Expanding_Top-Level_Forms.html#%28part._modinfo%29

        ModuleIdentity id = (ModuleIdentity)
            moduleStx.findProperty(eval, STX_PROP_MODULE_IDENTITY);

        Namespace moduleNamespace;
        {
            ModuleIdentity languageId = (ModuleIdentity)
                moduleStx.findProperty(eval, STX_PROP_LANGUAGE_IDENTITY);
            ModuleIdentity[] required = (ModuleIdentity[])
                moduleStx.findProperty(eval, STX_PROP_REQUIRED_MODULES);
            assert required[0] == languageId;

            ModuleRegistry registry =
                envOutsideModule.namespace().getRegistry();

            moduleNamespace = new ModuleNamespace(eval, registry, id, required);
        }

        // Skip `module` name language
        moduleDatum = unsafePairTailN(eval, moduleDatum, 3);

        ArrayList<CompiledForm> otherForms = new ArrayList<>();

        String docs = null;
        if (isPair(eval, moduleDatum))
        {
            Object maybeDoc =
                unsafeSyntaxUnwrap(eval, unsafePairHead(eval, moduleDatum));
            if (isString(eval, maybeDoc))
            {
                // We're gonna call this documentation!
                moduleDatum = unsafePairTail(eval, moduleDatum);
                if (comp.isDocCollectingEnabled())
                {
                    docs = unsafeStringToJavaString(eval, maybeDoc);
                }
            }
        }


        final Binding kernelRequireBinding =
            eval.getGlobalState().myKernelRequireBinding;
        final Binding kernelProvideBinding =
            eval.getGlobalState().myKernelProvideBinding;

        for (;
             isPair(eval, moduleDatum);
             moduleDatum = unsafePairTail(eval, moduleDatum))
        {
            SyntaxValue form = (SyntaxValue) unsafePairHead(eval, moduleDatum);

            Binding firstBinding = firstTargetBindingOfSexp(eval, form);
            if (firstBinding == kernelRequireBinding)
            {
                // All require forms have already been evaluated.
                // We preserve them through macro expansion so that they are
                // retained by `expand` and similar operations.
                continue;
            }

            if (firstBinding == kernelProvideBinding)
            {
                // Expansion pushes all `provide` forms to the end of the
                // module, so when we hit one we know we're done compiling.
                break;
            }

            CompiledForm compiled =
                comp.compileExpression(moduleNamespace, form);
            otherForms.add(compiled);
        }

        ProvidedBinding[] providedBindings =
            providedBindings(eval, moduleNamespace, moduleDatum);

        CompiledForm[] otherFormsArray =
            otherForms.toArray(CompiledForm.EMPTY_ARRAY);

        BaseSymbol[] definedNames = (BaseSymbol[])
            moduleStx.findProperty(eval, STX_PROP_DEFINED_NAMES);

        return new CompiledModule(id,
                                  docs,
                                  moduleNamespace.requiredModuleIds(),
                                  definedNames,
                                  moduleNamespace.extractBindingDocs(),
                                  providedBindings,
                                  otherFormsArray);
    }

    private ModuleIdentity determineIdentity(Evaluator eval,
                                             Environment envOutsideModule,
                                             SyntaxSymbol moduleNameSymbol)
        throws SyntaxException
    {
        if (! isValidModuleName(moduleNameSymbol.stringValue()))
        {
            throw new SyntaxException("declared module name",
                                      "Expected a valid module name",
                                      moduleNameSymbol);
        }

        // When the module name resolver loads a module from a repository, as
        // with `require`, it sets this parameter with the resolved identity.
        // In such cases, we ignore the declared name.
        Object current = myCurrentModuleDeclareName.currentValue(eval);
        if (current instanceof ModuleIdentity)
        {
            return (ModuleIdentity) current;
        }

        // Otherwise, we must be evaluating the `module` form at namespace-level,
        // in which case we treat it as a child module of the current namespace.
        Namespace outsideNs = envOutsideModule.namespace();

        // current_namespace should be the top-level we're evaluating in.
        assert outsideNs == eval.findCurrentNamespace();

        String declaredName = moduleNameSymbol.stringValue();
        return ModuleIdentity.forChild(outsideNs.getModuleId(), declaredName);
    }


    //========================================================================


    private static final class ProvideCompiler
    {
        private final Map<BaseSymbol,Binding> myExportedNames = new IdentityHashMap<>();
        private final List<ProvidedBinding>   myBindings      = new ArrayList<>();


        /**
         * Exports an identifier/binding, as in {@code (provide exportId)}.
         * This creates a new {@code ProvidedBinding} at the location of the
         * {@code exportId}.
         *
         * @param exportId the identifier to be exported.
         * @param binding the binding to be exported.
         */
        private void addBinding(SyntaxChecker check,
                                SyntaxSymbol  exportId,
                                Binding       binding)
            throws FusionException
        {
            BaseSymbol name = exportId.getName();
            Binding prior = myExportedNames.put(name, binding);
            if (prior != null && ! binding.sameTarget(prior))
            {
                String message =
                    "the identifier " +
                        printQuotedSymbol(name.stringValue()) +
                        " is being exported with multiple bindings";
                throw check.failure(message, exportId);
            }

            ProvidedBinding provided = binding.provideAs(name, exportId.getLocation());
            assert name == provided.getName();

            myBindings.add(provided);
        }

        ProvidedBinding[] compiledBindings()
        {
            return myBindings.toArray(new ProvidedBinding[0]);
        }

        void compileProvide(Evaluator eval, SyntaxSexp formStx)
            throws FusionException
        {
            SyntaxChecker check = new SyntaxChecker(eval, PROVIDE, formStx);

            // Skip the leading `provide` keyword.
            for (Object formDatum = unsafePairTail(eval, formStx.unwrap(eval));
                 isPair(eval, formDatum);
                 formDatum = unsafePairTail(eval, formDatum))
            {
                Object clauseObj = unsafePairHead(eval, formDatum);
                if (clauseObj instanceof SyntaxSymbol)
                {
                    // (provide insideId)
                    SyntaxSymbol insideId = (SyntaxSymbol) clauseObj;
                    addBinding(check, insideId, insideId.getBinding());
                }
                else
                {
                    // (provide (FORM ...))
                    SyntaxSexp clauseStx = (SyntaxSexp) clauseObj;
                    SyntaxSymbol formName = clauseStx.firstIdentifier(eval);
                    switch (formName.getName().stringValue())
                    {
                        case "all_defined":
                        {
                            // Expansion of all_defined_out provides the
                            // individual symbols before this marker.
                            break;
                        }
                        case "rename":
                        {
                            SyntaxSymbol insideId = (SyntaxSymbol)
                                clauseStx.get(eval, 1);
                            SyntaxSymbol outsideId = (SyntaxSymbol)
                                clauseStx.get(eval, 2);
                            addBinding(check, outsideId, insideId.getBinding());
                            break;
                        }
                        default:
                        {
                            throw check.failure("invalid provide-spec");
                        }
                    }
                }
            }
        }
    }


    /**
     * Process all the provide-forms, which macro-expansion has grouped
     * together at the end of the module.
     */
    private ProvidedBinding[] providedBindings(Evaluator eval,
                                               Namespace ns,
                                               Object    moduleDatum)
        throws FusionException
    {
        ProvideCompiler compiler = new ProvideCompiler();

        while (isPair(eval, moduleDatum))
        {
            SyntaxSexp form = (SyntaxSexp) unsafePairHead(eval, moduleDatum);
            compiler.compileProvide(eval, form);

            moduleDatum = unsafePairTail(eval, moduleDatum);
        }

        return compiler.compiledBindings();
    }


    //========================================================================


    static final class CompiledModule
        implements CompiledForm
    {
        // We use arrays to hold the provided bindings, rather than a map
        // from names to bindings, because we want compiled forms to be flat
        // and compact.
        private final ModuleIdentity    myId;
        private final String            myDocs;
        private final ModuleIdentity[]  myRequiredModules;
        private final BaseSymbol[]      myDefinedNames;
        private final BindingDoc[]      myBindingDocs;
        private final ProvidedBinding[] myProvidedBindings;
        private final CompiledForm[]    myBody;

        private CompiledModule(ModuleIdentity    id,
                               String            docs,
                               ModuleIdentity[]  requiredModules,
                               BaseSymbol[]      definedNames,
                               BindingDoc[]      bindingDocs,
                               ProvidedBinding[] providedBindings,
                               CompiledForm[]    body)
        {
            myId                  = id;
            myDocs                = docs;
            myRequiredModules     = requiredModules;
            myDefinedNames        = definedNames;
            myBindingDocs         = bindingDocs;
            myProvidedBindings    = providedBindings;
            myBody                = body;
        }

        ModuleIdentity[] getRequiredModules()
        {
            return myRequiredModules;
        }

        @Override
        public Object doEval(Evaluator eval, Store ignored)
            throws FusionException
        {
            ModuleRegistry registry =
                eval.findCurrentNamespace().getRegistry();
            ModuleNameResolver resolver =
                eval.getGlobalState().myModuleNameResolver;
            registry.declare(resolver, myId, this);

            return voidValue(eval);
        }


        ModuleInstance instantiate(Evaluator eval)
            throws FusionException
        {
            ModuleRegistry registry =
                eval.findCurrentNamespace().getRegistry();

            ModuleStore[] requiredModuleStores = requireModules(eval, registry);

            // Allocate just enough space for the top-level bindings.
            ModuleStore store =
                new ModuleStore(registry, requiredModuleStores,
                                myDefinedNames, myBindingDocs);

            ModuleInstance module =
                new ModuleInstance(myId, myDocs, store, myProvidedBindings);

            for (CompiledForm form : myBody)
            {
                // TODO Each eval should be wrapped with a prompt.
                //  https://github.com/ion-fusion/fusion-java/issues/79
                //  See Racket reference for `module`.
                eval.eval(store, form);
            }

            return module;
        }

        private ModuleStore[] requireModules(Evaluator eval,
                                             ModuleRegistry registry)
            throws FusionException
        {
            int count = myRequiredModules.length;
            ModuleStore[] stores = new ModuleStore[count];
            for (int i = 0; i < count; i++)
            {
                ModuleInstance module =
                    registry.instantiate(eval, myRequiredModules[i]);
                stores[i] = module.getStore();
            }

            return stores;
        }
    }
}
