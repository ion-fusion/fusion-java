// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.model;

import static com.amazon.ion.system.IonTextWriterBuilder.UTF8;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the content of a standalone document backed by a Markdown file.
 */
public class MarkdownArticle
{
    private static final String  TITLE_REGEX   = "^#\\s+(\\p{Print}+)\\s*$";
    private static final Pattern TITLE_PATTERN = compile(TITLE_REGEX, MULTILINE);

    private final Path   myMarkdownFile;
    private       String myContent;
    private       String myTitle;

    public MarkdownArticle(Path markdownFile)
    {
        myMarkdownFile = markdownFile;
    }


    public String getContent()
        throws IOException
    {
        if (myContent == null)
        {
            // TODO Java11: use Files.readString
            byte[] bytes = Files.readAllBytes(myMarkdownFile);
            myContent = new String(bytes, UTF8);
        }

        return myContent;
    }

    public String getTitle()
        throws IOException
    {
        if (myTitle == null)
        {
            Matcher matcher = TITLE_PATTERN.matcher(getContent());
            myTitle = (matcher.find() ? matcher.group(1) : "");
        }

        return myTitle;
    }
}
