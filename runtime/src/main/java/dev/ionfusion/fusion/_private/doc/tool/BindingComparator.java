// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import java.util.Comparator;

/**
 * Custom comparator to hide ugly #% bindings down at the bottom of binding lists. Otherwise, they
 * tend to show up early, which is silly since most people don't care about them and shouldn't use
 * them.
 */
final class BindingComparator
    implements Comparator<String>
{
    @Override
    public int compare(String arg0, String arg1)
    {
        if (arg0.startsWith("#%"))
        {
            if (!arg1.startsWith("#%"))
            {
                return 1;
            }
        }
        else if (arg1.startsWith("#%"))
        {
            return -1;
        }

        return arg0.compareTo(arg1);
    }
}
