// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


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
     * Attempts to locate a module within this repository, without forcing its
     * loading or instantiation.
     *
     * @return the location of a module with the given identity, or null.
     */
    abstract ModuleLocation locateModule(Evaluator eval, ModuleIdentity id)
        throws FusionException;

    /**
     * Enumerate modules that are visible to this repository.
     * This may not be the entire set of loadable modules! Some repositories
     * may not be able to enumerate their own content, and submodules may not
     * be discovered until their containing module is loaded.
     */
    abstract void collectModules(Predicate<ModuleIdentity> selector,
                                 Consumer<ModuleIdentity>  results)
        throws FusionException;
}
