// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.FUSION_SOURCE_EXTENSION;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


final class ModuleDoc
{
    private final StandardRuntime myRuntime;
    private final ModuleIdentity        myModuleId;
    private final ModuleDocs            myModuleDocs;
    private       Map<String,ModuleDoc> mySubmodules;


    public static ModuleDoc buildDocTree(FusionRuntime runtime,
                                         Predicate<ModuleIdentity> filter,
                                         File repoDir)
        throws IOException, FusionException
    {
        ModuleDoc doc = new ModuleDoc((StandardRuntime) runtime);
        doc.addModules(filter, repoDir);
        return doc;
    }


    //========================================================================


    /**
     * Constructs the documentation root as a pseudo-module.
     */
    private ModuleDoc(StandardRuntime runtime)
        throws FusionException
    {
        myRuntime = runtime;
        myModuleId = null;
        myModuleDocs = null;
    }


    /**
     * Constructs docs for a real or implicit top-level module or submodule.
     */
    private ModuleDoc(StandardRuntime runtime,
                      ModuleIdentity id,
                      ModuleDocs docModel)
        throws FusionException
    {
        assert id != null;
        assert docModel != null;

        myRuntime = runtime;
        myModuleId = id;
        myModuleDocs = docModel;
    }


    ModuleIdentity getModuleId()
    {
        return myModuleId;
    }


    String baseName()
    {
        return (myModuleId == null ? null : myModuleId.baseName());
    }


    String submodulePath(String name)
    {
        if (myModuleId == null)
        {
            return "/" + name;
        }

        String parentPath = myModuleId.absolutePath();
        assert parentPath.startsWith("/");

        return parentPath + "/" + name;
    }


    ModuleDocs getModuleDocs()
    {
        return myModuleDocs;
    }

    Map<String, ModuleDoc> submoduleMap()
    {
        return mySubmodules;
    }

    Collection<ModuleDoc> submodules()
    {
        if (mySubmodules == null)
        {
            return Collections.emptySet();
        }
        return mySubmodules.values();
    }


    private ModuleDocs instantiateModuleDocModel(ModuleIdentity id)
        throws FusionException
    {
        ModuleInstance module =
            myRuntime.getDefaultRegistry().instantiate(evaluator(), id);
        if (module == null) return null;

        Set<BaseSymbol> names = module.providedNames();
        Map<String, BindingDoc> bindings =
            (names.isEmpty()
                 ? Collections.emptyMap()
                 : new HashMap<>(names.size()));

        for (BaseSymbol name : names)
        {
            String text = name.stringValue();
            BindingDoc doc = module.documentProvidedName(text);
            bindings.put(text, doc);
        }

        return new ModuleDocs(id, module.getDocs(), bindings);
    }


    /**
     * @return null if the submodule is to be excluded from documentation.
     */
    private ModuleDoc addSubmodule(Predicate<ModuleIdentity> filter,
                                   String name)
        throws FusionException
    {
        ModuleIdentity id;
        try
        {
            id = resolveModulePath(submodulePath(name));
            assert id.baseName().equals(name);
        }
        // FIXME This can happen for modules required by the one requested.
        catch (ModuleNotFoundException e)
        {
            // This can happen for implicit modules, that is, directories with
            // no corresponding .fusion file.
            id = ModuleIdentity.forAbsolutePath(submodulePath(name));
        }

        if (! filter.test(id)) return null;

        ModuleDocs model = instantiateModuleDocModel(id);

        ModuleDoc doc = new ModuleDoc(myRuntime, id, model);

        if (mySubmodules == null)
        {
            mySubmodules = new HashMap<>();
        }

        assert ! mySubmodules.containsKey(name);
        mySubmodules.put(name, doc);

        return doc;
    }


    /**
     * Adds a submodule doc if and only if it doesn't already exist.
     * @return null if the submodule is to be excluded from documentation.
     */
    private ModuleDoc addImplicitSubmodule(Predicate<ModuleIdentity> filter,
                                           String name)
        throws FusionException
    {
        if (mySubmodules != null)
        {
            ModuleDoc doc = mySubmodules.get(name);

            if (doc != null) return doc;
        }

        return addSubmodule(filter, name);
    }


    /**
     * Adds all modules that we can discover within a directory.
     */
    private void addModules(Predicate<ModuleIdentity> filter, File dir)
        throws IOException, FusionException
    {
        String[] fileNames = dir.list();

        // First pass: build all "real" modules
        for (String fileName : fileNames)
        {
            if (fileName.endsWith(FUSION_SOURCE_EXTENSION))
            {
                // We assume that all .fusion files are modules.
                int endIndex =
                    fileName.length() - FUSION_SOURCE_EXTENSION.length();
                String moduleName = fileName.substring(0, endIndex);
                addSubmodule(filter, moduleName);
            }
        }

        // Second pass: look for directories, which are implicitly submodules.
        for (String fileName : fileNames)
        {
            File testFile = new File(dir, fileName);
            if (testFile.isDirectory())
            {
                ModuleDoc d = addImplicitSubmodule(filter, fileName);
                if (d != null)
                {
                    d.addModules(filter, testFile);
                }
            }
        }
    }


    private ModuleIdentity resolveModulePath(String modulePath)
        throws FusionException
    {
        assert modulePath.startsWith("/");

        Evaluator eval = evaluator();
        ModuleNameResolver resolver =
            eval.getGlobalState().myModuleNameResolver;

        return resolver.resolveModulePath(eval,
                                          null,       // baseModule
                                          modulePath,
                                          true,       // load the module
                                          null);      // syntax form for errors
    }


    private Evaluator evaluator()
        throws FusionException
    {
        return myRuntime.getDefaultTopLevel().getEvaluator();
    }
}
