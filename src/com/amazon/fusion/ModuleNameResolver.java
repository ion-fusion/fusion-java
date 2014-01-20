// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionText.isText;
import static com.amazon.fusion.FusionText.unsafeTextToJavaString;
import static com.amazon.fusion.ModuleIdentity.isValidModulePath;
import static com.amazon.ion.util.IonTextUtils.printString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
    private final Set<ModuleIdentity> myLoadedModuleIds =
        new HashSet<>();

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
     * Asserts that a particular module has already had its declaration loaded
     * in the given registry.
     */
    void registerDeclaredModule(ModuleRegistry registry, ModuleIdentity id)
        throws FusionException
    {
        // TODO FUSION-239 Keep a set per registry
        synchronized (myLoadedModuleIds)
        {
            myLoadedModuleIds.add(id);
        }
    }


    /**
     * Locates and loads a module, dispatching on the concrete syntax of the
     * request.
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
     * @return null if the referenced module couldn't be located in the current
     * registry or any repository.
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
     * Locates and loads a module from the registered repositories.
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

        ModuleIdentity id = ModuleIdentity.forPath(baseModule, modulePath);
        synchronized (myLoadedModuleIds)
        {
            if (myLoadedModuleIds.contains(id)) return id;
        }

        ModuleLocation loc = locate(eval, id, stxForErrors);
        if (loc != null)
        {
            if (load) loadModule(eval, id, loc, false /* don't reload */);
            return id;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("A module named ");
        buf.append(printString(modulePath));
        buf.append(" could not be found in the registered repositories.");
        buf.append(" The repositories are:\n");
        for (ModuleRepository repo : myRepositories)
        {
            buf.append("  * ");
            buf.append(repo.identify());
            buf.append('\n');
        }
        String message = buf.toString();

        if (stxForErrors == null)
        {
            throw new ModuleNotFoundException(message);
        }
        else
        {
            throw new ModuleNotFoundException(message, stxForErrors);
        }
    }


    private void checkForCycles(Evaluator eval, Object moduleId)
        throws FusionException
    {
        ArrayList<Object> marks =
            eval.continuationMarks(MODULE_LOADING_MARK);

        for (int i = 0; i < marks.size(); i++)
        {
            if (marks.get(i).equals(moduleId))
            {
                // Found a cycle!
                StringBuilder message = new StringBuilder();
                message.append("Module dependency cycle detected: ");
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
     * registry.
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
        // TODO FUSION-73 This is probably far too coarse-grained in general.
        synchronized (reg)
        {
            if (reload || ! reg.isLoaded(id))
            {
                Object idString = makeString(eval, id.absolutePath());

                checkForCycles(eval, idString);

                Evaluator loadEval =
                    eval.markedContinuation(new Object[]{ myCurrentModuleDeclareName,
                                                          MODULE_LOADING_MARK },
                                            new Object[]{ idString, idString });
                myLoadHandler.loadModule(loadEval, id, loc);
                // Evaluation of 'module' declares it, but doesn't instantiate.
            }
        }
    }
}
