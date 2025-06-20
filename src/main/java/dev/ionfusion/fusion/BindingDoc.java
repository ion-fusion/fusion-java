// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.util.HashSet;
import java.util.Set;


final class BindingDoc
{
    static final BindingDoc[] EMPTY_ARRAY = new BindingDoc[0];

    enum Kind { PROCEDURE, SYNTAX, CONSTANT }

    private String myName;
    private Kind   myKind;
    // TODO one-liner
    // TODO intro
    // TODO pairs of usage/body
    private String myUsage;
    private final String myBody;
    private final HashSet<ModuleIdentity> myProvidingModules = new HashSet<>();


    BindingDoc(String name, Kind kind, String usage, String body)
    {
        myName = name;
        myKind = kind;
        myUsage = usage;
        myBody = body;
    }


    String getName()
    {
        return myName;
    }

    void setName(String name)
    {
        assert myName == null;
        myName = name;
    }


    Kind getKind()
    {
        return myKind;
    }

    void setKind(Kind kind)
    {
        assert myKind == null;
        myKind = kind;
    }


    String getUsage()
    {
        if (myUsage != null
            && ! (myUsage.startsWith("(") && myUsage.endsWith(")")))
        {
            StringBuilder buf = new StringBuilder();
            buf.append('(');
            buf.append(myName == null ? "_" : myName);
            if (myUsage.length() != 0)
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


    String getBody()
    {
        return myBody;
    }


    Set<ModuleIdentity> getProvidingModules()
    {
        return myProvidingModules;
    }

    void addProvidingModule(ModuleIdentity id)
    {
        myProvidingModules.add(id);
    }
}
