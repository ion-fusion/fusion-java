// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.embed.TopLevel;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * NOT FOR APPLICATION USE
 */
public class _Private_Trampoline
{
    private _Private_Trampoline() {}


    public static void setDocumenting(FusionRuntimeBuilder rb,
                                      boolean documenting)
    {
        rb.setDocumenting(documenting);
    }


    public static void discoverModulesInRepository(Path repoDir,
                                                   Predicate<ModuleIdentity> selector,
                                                   Consumer<ModuleIdentity> results)
        throws FusionException
    {
        ModuleRepository repo = new FileSystemModuleRepository(repoDir.toFile());
        repo.collectModules(selector, results);
    }


    public static ModuleIdentity loadModule(TopLevel top, String modulePath)
        throws FusionInterruptedException, FusionException
    {
        return ((StandardTopLevel) top).loadModule(modulePath);
    }


    /**
     * Instantiates a previously loaded module and returns its documentation.
     *
     * @return null if the module isn't known in the top-level's registry.
     */
    public static ModuleDocs instantiateModuleDocs(TopLevel top, ModuleIdentity id)
        throws FusionException
    {
        ModuleInstance moduleInstance = ((StandardTopLevel) top).instantiateLoadedModule(id);
        return (moduleInstance == null ? null : moduleInstance.getDocs());
    }


    public static FusionException newFusionException(String message,
                                                     Throwable cause)
    {
        return new FusionException(message, cause);
    }
}
