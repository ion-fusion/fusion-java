// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import static java.util.Collections.emptyMap;

import dev.ionfusion.fusion.ModuleIdentity;
import java.text.BreakIterator;
import java.util.Map;
import java.util.Set;

/**
 * Documentation of a single module.
 */
public final class ModuleDocs
{
    private final ModuleIdentity          myIdentity;
    private final String                  myOverview;
    private final Map<String, BindingDoc> myBindingDocs;

    public ModuleDocs(ModuleIdentity myIdentity)
    {
        this(myIdentity, null, null);
    }

    public ModuleDocs(ModuleIdentity identity,
                      String overview,
                      Map<String, BindingDoc> bindingDocs)
    {
        assert identity != null;
        myIdentity = identity;
        myOverview = overview;
        myBindingDocs = (bindingDocs == null ? emptyMap() : bindingDocs);
    }


    public ModuleIdentity getIdentity()
    {
        return myIdentity;
    }

    public String getOneLiner()
    {
        if (myOverview == null) return null;

        // TODO pick a better locale?
        BreakIterator breaks = BreakIterator.getSentenceInstance();
        breaks.setText(myOverview);
        int start = breaks.first();
        int end = breaks.next();
        if (end == BreakIterator.DONE) return null;

        return myOverview.substring(start, end);
    }

    /**
     * @return may be null.
     */
    public String getOverview()
    {
        return myOverview;
    }

    public Set<String> getProvidedNames()
    {
        return getBindingDocs().keySet();
    }

    /**
     * @return not null.
     */
    public Map<String, BindingDoc> getBindingDocs()
    {
        return myBindingDocs;
    }
}
