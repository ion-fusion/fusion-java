// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion._Private_Trampoline.findBindingDoc;
import static dev.ionfusion.fusion._Private_Trampoline.instantiateModuleDocs;
import static dev.ionfusion.fusion._Private_Trampoline.setDocumenting;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.ionfusion.runtime._private.doc.BindingDoc;
import dev.ionfusion.runtime._private.doc.ModuleDocs;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DocumentationAccessTest
    extends CoreTestCase
{
    @BeforeEach
    public void setup()
        throws Exception
    {
        setDocumenting(runtimeBuilder(), true);
        useTstRepo();

        topLevel().requireModule("/fusion/private/syntax");
        topLevel().requireModule("/documentation");
    }


    private ModuleDocs moduleDocs(String modulePath)
        throws FusionException
    {
        ModuleIdentity modId = ModuleIdentity.forAbsolutePath(modulePath);

        ModuleDocs doc = instantiateModuleDocs(topLevel(), modId);
        if (doc != null)
        {
            assertSame(doc.getIdentity(), modId);
        }

        return doc;
    }

    private BindingDoc bindingDoc(String name)
        throws FusionException
    {
        Object id = topLevel().eval("(quote_syntax " + name + ")");
        return findBindingDoc(topLevel(), id);
    }


    @Test
    public void docForUnknownModule()
        throws FusionException
    {
        assertNull(moduleDocs("/unknown/module/path"));
    }


    @Test
    public void docForModule()
        throws FusionException
    {
        ModuleDocs doc = moduleDocs("/documentation");

        assertThat(doc.getOneLiner(), is("These is the module body doc."));
        assertThat(doc.getOverview(), is("These is the module body doc."));

        assertThat(doc.getProvidedNames(),
                   containsInAnyOrder("const", "macro", "proc"));


        BindingDoc constDoc = doc.getBindingDocs().get("const");

        assertThat(constDoc.getKind(), is(BindingDoc.Kind.CONSTANT));
        assertThat(constDoc.getBody(), is("This is the const doc."));
    }


    @Test
    public void docForUnboundIdentifier()
        throws FusionException
    {
        assertNull(bindingDoc("unbound_var"));
    }

    @Test
    public void docForConstant()
        throws FusionException
    {
        BindingDoc doc = bindingDoc("const");

        assertThat(doc.getKind(), is(BindingDoc.Kind.CONSTANT));
        assertThat(doc.getBody(), is("This is the const doc."));
    }

    @Test
    public void docForProcedure()
        throws FusionException
    {
        BindingDoc doc = bindingDoc("proc");

        assertThat(doc.getKind(), is(BindingDoc.Kind.PROCEDURE));
        assertThat(doc.getBody(), containsString("(proc)"));
        assertThat(doc.getBody(), containsString("This is the proc doc."));
    }

    @Test
    public void docForMacro()
        throws FusionException
    {
        BindingDoc doc = bindingDoc("macro");

        assertThat(doc.getKind(), is(BindingDoc.Kind.SYNTAX));
        assertThat(doc.getBody(), is("This is the macro doc."));
    }
}
