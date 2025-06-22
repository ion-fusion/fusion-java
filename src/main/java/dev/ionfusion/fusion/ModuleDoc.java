// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.FUSION_SOURCE_EXTENSION;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


final class ModuleDoc
{
    private final StandardRuntime myRuntime;
    final ModuleIdentity myModuleId;
    final String myIntroDocs;

    private Map<String,ModuleDoc>  mySubmodules;
    private Map<String,BindingDoc> myBindings;


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
        myIntroDocs = null;
    }


    /**
     * Constructs docs for a real or implicit top-level module or submodule.
     */
    private ModuleDoc(StandardRuntime runtime,
                      ModuleIdentity id,
                      String introDocs)
        throws FusionException
    {
        assert id != null;

        myRuntime = runtime;
        myModuleId = id;
        myIntroDocs = introDocs;
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


    String oneLiner()
    {
        if (myIntroDocs == null) return null;

        // TODO pick a better locale?
        BreakIterator breaks = BreakIterator.getSentenceInstance();
        breaks.setText(myIntroDocs);
        int start = breaks.first();
        int end = breaks.next();
        if (end == BreakIterator.DONE) return null;

        return myIntroDocs.substring(start, end);
    }


    Map<String, BindingDoc> bindingMap()
    {
        return myBindings;
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


    private void addBindings(ModuleInstance module)
    {
        Set<BaseSymbol> names = module.providedNames();
        if (names.isEmpty()) return;

        myBindings = new HashMap<>(names.size());

        for (BaseSymbol name : names)
        {
            String text = name.stringValue();
            BindingDoc doc = module.documentProvidedName(text);
            myBindings.put(text, doc);
        }
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
        catch (ModuleNotFoundException e)
        {
            // This can happen for implicit modules with no stub .fusion file.
            id = ModuleIdentity.forAbsolutePath(submodulePath(name));
        }

        if (! filter.test(id)) return null;

        ModuleInstance moduleInstance =
            myRuntime.getDefaultRegistry().instantiate(evaluator(), id);

        ModuleDoc doc;
        if (moduleInstance != null)
        {
            doc = new ModuleDoc(myRuntime, id, moduleInstance.getDocs());
            doc.addBindings(moduleInstance);
        }
        else
        {
            // This is an implicit module with no code.
            doc = new ModuleDoc(myRuntime, id, null);
        }

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
