// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion._private.doc.model.ModuleDocs;

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
