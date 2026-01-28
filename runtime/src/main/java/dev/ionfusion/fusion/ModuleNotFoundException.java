// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


/**
 * Indicates failure to locate a required module.
 */
@SuppressWarnings("serial")
public final class ModuleNotFoundException
    extends FusionErrorException
{
    ModuleNotFoundException(String message)
    {
        super(message != null ? message : "Module not found");
    }

    /**
     * @param location may be null to indicate an unknown location.
     */
    ModuleNotFoundException(String message, SyntaxValue location)
    {
        this(message);
        if (location != null)
        {
            addContext(location.getLocation());
        }
    }
}
