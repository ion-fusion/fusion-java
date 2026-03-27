// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

import dev.ionfusion.fusioncli.Command;

/**
 * Dummy command that never matches, but display as a separator in help output.
 */
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
