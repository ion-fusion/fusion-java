// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool.layout;

import dev.ionfusion.fusion._private.HtmlWriter;
import dev.ionfusion.fusion._private.StreamWriter;
import dev.ionfusion.fusion._private.doc.site.Artifact;
import dev.ionfusion.fusion._private.doc.site.ArtifactGenerator;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The base HTML layout for all pages, providing common structure and style.
 *
 * @param <E> the type of entity providing page content.
 */
public abstract class CommonLayout <E>
    extends ArtifactGenerator<E, StreamWriter>
{
    /** HTML content for the masthead links */
    private static final String HEADER_LINKS =
        "<div class='indexlink'>" +
        "<a href='index.html'>Top</a> " +
        "<a href='binding-index.html'>Binding Index</a> " +
        "(<a href='permuted-index.html'>Permuted</a>)" +
        "</div>\n";


    protected CommonLayout(Artifact<E> artifact)
    {
        super(artifact);
    }


    /**
     * Collects the URLs of CSS style sheets to include in the HTML header.
     * <p>
     * This implementation adds the common style sheet.
     * Subclasses that override to call this and add their own.
     */
    void addCssUrls(ArrayList<String> urls)
        throws IOException
    {
        urls.add("common.css");
    }


    /**
     * Gets the unescaped title of this page.
     *
     * @return the title. If null, a default title will be used.
     *
     * @throws IOException if the title cannot be retrieved.
     */
    abstract String getTitle()
        throws IOException;


    /**
     * Writes layout content to the given output stream.
     *
     * @param out where to write content; not null.
     *
     * @throws IOException from the writer.
     */
    abstract void renderContent(StreamWriter out)
        throws IOException;


    @Override
    public final void generate(StreamWriter out)
        throws IOException
    {
        String title = getTitle();
        if (title == null || title.isEmpty()) title = "Ion Fusion Documentation";

        ArrayList<String> cssUrls = new ArrayList<>();
        addCssUrls(cssUrls);

        HtmlWriter writer = new HtmlWriter(out);
        writer.openHtml();
        {
            writer.renderHead(title,
                              getArtifact().getPathToBase().toString(),
                              cssUrls.toArray(new String[0]));

            writer.openBody();
            {
                writer.append(HEADER_LINKS);

                renderContent(out);
            }
            writer.closeBody();
        }
        writer.closeHtml();
    }
}
