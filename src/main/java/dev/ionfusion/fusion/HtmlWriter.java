// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

class HtmlWriter
    implements AutoCloseable
{
    private final OutputStream myOutStream;
    private final Writer myOut;

    HtmlWriter(File outputFile)
        throws IOException
    {
        Path outputPath = outputFile.toPath();
        Files.createDirectories(outputPath.getParent());

        myOutStream = Files.newOutputStream(outputPath);

        // FIXME OutputStream will not be closed if this fails:
        myOut = new BufferedWriter(new OutputStreamWriter(myOutStream, UTF8));
    }

    HtmlWriter(File outputDir, String fileName)
        throws IOException
    {
        this(new File(outputDir, fileName));
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
        text = text.replace("'", "&apos;");
        return text;
    }

    final void escape(String text)
        throws IOException
    {
        text = escapeString(text);
        myOut.append(text);
    }


    void openHtml()
        throws IOException
    {
        myOut.append("<!DOCTYPE html>\n" +
                     "<html>\n");
    }

    void closeHtml()
        throws IOException
    {
        myOut.append("</html>\n");
    }

    private void openHead(String title, String baseUrl)
        throws IOException
    {
        myOut.append("<head>" +
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
     * Renders a {@code <head>} block, using the given title and style sheets.
     *
     * @param cssUrls are relative to the {@code baseUrl}; may be empty.
     */
    void renderHead(String title, String baseUrl, String... cssUrls)
        throws IOException
    {
        openHead(title, baseUrl);

        for (String cssUrl : cssUrls)
        {
            myOut.append("<link href='");
            escape(cssUrl);
            myOut.append("' rel='stylesheet' type='text/css'></link>\n");
        }

        myOut.append("</head>\n");
    }


    void openBody()
        throws IOException
    {
        myOut.append("<body>\n");
    }

    void closeBody()
        throws IOException
    {
        myOut.append("</body>\n");
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
