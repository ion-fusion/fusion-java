// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;

interface NamespaceStore
    extends Store
{
    ModuleRegistry getRegistry();

    ModuleStore lookupRequiredModule(int moduleAddress);

    Object lookupImport(int moduleAddress, int bindingAddress);

    /**
     * Currently only implemented for modules.
     */
    BaseSymbol getDefinedName(int address);
}
