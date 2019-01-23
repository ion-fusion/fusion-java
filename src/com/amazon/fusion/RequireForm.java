// Copyright (c) 2012-2018 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionSexp.isPair;
import static com.amazon.fusion.FusionSexp.isSexp;
import static com.amazon.fusion.FusionSexp.unsafePairHead;
import static com.amazon.fusion.FusionSexp.unsafePairTail;
import static com.amazon.fusion.FusionSymbol.makeSymbol;
import static com.amazon.fusion.FusionSyntax.syntaxTrackOrigin;
import static com.amazon.fusion.FusionText.isText;
import static com.amazon.fusion.FusionVoid.voidValue;
import static com.amazon.fusion.GlobalState.REQUIRE;
import static com.amazon.ion.util.IonTextUtils.printString;
import com.amazon.fusion.Namespace.RequireRenameMapping;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Note that there is a resiliency problem with respect to multiple
 * imports: if a library module adds a binding thats already used by a user
 * module, there can be a conflict introduced by a new release of the library.
 * The same problem exists with Java code using {@code import *}, and the
 * recommended preventive measure is the same: robust modules should declare
 * their imported names explicitly using {@code only_in}, rather than using the
 * default "import everything" behavior.
 */
final class RequireForm
    extends SyntacticForm
{
    private final ModuleNameResolver myModuleNameResolver;

    RequireForm(ModuleNameResolver moduleNameResolver)
    {
        myModuleNameResolver = moduleNameResolver;
    }


    @Override
    SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();

        SyntaxChecker check = check(eval, stx);

        if (!expander.isModuleContext() && !expander.isTopLevelContext())
        {
            throw check.failure("May only appear in a top-level context " +
                                "or a module context");
        }

        int arity = check.arityAtLeast(2);

        ArrayList<SyntaxValue> expanded = new ArrayList<>(stx.size());
        expanded.add(stx.get(eval, 0));

        for (int i = 1; i < arity; i++)
        {
            SyntaxValue spec = stx.get(eval, i);
            Object datum = spec.unwrap(eval);
            if (isText(eval, datum))
            {
                expandRequireModule(eval, check, spec, datum, expanded);
            }
            else if (isSexp(eval, datum))
            {
                expandRequireSexp(expander, eval, check,
                                  (SyntaxSexp) spec, expanded);
            }
            else
            {
                throw check.failure("invalid require-spec", spec);
            }
        }

        SyntaxValue[] children = expanded.toArray(SyntaxValue.EMPTY_ARRAY);
        return stx.copyReplacingChildren(eval, children);
    }

    private void expandRequireModule(Evaluator eval,
                                     SyntaxChecker requireCheck,
                                     SyntaxValue spec,
                                     Object textDatum,
                                     ArrayList<SyntaxValue> expandedSpecs)
        throws FusionException
    {
        String path = FusionText.textToJavaString(eval, textDatum);
        checkValidModulePath(eval, requireCheck, path);

        expandedSpecs.add(spec);
    }

    private void expandRequireSexp(Expander expander,
                                   Evaluator eval,
                                   SyntaxChecker requireCheck,
                                   SyntaxSexp spec,
                                   ArrayList<SyntaxValue> expandedSpecs)
        throws FusionException
    {
        SyntaxChecker check = new SyntaxChecker(eval, spec);

        GlobalState globalState = eval.getGlobalState();

        Binding b = spec.firstTargetBinding(eval);
        if (b == globalState.myKernelOnlyInBinding)
        {
            int arity = check.arityAtLeast(2);

            SyntaxValue[] children = spec.extract(eval);
            children[0] = expandRequireSpecNameToPrimitiveImportName(eval, spec, "only");

            // TODO This should visit the module (run its phase-1 code)
            //      but not instantiate it.
            String path = check.requiredText(eval, "module path", 1);
            checkValidModulePath(eval, check, path);

            for (int i = 2; i < arity; i++)
            {
                // "The lexical context of the raw-require-spec determines the
                // context of introduced identifiers.  The exception is the
                // rename sub-form, where the lexical context of the local-id
                // is preserved."
                // https://docs.racket-lang.org/reference/require.html
                //
                // But: Racket expands only_in to rename!

                check.requiredIdentifier(i);
            }

            expandedSpecs.add(SyntaxSexp.make(eval, children));
        }
        else if (b == globalState.myKernelPrefixInBinding)
        {
            check.arityExact(3);

            SyntaxValue[] children = spec.extract(eval);
            children[0] = expandRequireSpecNameToPrimitiveImportName(eval, spec, "prefix");

            children[1] = check.requiredIdentifier("prefix id", 1);

            String path = check.requiredText(eval, "module path", 2);
            checkValidModulePath(eval, check, path);

            expandedSpecs.add(SyntaxSexp.make(eval, children));
        }
        else if (b == globalState.myKernelRenameInBinding)
        {
            int arity = check.arityAtLeast(2);

            SyntaxSymbol renamePrimitiveSymbol =
                    expandRequireSpecNameToPrimitiveImportName(eval, spec, "rename");

            String path = check.requiredText(eval, "module path", 1);
            checkValidModulePath(eval, check, path);

            final Set<String> localIdsSoFar = new HashSet<>();
            for (int i = 2; i < arity; i++)
            {
                SyntaxSexp renamePair = check.requiredSexp("(local_id exported_id)", i);

                SyntaxChecker renamePairChecker = new SyntaxChecker(eval, renamePair);
                renamePairChecker.arityExact(2);

                // In Racket,
                // the rename sub-form has the identifiers in reverse order compared to rename-in.
                SyntaxSymbol exportedId = renamePairChecker.requiredIdentifier("exported_id", 0);
                SyntaxSymbol localId = renamePairChecker.requiredIdentifier("local_id", 1);

                if (!localIdsSoFar.add(localId.stringValue())) {
                    throw check.failure("rename_in pair specified duplicate local_id: " + localId,
                                        spec);
                }
                expandedSpecs.add(SyntaxSexp.make(eval,
                                                  renamePrimitiveSymbol,
                                                  spec.get(eval, 1), // module path
                                                  localId,
                                                  exportedId));
            }
        }
        else
        {
            throw requireCheck.failure("invalid require-spec");
        }
    }

    private SyntaxSymbol expandRequireSpecNameToPrimitiveImportName(Evaluator eval,
                                                                    SyntaxSexp spec,
                                                                    String primitiveImportName)
            throws FusionException
    {
        SyntaxSymbol specId = spec.firstIdentifier(eval);
        return syntaxTrackOrigin(eval,
                                 SyntaxSymbol.make(eval,
                                                   specId.getLocation(),
                                                   makeSymbol(eval, primitiveImportName)),
                                 spec,
                                 specId);
    }

    private void checkValidModulePath(Evaluator     eval,
                                      SyntaxChecker check,
                                      String        path)
        throws SyntaxException
    {
        if (! ModuleIdentity.isValidModulePath(path))
        {
            String message = "invalid module path: " + printString(path);
            throw check.failure(message);
        }
    }


    //========================================================================


    @Override
    void evalCompileTimePart(Compiler comp,
                             TopLevelNamespace topNs,
                             SyntaxSexp topStx)
        throws FusionException
    {
        CompiledForm compiledForm = compile(comp, topNs, topStx);

        // TODO This needs to visit, not instantiate, modules.
        comp.getEvaluator().eval(topNs, compiledForm);
    }


    //========================================================================


    @Override
    CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
        throws FusionException
    {
        Evaluator eval = comp.getEvaluator();

        // We resolve the name at compile-time, noting that for `require`
        // the form is immediately evaluated. I don't want to think about what
        // would happen if resolving at runtime gave a different result.

        ModuleIdentity baseModule = env.namespace().getModuleId();
        int arity = stx.size();

        SyntaxChecker check = new SyntaxChecker(eval, REQUIRE, stx);
        SyntaxSymbol requireSym = (SyntaxSymbol) stx.get(eval, 0);

        CompiledRequireSpec[] compiledSpecs =
            new CompiledRequireSpec[arity - 1];

        int i = 0;
        for (Object p = unsafePairTail(eval, stx.unwrap(eval));
             isPair(eval, p);
             p = unsafePairTail(eval, p), i++)
        {
            SyntaxValue spec = (SyntaxValue) unsafePairHead(eval, p);

            try
            {
                compiledSpecs[i] =
                    compileSpec(eval, env, baseModule, check, requireSym, spec);
            }
            catch (FusionException e)
            {
                e.addContext(spec);
                throw e;
            }
        }

        return new CompiledRequire(compiledSpecs);
    }

    private CompiledRequireSpec compileSpec(Evaluator eval,
                                            Environment env,
                                            ModuleIdentity baseModule,
                                            SyntaxChecker requireCheck,
                                            SyntaxSymbol lexicalContext,
                                            SyntaxValue spec)
        throws FusionException
    {
        if (spec instanceof SyntaxSexp)
        {
            SyntaxSexp sexp = (SyntaxSexp) spec;
            switch (sexp.firstIdentifier(eval).getName().stringValue())
            {
                case "only":
                {
                    ModuleIdentity moduleId =
                        myModuleNameResolver.resolve(eval, baseModule,
                                                     sexp.get(eval, 1), true);

                    int idCount = sexp.size() - 2;
                    RequireRenameMapping[] mappings =
                        new RequireRenameMapping[idCount];
                    for (int i = 0; i < idCount; i++)
                    {
                        SyntaxSymbol id = (SyntaxSymbol) sexp.get(eval, i + 2);
                        mappings[i] =
                            new RequireRenameMapping(id, id.getName());
                    }

                    return new CompiledPartialRequire(moduleId, mappings);
                }
                case "prefix":
                {
                    SyntaxSymbol prefixId = (SyntaxSymbol) sexp.get(eval, 1);

                    ModuleIdentity moduleId =
                            myModuleNameResolver.resolve(eval,
                                                         baseModule,
                                                         sexp.get(eval, 2),
                                                         true);

                    ModuleInstance moduleInstance =
                            env.namespace().getRegistry().instantiate(eval, moduleId);

                    int idCount = moduleInstance.providedBindings().size();
                    RequireRenameMapping[] mappings = new RequireRenameMapping[idCount];
                    int i = 0;
                    for (FusionSymbol.BaseSymbol providedName : moduleInstance.providedNames())
                    {
                        String newBindingName = prefixId.stringValue() + providedName.stringValue();
                        SyntaxSymbol newBindingSymbol = SyntaxSymbol.make(eval, newBindingName);
                        mappings[i] = new RequireRenameMapping(newBindingSymbol, providedName);
                        i++;
                    }
                    return new CompiledPartialRequire(moduleId, mappings);
                }
                case "rename":
                {
                    ModuleIdentity moduleId =
                            myModuleNameResolver.resolve(eval,
                                                         baseModule,
                                                         sexp.get(eval, 1),
                                                         true);
                    SyntaxSymbol localId = (SyntaxSymbol) sexp.get(eval, 2);
                    SyntaxSymbol exportedId = (SyntaxSymbol) sexp.get(eval, 3);
                    RequireRenameMapping[] mappings =
                            { new RequireRenameMapping(localId, exportedId.getName()) };
                    return new CompiledPartialRequire(moduleId, mappings);
                }
                default:
                {
                    throw requireCheck.failure("invalid provide-spec");
                }
            }
        }
        else // It's a (string or symbol) module path.
        {
            ModuleIdentity moduleId =
                myModuleNameResolver.resolve(eval, baseModule, spec, true);
            return new CompiledFullRequire(moduleId, lexicalContext);
        }
    }


    /**
     * This class primarily provides syntax to bind to require-clauses so that
     * {@link RequireForm} can compare bindings, not symbolic names.  This is
     * as specified by Racket.
     */
    private abstract static class AbstractRequireClauseForm
        extends SyntacticForm
    {
        @Override
        SyntaxValue expand(Expander expander, Environment env, SyntaxSexp stx)
            throws FusionException
        {
            throw check(expander, stx).failure("must be used inside `require`");
        }

        @Override
        CompiledForm compile(Compiler comp, Environment env, SyntaxSexp stx)
            throws FusionException
        {
            throw new IllegalStateException("Shouldn't be compiled");
        }
    }


    static final class OnlyInForm
        extends AbstractRequireClauseForm
    {
    }

    static final class PrefixInForm
            extends AbstractRequireClauseForm
    {
    }

    static final class RenameInForm
            extends AbstractRequireClauseForm
    {
    }


    //========================================================================


    private static final class CompiledRequire
        implements CompiledForm
    {
        private final CompiledRequireSpec[] mySpecs;

        private CompiledRequire(CompiledRequireSpec[] specs)
        {
            mySpecs = specs;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            // These forms are evaluated during expansion of a module.
            // So it's never entered with a "real" Store.
            Namespace namespace = (Namespace) store.namespace();

            for (CompiledRequireSpec spec : mySpecs)
            {
                spec.eval(eval, namespace);
            }
            return voidValue(eval);
        }
    }

    private abstract static class CompiledRequireSpec
    {
        protected final ModuleIdentity myUsedModuleId;

        private CompiledRequireSpec(ModuleIdentity usedModuleId)
        {
            myUsedModuleId = usedModuleId;
        }

        abstract void eval(Evaluator eval, Namespace namespace)
            throws FusionException;
    }


    private static final class CompiledFullRequire
        extends CompiledRequireSpec
    {
        private final SyntaxSymbol myLexicalContext;

        private CompiledFullRequire(ModuleIdentity usedModuleId,
                                    SyntaxSymbol lexicalContext)
        {
            super(usedModuleId);
            myLexicalContext = lexicalContext;
        }

        @Override
        public void eval(Evaluator eval, Namespace namespace)
            throws FusionException
        {
            namespace.require(eval, myLexicalContext, myUsedModuleId);
        }
    }


    private static final class CompiledPartialRequire
        extends CompiledRequireSpec
    {
        private final RequireRenameMapping[] myMappings;

        private CompiledPartialRequire(ModuleIdentity usedModuleId,
                                       RequireRenameMapping[] mappings)
        {
            super(usedModuleId);
            myMappings = mappings;
        }

        @Override
        public void eval(Evaluator eval, Namespace namespace)
            throws FusionException
        {
            namespace.require(eval, myUsedModuleId,
                              Arrays.asList(myMappings).iterator());
        }
    }
}
