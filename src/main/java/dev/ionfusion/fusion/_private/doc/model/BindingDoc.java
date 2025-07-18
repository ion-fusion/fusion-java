// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import dev.ionfusion.fusion.ModuleIdentity;
import java.util.HashSet;
import java.util.Set;


public final class BindingDoc
{
    public static final BindingDoc[] EMPTY_ARRAY = new BindingDoc[0];

    public enum Kind { PROCEDURE, SYNTAX, CONSTANT }

    private String myName;
    private Kind   myKind;
    // TODO one-liner
    // TODO intro
    // TODO pairs of usage/body
    private String myUsage;
    private final String myBody;
    private final HashSet<ModuleIdentity> myProvidingModules = new HashSet<>();


    public BindingDoc(String name, Kind kind, String usage, String body)
    {
        myName = name;
        myKind = kind;
        myUsage = usage;
        myBody = body;
    }


    public String getName()
    {
        return myName;
    }

    public void setName(String name)
    {
        assert myName == null;
        myName = name;
    }


    public Kind getKind()
    {
        return myKind;
    }

    public void setKind(Kind kind)
    {
        assert myKind == null;
        myKind = kind;
    }


    public String getUsage()
    {
        if (myUsage != null
            && ! (myUsage.startsWith("(") && myUsage.endsWith(")")))
        {
            StringBuilder buf = new StringBuilder();
            buf.append('(');
            buf.append(myName == null ? "_" : myName);
            if (! myUsage.isEmpty())
            {
                if (! myUsage.startsWith(" ")) buf.append(' ');
                buf.append(myUsage);
            }
            buf.append(')');

            myUsage = buf.toString();
        }

        return myUsage;
    }

    void setUsage(String usage)
    {
        assert myUsage == null;
        myUsage = usage;
    }


    public String getBody()
    {
        return myBody;
    }


    public Set<ModuleIdentity> getProvidingModules()
    {
        return myProvidingModules;
    }

    public void addProvidingModule(ModuleIdentity id)
    {
        myProvidingModules.add(id);
    }
}
