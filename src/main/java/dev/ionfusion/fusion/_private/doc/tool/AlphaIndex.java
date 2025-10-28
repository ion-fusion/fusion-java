// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class AlphaIndex
    implements Iterable<AlphaIndex.AlphaEntry>
{
    private final TreeSet<AlphaEntry> myEntries;

    public AlphaIndex()
    {
        myEntries = new TreeSet<>();
    }

    @Override
    public Iterator<AlphaEntry> iterator()
    {
        return myEntries.iterator();
    }


    /**
     * An entry in the alphabetical index.
     */
    public static final class AlphaEntry
        implements Comparable<AlphaEntry>
    {
        private final String                     myName;
        private final SortedSet<ExportedBinding> myExports;

        AlphaEntry(String name, SortedSet<ExportedBinding> exports)
        {
            myName = name;
            myExports = exports;
        }

        public String bindingName()
        {
            return myName;
        }

        public SortedSet<ExportedBinding> exports()
        {
            return myExports;
        }

        @Override
        public int compareTo(AlphaEntry that)
        {
            return myName.compareTo(that.myName);
        }
    }


    public void addEntry(String name, SortedSet<ExportedBinding> exports)
    {
        myEntries.add(new AlphaEntry(name, exports));
    }
}
