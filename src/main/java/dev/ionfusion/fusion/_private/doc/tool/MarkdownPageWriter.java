// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import static dev.ionfusion.fusion._private.doc.tool.DocGenerator.HEADER_LINKS;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms a single Markdown file into HTML.
 * <p>
 * The page title is taken from the first H1, assumed to be authored using
 * the atx syntax: {@code # <Title content>}.
 */
class MarkdownPageWriter
    extends MarkdownWriter
{
    private static final String  TITLE_REGEX   = "^#\\s+(\\p{Print}+)\\s*$";
    private static final Pattern TITLE_PATTERN = compile(TITLE_REGEX, MULTILINE);


    private final String myBaseUrl;
    private final Path   myMarkdownFile;

    MarkdownPageWriter(Path outputFile, String baseUrl, Path markdownFile)
        throws IOException
    {
        super(outputFile.toFile());
        myBaseUrl = baseUrl;
        myMarkdownFile = markdownFile;
    }


    void render()
        throws IOException
    {
        // TODO Java11: use Files.readString
        byte[] bytes = Files.readAllBytes(myMarkdownFile);
        String markdownContent = new String(bytes, UTF8);

        Matcher matcher = TITLE_PATTERN.matcher(markdownContent);
        String title =
            (matcher.find() ? matcher.group(1) : "Ion Fusion Documentation");

        openHtml();
        {
            renderHead(title, myBaseUrl, "common.css", "doc.css");
            openBody();
            {
                append(HEADER_LINKS);
                markdown(markdownContent);
            }
            closeBody();
        }
        closeHtml();
    }
}
