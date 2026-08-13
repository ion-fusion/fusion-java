// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

/**
 * Captures information about a specific piece of Fusion code, adding semantic context
 * a position.
 */
public interface CodePosition
    extends ResourcePosition
{
    /**
     * Returns the module containing this position, if any.
     * <p>
     * It is not guaranteed that the module declaration is the only content of the
     * resource: it could be a script with several modules inside, and module
     * declarations will eventually nest.
     *
     * @return the module associated with this position, if any. Can be null.
     */
    ModuleIdentity getModuleIdentity();
}
