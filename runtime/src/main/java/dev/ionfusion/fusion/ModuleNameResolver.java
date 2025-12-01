// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printString;
import static dev.ionfusion.fusion.FusionSexp.unsafePairHead;
import static dev.ionfusion.fusion.FusionSexp.unsafePairTail;
import static dev.ionfusion.fusion.FusionSexp.unsafeSexpToJavaList;
import static dev.ionfusion.fusion.FusionText.isText;
import static dev.ionfusion.fusion.FusionText.unsafeTextToJavaString;
import static dev.ionfusion.fusion.ModuleIdentity.isValidAbsoluteModulePath;
import static dev.ionfusion.fusion.ModuleIdentity.isValidModulePath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 */
final class ModuleNameResolver
{
    /** Private continuation mark to detect module cycles. */
    private static final Object MODULE_LOADING_MARK =
        new DynamicParameter(null);

    private final LoadHandler myLoadHandler;
    private final DynamicParameter myCurrentLoadRelativeDirectory;
    private final DynamicParameter myCurrentDirectory;
    private final DynamicParameter myCurrentModuleDeclareName;
    private final ModuleRepository[] myRepositories;

    /**
     * Access to this map must be synchronized on it!
     */
    private final WeakHashMap<ModuleRegistry, RegistryCache> myRegistryCache =
        new WeakHashMap<>();

    ModuleNameResolver(LoadHandler loadHandler,
                       DynamicParameter currentLoadRelativeDirectory,
                       DynamicParameter currentDirectory,
                       DynamicParameter currentModuleDeclareName,
                       ModuleRepository[] repositories)
    {
        myLoadHandler = loadHandler;
        myCurrentLoadRelativeDirectory = currentLoadRelativeDirectory;
        myCurrentDirectory = currentDirectory;
        myCurrentModuleDeclareName = currentModuleDeclareName;
        myRepositories = repositories;
    }


    /**
     * Caches various per-registry information.
     */
    private static class RegistryCache
    {
        /**
         * "The standard module name resolver keeps a table per module registry
         * containing loaded module name."
         */
        private final Set<ModuleIdentity> myLoadedModuleIds = new HashSet<>();

        void declare(ModuleIdentity id)
        {
            myLoadedModuleIds.add(id);
        }

        boolean isDeclared(ModuleIdentity id)
        {
            return myLoadedModuleIds.contains(id);
        }
    }

    /**
     * Asserts that a particular module has already had its declaration loaded
     * in the given registry.
     */
    void registerDeclaredModule(ModuleRegistry registry, ModuleIdentity id)
        throws FusionException
    {
        synchronized (myRegistryCache)
        {
            RegistryCache cache = myRegistryCache.get(registry);
            if (cache == null)
            {
                cache = new RegistryCache();
                myRegistryCache.put(registry, cache);
            }
            cache.declare(id);
        }
    }

    /**
     * Check whether we know that a module has been declared in a registry.
     */
    boolean isDeclared(ModuleRegistry registry, ModuleIdentity id)
    {
        synchronized (myRegistryCache)
        {
            RegistryCache cache = myRegistryCache.get(registry);
            return (cache != null && cache.isDeclared(id));
        }
    }

    /**
     * Locates and (optionally) loads a module, dispatching on the concrete
     * syntax of the request. The module is not instantiated.
     * <p>
     * If {@code load} is true, the module will be declared in the current
     * namespace's registry.
     * </p>
     *
     * @param baseModule the starting point for relative references; not null.
     *
     * @throws ModuleNotFoundException if the module could not be found.
     */
    ModuleIdentity resolve(Evaluator      eval,
                           ModuleIdentity baseModule,
                           SyntaxValue    pathStx,
                           boolean        load)
        throws FusionException, ModuleNotFoundException
    {
        Object datum = pathStx.unwrap(eval);
        if (isText(eval, datum))
        {
            String path = unsafeTextToJavaString(eval, datum);
            // TODO check null/empty
            return resolveModulePath(eval, baseModule, path, load, pathStx);
        }

        throw new SyntaxException("module path", "unrecognized form", pathStx);
    }


    /**
     * @return null if the referenced module couldn't be located in any repository.
     */
    private ModuleLocation locate(Evaluator eval,
                                  ModuleIdentity id,
                                  SyntaxValue stxForErrors)
        throws FusionException
    {
        for (ModuleRepository repo : myRepositories)
        {
            ModuleLocation loc = repo.locateModule(eval, id);
            if (loc != null) return loc;
        }

        return null;
    }


    /**
     * Locates and (optionally) loads a module from the registered repositories.
     * The module is not instantiated.
     * <p>
     * If {@code load} is true, the module will be declared in the current
     * namespace's registry.
     * </p>
     *
     * @param eval the evaluation context.
     * @param baseModule the starting point for relative references; not null.
     * @param modulePath must be a module path.
     * @param load should we load the module, or just determine its identity?
     * @param stxForErrors is used for error messaging; may be null.
     *
     * @throws ModuleNotFoundException if the module could not be found.
     */
    ModuleIdentity resolveModulePath(Evaluator eval,
                                     ModuleIdentity baseModule,
                                     String modulePath,
                                     boolean load,
                                     SyntaxValue stxForErrors)
        throws FusionException, ModuleNotFoundException
    {
        if (! isValidModulePath(modulePath))
        {
            String message = "Invalid module path: " + printString(modulePath);
            throw new SyntaxException(null, message, stxForErrors);
        }

        ModuleRegistry reg = eval.findCurrentNamespace().getRegistry();
        ModuleIdentity id = ModuleIdentity.forPath(baseModule, modulePath);

        if (isDeclared(reg, id)) return id;

        ModuleLocation loc = locate(eval, id, stxForErrors);
        if (loc != null)
        {
            if (load) loadModule(eval, id, loc, false /* don't reload */);
            return id;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("A module named ");
        buf.append(printString(modulePath));
        buf.append(" could not be found");
        if (! isValidAbsoluteModulePath(modulePath) && baseModule != null)
        {
            buf.append(" relative to ");
            buf.append(baseModule.absolutePath());
        }
        buf.append('\n');
        buf.append("These repositories were searched:\n");
        for (ModuleRepository repo : myRepositories)
        {
            buf.append("  * ");
            buf.append(repo.identify());
            buf.append('\n');
        }
        String message = buf.toString();

        throw new ModuleNotFoundException(message, stxForErrors);
    }


    private void checkForCycles(Evaluator eval, ModuleIdentity moduleId)
        throws FusionException
    {
        Object loading = eval.continuationMarkSexp(MODULE_LOADING_MARK);
        int i = 0;
        for (Object current = loading;
             FusionSexp.isPair(eval, current);
             current = unsafePairTail(eval, current), i++)
        {
            Object m = unsafePairHead(eval, current);
            if (m.equals(moduleId))
            {
                // Found a cycle!
                StringBuilder message = new StringBuilder();
                message.append("Module dependency cycle detected: ");

                // Make a copy for easier reverse traversal.
                List<Object> marks = unsafeSexpToJavaList(eval, loading);
                assert marks != null;
                for ( ; i >= 0; i--)
                {
                    message.append(marks.get(i));
                    message.append(" -> ");
                }
                message.append(moduleId);

                throw new FusionException(message.toString());
            }
        }
    }


    /**
     * Loads a module from a known location into the current namespace's
     * registry. The module is not instantiated.
     * <p>
     * This method is awkwardly placed in this class, and might make more sense
     * in {@link LoadHandler}.  However, Racket gives this component the task
     * of parameterizing current_module_declare_name and performing cycle
     * detection, and I'm loath to change that without good cause.
     * <p>
     * Also, its unclear how all of this will play out for enclosed submodules
     * like {@code (module Parent ... (module Child ...))}.
     *
     * @param reload indicates whether the module should be reloaded if it's
     * already in the registry.
     */
    void loadModule(Evaluator      eval,
                    ModuleIdentity id,
                    ModuleLocation loc,
                    boolean reload)
        throws FusionException
    {
        ModuleRegistry reg = eval.findCurrentNamespace().getRegistry();

        // Ensure that we don't try to load the module twice simultaneously.
        // TODO This is far too coarse-grained of a lock.
        //  https://github.com/ion-fusion/fusion-java/issues/80
        synchronized (reg)
        {
            if (reload || ! reg.isLoaded(id))
            {
                checkForCycles(eval, id);

                Evaluator loadEval =
                    eval.markedContinuation(new Object[]{ myCurrentModuleDeclareName,
                                                          MODULE_LOADING_MARK },
                                            new Object[]{ id, id });
                myLoadHandler.loadModule(loadEval, id, loc);
                // Evaluation of 'module' declares it, but doesn't instantiate.
                // It also calls-back to registerDeclaredModule().
            }
        }
        assert isDeclared(reg, id);
    }
}
