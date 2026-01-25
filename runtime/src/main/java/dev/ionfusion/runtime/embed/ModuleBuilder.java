// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.embed;

import dev.ionfusion.fusion.FusionException;

/**
 * Constructs "built-in" modules via Java code.
 * <p>
 * Applications should use this with great discretion and hesitation, since
 * it leads to non-portable Fusion code.
 * <p>
 * To create a {@link ModuleBuilder}, use
 * {@link FusionRuntime#makeModuleBuilder(String)}.
 * <p>
 * <b>WARNING:</b> This interface should not be implemented or extended by
 * code outside of this library.
 */
public interface ModuleBuilder
{
    /**
     * Creates or alters a top-level definition in the module to be built.
     *
     * @param name must be non-empty.
     * @param value must be acceptable to the Fusion runtime.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    void define(String name, Object value)
        throws FusionException;


    /**
     * Instantiates the module for use from Fusion code.
     * <p>
     * This builder must be discarded after this method is called.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    void instantiate()
        throws FusionException;
}
