// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;

interface NamespaceStore
    extends Store
{
    ModuleRegistry getRegistry();

    /**
     * Provides runtime access to module-level definitions from outside the module.
     */
    Object lookupImport(int moduleAddress, int bindingAddress);

    /**
     * Currently only implemented for modules.
     */
    BaseSymbol getDefinedName(int address);
}
