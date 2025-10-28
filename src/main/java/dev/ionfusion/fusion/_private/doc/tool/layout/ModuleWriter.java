// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import static java.util.stream.Collectors.toList;

import dev.ionfusion.fusion.ModuleIdentity;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.model.BindingDoc;
import dev.ionfusion.fusion._private.doc.model.ModuleDocs;
import dev.ionfusion.fusion._private.doc.tool.ExportedBinding;
import dev.ionfusion.fusion._private.doc.tool.MarkdownWriter;
import dev.ionfusion.fusion._private.doc.tool.ModuleEntity;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class ModuleWriter
    extends MarkdownWriter
{
    private final ModuleEntity              myModuleEntity;
    private final ModuleDocs                myModuleDocs;
    private final ModuleIdentity            myModuleId;

    public ModuleWriter(StreamWriter out, ModuleEntity module)
    {
        super(out);
        myModuleEntity = module;
        myModuleDocs = myModuleEntity.getModuleDocs();
        myModuleId = myModuleEntity.getIdentity();
    }


    void renderModule()
        throws IOException
    {
        renderHeader();
        renderModuleIntro();
        renderSubmoduleLinks();
        renderBindings();
    }


    private void renderModulePathWithLinks(ModuleIdentity id)
        throws IOException
    {
        ModuleIdentity parent = id.parent();
        if (parent != null)
        {
            renderModulePathWithLinks(parent);
        }

        append('/');

        String baseName = id.baseName();

        if (id == myModuleId)
        {
            // Don't link to ourselves, that's silly.
            append(baseName);
        }
        else
        {
            linkToModule(id, baseName);
        }
    }

    private void renderHeader()
        throws IOException
    {
        append("<h1>Module ");
        renderModulePathWithLinks(myModuleId);
        append("</h1>");
    }

    private void renderModuleIntro()
        throws IOException
    {
        String overview = myModuleDocs.getOverview();
        if (overview != null)
        {
            markdown(overview);
        }
    }

    private void renderSubmoduleLinks()
        throws IOException
    {
        Set<String> submoduleNames = myModuleEntity.getChildNames();
        if (submoduleNames.isEmpty()) return;

        renderHeader2("Submodules");

        List<String> names = submoduleNames.stream().sorted().collect(toList());

        append("<ul class='submodules'>");
        for (String name : names)
        {
            ModuleEntity module = myModuleEntity.getChild(name);

            String escapedName = escapeString(name);
            append("<li>");
            linkToModule(module.getIdentity(), escapedName);

            String oneLiner = module.getModuleDocs().getOneLiner();
            if (oneLiner != null)
            {
                append(" &ndash; <span class='oneliner'>");
                markdown(oneLiner);
                append("</span>");
            }
            append("</li>\n");
        }
        append("</ul>\n");
    }


    private void renderBindingIndex(Collection<ExportedBinding> exports)
        throws IOException
    {
        if (exports.isEmpty()) { return; }

        append("<div class='exports'>\n");
        for (ExportedBinding export : exports)
        {
            String escapedName = escapeString(export.getName());
            linkToBindingAsName(myModuleId, escapedName);
            append("&nbsp;&nbsp;\n");
        }
        append("</div>\n");
    }


    private void renderBindings()
        throws IOException
    {
        Collection<ExportedBinding> exports = myModuleEntity.exports();
        if (exports.isEmpty()) { return; }

        renderHeader2("Exported Bindings");

        renderBindingIndex(exports);

        for (ExportedBinding export: exports)
        {
            renderBinding(export);
        }
    }


    /* CSS hierarchy:
     *
     *  binding
     *    name
     *    kind
     *    doc
     *      oneliner -- presently unused, intended to be text description
     *      body
     *      also
     */
    private void renderBinding(ExportedBinding export)
        throws IOException
    {
        String name = export.getName();
        BindingDoc doc = export.getDocs();

        String escapedName = escapeString(name);

        append("\n<div class='binding' id='");
        append(escapedName);
        append("'><span class='name'>");
        append(escapedName);
        // FIXME The </a> below is spurious, but unit tests don't flag it.
        append("</a></span>");   // binding div is still open

        if (doc == null)
        {
            append("<p class='nodoc'>No documentation available.</p>\n\n");
        }
        else
        {
            if (doc.getKind() != null)
            {
                append(" <span class='kind'>");
                // Using enum toString() allows display name to be changed
                append(doc.getKind().toString().toLowerCase());
                append("</span>\n");
            }

            append("<div class='doc'>");

            if (doc.getUsage() != null || doc.getBody() != null)
            {
                StringBuilder buf = new StringBuilder();

                if (doc.getUsage() != null)
                {
                    buf.append("    ");
                    buf.append(doc.getUsage());
                    buf.append('\n');
                }

                if (doc.getBody() != null)
                {
                    buf.append('\n');
                    buf.append(doc.getBody());
                    buf.append('\n');
                }

                append("<div class='body'>");
                markdown(buf.toString());
                append("</div>\n");
            }

            append('\n');

            Iterator<ExportedBinding> others = export.alsoProvidedBy().iterator();
            if (others.hasNext())
            {
                append("<p class='also'>Also provided by ");
                while (others.hasNext())
                {
                    ExportedBinding other = others.next();
                    linkToBindingAsModulePath(other.getModuleId(), escapedName);
                    if (others.hasNext())
                    {
                        append(", ");
                    }
                }
                append("</p>\n");
            }

            append("</div>\n"); // doc
        }

        append("</div>\n"); // binding
    }
}
