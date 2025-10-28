// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class PermutedIndex
    implements Iterable<PermutedIndex.PermutedEntry>
{
    private final TreeSet<PermutedEntry> myEntries = new TreeSet<>();

    @Override
    public Iterator<PermutedEntry> iterator()
    {
        return myEntries.iterator();
    }


    /**
     * An entry in the permuted index.
     */
    public static final class PermutedEntry
        implements Comparable<PermutedEntry>
    {
        private final String                     myName;
        private final String                     myPrefix;
        private final String                     myKeyword;
        private final SortedSet<ExportedBinding> myExports;


        PermutedEntry(String name, SortedSet<ExportedBinding> exports,
                      int keywordStartPos, int keywordLimitPos)
        {
            myName = name;
            myPrefix = name.substring(0, keywordStartPos);
            myKeyword = name.substring(keywordStartPos, keywordLimitPos);
            myExports = exports;
        }

        public String bindingName()
        {
            return myName;
        }

        public String prefix()
        {
            return myPrefix;
        }

        public String keyword()
        {
            return myKeyword;
        }

        public String suffix()
        {
            int pos = myPrefix.length() + myKeyword.length();
            return myName.substring(pos);
        }

        public SortedSet<ExportedBinding> exports()
        {
            return myExports;
        }

        @Override
        public int compareTo(PermutedEntry that)
        {
            int result = myKeyword.compareTo(that.myKeyword);
            if (result == 0)
            {
                result = myPrefix.compareTo(that.myPrefix);
                if (result == 0)
                {
                    // We shouldn't get this far often, so we spend time to
                    // get the suffix rather than memory to cache it.
                    result = suffix().compareTo(that.suffix());
                }
            }
            return result;
        }
    }


    public void addEntries(String name, SortedSet<ExportedBinding> exports)
    {
        int pos = 0;
        while (true)
        {
            int underscorePos = name.indexOf('_', pos);
            if (underscorePos == -1)
            {
                myEntries.add(new PermutedEntry(name, exports, pos, name.length()));
                break;
            }
            else if (pos < underscorePos)
            {
                myEntries.add(new PermutedEntry(name, exports, pos, underscorePos));
            }
            pos = underscorePos + 1;
        }
    }
}
