// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

public final class BindingDoc
{
    public static final BindingDoc[] EMPTY_ARRAY = new BindingDoc[0];

    public enum Kind { PROCEDURE, SYNTAX, CONSTANT }

    private Kind   myKind;
    // TODO one-liner
    // TODO intro
    // TODO pairs of usage/body
    private String myUsage;
    private final String myBody;


    public BindingDoc(Kind kind, String usage, String body)
    {
        myKind = kind;
        myUsage = usage;
        myBody = body;
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
}
