// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import static dev.ionfusion.fusion._Private_Trampoline.instantiateModuleDocs;
import static dev.ionfusion.fusion._Private_Trampoline.loadModule;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion.ModuleNotFoundException;
import dev.ionfusion.fusion.TopLevel;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public final class DocTreeNode
{
    /**
     * This was copied from {@code GlobalState} due to access constraints.
     * TODO Deduplicate this constant.
     */
    private static final String FUSION_SOURCE_EXTENSION = ".fusion";

    private final TopLevel                 myTopLevel;
    private final ModuleIdentity           myModuleId;
    private final ModuleDocs               myModuleDocs;
    private       Map<String, DocTreeNode> mySubmodules;


    public static DocTreeNode buildDocTree(FusionRuntime runtime,
                                           Predicate<ModuleIdentity> filter,
                                           File repoDir)
        throws IOException, FusionException
    {
        DocTreeNode doc = new DocTreeNode(runtime.makeTopLevel());
        doc.addModules(filter, repoDir);
        return doc;
    }


    //========================================================================


    /**
     * Constructs the documentation root as a pseudo-module.
     */
    private DocTreeNode(TopLevel top)
        throws FusionException
    {
        myTopLevel = top;
        myModuleId = null;
        myModuleDocs = null;
    }


    /**
     * Constructs docs for a real or implicit top-level module or submodule.
     */
    private DocTreeNode(TopLevel       top,
                        ModuleIdentity id,
                        ModuleDocs docModel)
        throws FusionException
    {
        assert id != null;
        assert docModel != null;

        myTopLevel = top;
        myModuleId = id;
        myModuleDocs = docModel;
    }


    public String baseName()
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


    public ModuleDocs getModuleDocs()
    {
        return myModuleDocs;
    }

    public Map<String, DocTreeNode> submoduleMap()
    {
        return mySubmodules;
    }

    public Collection<DocTreeNode> submodules()
    {
        if (mySubmodules == null)
        {
            return Collections.emptySet();
        }
        return mySubmodules.values();
    }


    /**
     * @return null if the submodule is to be excluded from documentation.
     */
    private DocTreeNode addSubmodule(Predicate<ModuleIdentity> filter,
                                     String name)
        throws FusionException
    {
        ModuleIdentity id;
        try
        {
            // This constructs the ModuleIdentity (which we could do manually)
            // and also loads the module in the registry of the top-level.
            id = loadModule(myTopLevel, submodulePath(name));
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

        ModuleDocs model = instantiateModuleDocs(myTopLevel, id);

        DocTreeNode doc = new DocTreeNode(myTopLevel, id, model);

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
    private DocTreeNode addImplicitSubmodule(Predicate<ModuleIdentity> filter,
                                             String name)
        throws FusionException
    {
        if (mySubmodules != null)
        {
            DocTreeNode doc = mySubmodules.get(name);

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
                DocTreeNode d = addImplicitSubmodule(filter, fileName);
                if (d != null)
                {
                    d.addModules(filter, testFile);
                }
            }
        }
    }
}
