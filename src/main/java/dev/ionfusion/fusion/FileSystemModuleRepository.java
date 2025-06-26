// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.FUSION_SOURCE_EXTENSION;
import static dev.ionfusion.fusion.ModuleIdentity.isValidModuleName;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Predicate;


final class FileSystemModuleRepository
    extends ModuleRepository
{
    private final File myRepoDir;
    private final File mySrcDir;

    /**
     * @param repoDir is converted to absolute if it's not already.
     */
    FileSystemModuleRepository(File repoDir)
    {
        myRepoDir = repoDir.getAbsoluteFile();

        File src = new File(repoDir, "src");
        mySrcDir = (src.isDirectory() ? src : null);
    }


    @Override
    String identify()
    {
        return myRepoDir.getPath();
    }


    @Override
    ModuleLocation locateModule(Evaluator eval, final ModuleIdentity id)
    {
        if (mySrcDir == null) return null;

        String path = id.absolutePath();
        String fileName = path.substring(1) + FUSION_SOURCE_EXTENSION;

        final File libFile = new File(mySrcDir, fileName);
        if (libFile.exists())
        {
            return ModuleLocation.forFile(id, libFile);
        }

        return null;
    }

    @Override
    public String toString()
    {
        return "[FileSystemModuleRepository " + identify() + ']';
    }


    private void collectModules(Predicate<ModuleIdentity> selector,
                                ModuleIdentity            parentId,
                                File                      parentDir,
                                Consumer<ModuleIdentity>  results)
    {
        String[] fileNames = parentDir.list();

        // First pass: build all "real" modules
        for (String fileName : fileNames)
        {
            if (fileName.endsWith(FUSION_SOURCE_EXTENSION))
            {
                // We assume that all .fusion files are modules.
                int endIndex =
                    fileName.length() - FUSION_SOURCE_EXTENSION.length();
                String moduleName = fileName.substring(0, endIndex);
                if (isValidModuleName(moduleName))
                {
                    ModuleIdentity child =
                        ModuleIdentity.forChild(parentId, moduleName);
                    if (selector.test(child))
                    {
                        results.accept(child);
                    }
                }
            }
        }

        // Second pass: look for directories, which are implicitly submodules.
        for (String fileName : fileNames)
        {
            File testFile = new File(parentDir, fileName);
            if (testFile.isDirectory() && isValidModuleName(fileName))
            {
                ModuleIdentity child =
                    ModuleIdentity.forChild(parentId, fileName);
                if (selector.test(child))
                {
                    // We don't add this to the results set because it may not
                    // have an associated .fusion file.
                    collectModules(selector, child, testFile, results);
                }
            }
        }

        // TODO I don't think this needs to be two passes
    }


    @Override
    void collectModules(Predicate<ModuleIdentity> selector,
                        Consumer<ModuleIdentity>  results)
        throws FusionException
    {
        if (mySrcDir != null)
        {
            collectModules(selector, null, mySrcDir, results);
        }
    }
}
