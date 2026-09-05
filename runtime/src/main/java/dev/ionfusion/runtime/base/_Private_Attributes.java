// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

public final class _Private_Attributes
{
    private _Private_Attributes() {}


    /**
     * {@link ResourceDescriptor} attribute holding the module contained by a resource.
     * It is not guaranteed that the module declaration is the only content present.
     */
    public static final Attribute<ModuleIdentity> MODULE_IDENTITY_ATTRIBUTE =
        Attribute.ofType(ModuleIdentity.class);
}
