// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

/**
 * Dummy command that never matches, but display as a separator in help output.
 */
@SuppressWarnings("rawtypes")
public class Separator
    extends Command
{
    public Separator()
    {
        super("-----");
        putHelpText("-------------------------", "-----", "-----");
    }

    @Override
    public boolean matches(String command)
    {
        return false;
    }
}
