// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlBody;
import org.htmlunit.html.HtmlHeading1;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlParagraph;
import org.htmlunit.html.parser.HTMLParserListener;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the documentation tree in the distribution.
 */
public class DocumentationTest
{
    /** Not thread-safe */
    @AutoClose
    private final WebClient myWebClient = new WebClient();

    @BeforeEach
    public void initWebClient()
    {
        myWebClient.setIncorrectnessListener((s, o) -> fail("Incorrectness: " + s));

        myWebClient.setHTMLParserListener(new HTMLParserListener()
        {
            @Override
            public void error(String message, URL url, String html,
                              int line, int column, String key)
            {
                fail("HTML error at line " + line + ": " + message);
            }

            @Override
            public void warning(String message, URL url, String html,
                                int line, int column, String key)
            {
                fail("HTML warning at line " + line + ": " + message);
            }
        });
    }


    private HtmlPage loadModule(String modulePath)
        throws IOException
    {
        String   url  = "file:build/install/fusion/docs" + modulePath + ".html";
        HtmlPage page = myWebClient.getPage(url);
        assertEquals(modulePath, page.getTitleText());

        HtmlBody     body    = page.getBody();
        HtmlHeading1 firstH1 = body.getFirstByXPath("h1");
        assertEquals("Module " + modulePath, firstH1.getTextContent());

        return page;
    }


    /**
     * Assumes that we've generated our documentation tree!
     */
    @Test
    public void testFusionDoc()
        throws Exception
    {
        HtmlPage page = loadModule("/fusion");
        HtmlBody body = page.getBody();

        HtmlParagraph p = body.getFirstByXPath("p");
        assertNotNull(p, "missing first <p>");
        assertThat(p.getTextContent(), startsWith("The main Fusion language."));
    }
}
