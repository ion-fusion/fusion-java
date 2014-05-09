// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

class HtmlWriter
    implements AutoCloseable
{
    private final OutputStream myOutStream;
    private final Writer myOut;

    HtmlWriter(File outputFile)
        throws IOException
    {
        FusionUtils.createParentDirs(outputFile);

        myOutStream = new FileOutputStream(outputFile);

        myOut = new BufferedWriter(new OutputStreamWriter(myOutStream, UTF8));
    }

    @Override
    public void close()
        throws IOException
    {
        myOut.close();
    }



    /**
     * Writes UTF-8 bytes, escaping HTML as necessary.
     */
    final void write(byte[] buffer, int off, int len)
        throws IOException
    {
        myOut.flush();

        int end = off + len;
        int curr_start = off;
        for (int i = off; i < end; ++i) {
            char c = (char) buffer[i];
            if (c == '&' || c == '<' || c == '>')
            {
                myOutStream.write(buffer, curr_start, i - curr_start);

                switch (c) {
                    case '&': append("&amp;"); break;
                    case '<': append("&lt;");  break;
                    case '>': append("&gt;");  break;
                }
                myOut.flush();

                curr_start = i + 1;
            }
        }
        myOutStream.write(buffer, curr_start, end - curr_start);
    }


    final void append(char escapedContent)
        throws IOException
    {
        myOut.append(escapedContent);
    }

    final void append(String escapedContent)
        throws IOException
    {
        myOut.append(escapedContent);
    }


    final String escapeString(String text)
        throws IOException
    {
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        text = text.replace("\"", "&quot;");
        text = text.replace("\'", "&apos;");
        return text;
    }

    final void escape(String text)
        throws IOException
    {
        text = escapeString(text);
        myOut.append(text);
    }


    private void openHead(String title, String baseUrl)
        throws IOException
    {
        myOut.append("<!DOCTYPE html>\n" +
                     "<head>" +
                     "<meta http-equiv='Content-Type'" +
                     " content='text/html; charset=utf-8'>");

        if (baseUrl != null)
        {
            myOut.append("<base href='");
            escape(baseUrl);
            myOut.append("'>");
        }

        myOut.append("<title>");
        escape(title);
        myOut.append("</title>\n");
    }


    void renderHeadWithInlineCss(String title, String css)
        throws IOException
    {
        openHead(title, null);

        myOut.append("<style type='text/css'><!--\n");
        myOut.append(css);
        myOut.append("\n--></style>");

        myOut.append("</head>\n");
    }


    /**
     * @param style may be null
     */
    void renderHead(String title, String baseUrl, String style)
        throws IOException
    {
        openHead(title, baseUrl);

        if (style != null)
        {
            myOut.append("<link href='");
            escape(style);
            myOut.append("' rel='stylesheet' type='text/css'></link>\n");
        }
        
        myOut.append("</head>\n");
    }


    final void renderHeader1(String text)
        throws IOException
    {
        myOut.append("<h1>");
        escape(text);
        myOut.append("</h1>\n");
    }

    final void renderHeader2(String text)
        throws IOException
    {
        myOut.append("<h2>");
        escape(text);
        myOut.append("</h2>\n");
    }


    /**
     * Renders a link to a module, using the given link text.
     */
    final void linkToModule(ModuleIdentity id, String escapedLinkText)
        throws IOException
    {
        String escapedId = escapeString(id.absolutePath());

        append("<a href='.");
        append(escapedId);     // starts with a slash
        append(".html'>");
        append(escapedLinkText);
        append("</a>");
    }


    /**
     * Renders a link to a binding in a module, using the binding name
     * as the link text.
     */
    final void linkToBindingAsName(ModuleIdentity id,
                                   String escapedName)
        throws IOException
    {
        String escapedId = escapeString(id.absolutePath());

        append("<a href='.");
        append(escapedId);     // starts with a slash
        append(".html#");
        append(escapedName);
        append("'><code>");
        append(escapedName);
        append("</code></a>");
    }


    /**
     * Renders a link to a binding in a module, using the full module path
     * as the link text.
     */
    final void linkToBindingAsModulePath(ModuleIdentity id,
                                         String escapedName)
        throws IOException
    {
        String escapedId = escapeString(id.absolutePath());

        append("<a href='.");
        append(escapedId);     // starts with a slash
        append(".html#");
        append(escapedName);
        append("'>");
        append(escapedId);
        append("</a>");
    }
}
