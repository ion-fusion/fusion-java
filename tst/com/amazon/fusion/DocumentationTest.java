// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlHeading1;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DocumentationTest
{
    private WebClient myWebClient;

    @BeforeEach
    public void initWebClient()
    {
        myWebClient = new WebClient();

        myWebClient.setIncorrectnessListener(new IncorrectnessListener()
        {
            @Override
            public void notify(String s, Object o)
            {
                fail("Incorrectness: " + s);
            }
        });

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
                // TODO Enable this and fix warnings.
//              fail("HTML warning at line " + line + ": " + message);
            }
        });
    }

    @AfterEach
    public void tearDown()
    {
        myWebClient = null;
    }

    private HtmlPage loadModule(String modulePath)
        throws IOException
    {
        String   url  = "file:build/docs/fusiondoc" + modulePath + ".html";
        HtmlPage page = myWebClient.getPage(url);
        assertEquals(modulePath, page.getTitleText());

        HtmlElement  body    = page.getBody();
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
        HtmlPage    page = loadModule("/fusion");
        HtmlElement body = page.getBody();

        HtmlElement p = body.getFirstByXPath("p");
        assertNotNull(p, "missing first <p>");
        assertThat(p.getTextContent(), startsWith("The main Fusion language."));
    }
}
