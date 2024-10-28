// Copyright (c) 2012-2020 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import static com.amazon.fusion.GlobalState.FUSION_SOURCE_EXTENSION;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


final class ModuleDoc
{
    private final StandardRuntime myRuntime;
    final ModuleIdentity myModuleId;
    final String myIntroDocs;

    private Map<String,ModuleDoc>  mySubmodules;
    private Map<String,BindingDoc> myBindings;


    public static ModuleDoc buildDocTree(FusionRuntime runtime, Filter filter,
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

    String[] sortedExportedNames()
    {
        String[] names = EMPTY_STRING_ARRAY;
        if (myBindings != null)
        {
            names = myBindings.keySet().toArray(EMPTY_STRING_ARRAY);
            Arrays.sort(names, new BindingComparator());
        }
        return names;
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
        if (names.size() == 0) return;

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
    private ModuleDoc addSubmodule(Filter filter, String name)
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

        if (! filter.accept(id)) return null;

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
    private ModuleDoc addImplicitSubmodule(Filter filter, String name)
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
    private void addModules(Filter filter, File dir)
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

    //========================================================================


    static final class Filter
    {
        boolean accept(ModuleIdentity id)
        {
            String path = id.absolutePath();
            if (path.endsWith("/private")) return false;
            if (path.contains("/private/")) return false;
            return true;
        }
    }


    /**
     * Customer comparator to hide ugly #% bindings down at the bottom of
     * binding lists. Otherwise they tend to show up early, which is silly
     * since most people don't care about them and shouldn't use them.
     */
    private static final class BindingComparator
        implements Comparator<String>
    {
        @Override
        public int compare(String arg0, String arg1)
        {
            if (arg0.startsWith("#%"))
            {
                if (! arg1.startsWith("#%"))
                {
                    return 1;
                }
            }
            else if (arg1.startsWith("#%"))
            {
                return -1;
            }

            return arg0.compareTo(arg1);
        }
    }
}
