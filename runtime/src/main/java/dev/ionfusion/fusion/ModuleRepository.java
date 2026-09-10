// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.ResourceDescriptor;
import dev.ionfusion.runtime.base.ResourceIdentifier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Provides access to module source code in some (usually persistent) store.
 * <p>
 * NOT FOR APPLICATION USE.
 */
abstract class ModuleRepository
{
    /**
     * Returns human-readable text identifying this repository.
     *
     * @return not null.
     */
    abstract String identify();

    /**
     * Attempts to locate the source code for a module within this repository without
     * loading or instantiating it.
     * <p>
     * A non-null result must have a {@link ResourceIdentifier}.
     *
     * @return a descriptor of the resource containing the module's code, or null.
     */
    abstract ResourceDescriptor locateModule(Evaluator eval, ModuleIdentity id)
        throws FusionException;

    /**
     * Collects the identities of the modules visible to this repository.
     * This may not be the entire set of loadable modules! Some repositories
     * may not be able to enumerate their own content, and submodules may not
     * be discovered until their containing module is loaded.
     */
    abstract void collectModules(Predicate<ModuleIdentity> selector,
                                 Consumer<ModuleIdentity>  results)
        throws FusionException;
}
