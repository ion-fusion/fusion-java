// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static dev.ionfusion.testing.ProjectLayout.testDataDirectory;
import static dev.ionfusion.testing.ProjectLayout.testDataFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.amazon.ion.IonReader;
import com.amazon.ion.system.IonReaderBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class ResourceIdentifierTest
{
    public static final Path HELLO_PATH = testDataFile("hello.ion");

    private void checkPathResource(Path path, ResourceIdentifier rsrc)
    {
        assertEquals(path, rsrc.getPath());
        assertEquals(path.toString(), rsrc.toString());
        assertEquals("file://" + path, rsrc.getUri().toString());
    }

    private static void checkContent(ResourceIdentifier rsrc)
        throws IOException
    {
        try (InputStream in = rsrc.openStream();
             IonReader reader = IonReaderBuilder.standard().build(in))
        {
            reader.next();
            assertEquals("hello", reader.stringValue());
        }
    }


    @Test
    void testPathResources()
        throws Exception
    {
        Path path = testDataFile("hello.ion");

        ResourceIdentifier rsrc = ResourceIdentifier.forFile(path);
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forFile(path.toFile());
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forFile(path.toString());
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forUri(path.toUri());
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forUri(path.toUri().toString());
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forUrl(path.toUri().toURL());
        checkPathResource(path, rsrc);
    }

    @Test
    void testPathNormalization()
    {
        Path dir  = testDataDirectory();
        Path path = testDataFile("hello.ion");
        assertThat(path.toString(), startsWith(dir.toString()));

        Path dotted = dir.resolve("../data/hello.ion");
        assertThat(dotted.toString(), containsString("/../"));

        ResourceIdentifier rsrc = ResourceIdentifier.forFile(dotted);
        checkPathResource(path, rsrc);

        Path cwd      = Paths.get("").toAbsolutePath();
        Path relative = cwd.relativize(path);
        assertFalse(relative.isAbsolute());

        rsrc = ResourceIdentifier.forFile(relative);
        checkPathResource(path, rsrc);

        rsrc = ResourceIdentifier.forFile("");
        // URI adds a slash if the path is a directory
        assertEquals("file://" + cwd + "/", rsrc.getUri().toString());
    }

    @Test
    void testPathReading()
        throws Exception
    {
        ResourceIdentifier rsrc = ResourceIdentifier.forFile(HELLO_PATH);
        checkContent(rsrc);
    }


    @Test
    void testUriFactories()
        throws Exception
    {
        URL url = new URL("https:/bar/baz");
        URI uri = url.toURI();

        ResourceIdentifier rsrc = ResourceIdentifier.forUrl(url);
        assertEquals("https:/bar/baz", rsrc.toString());
        assertNull(rsrc.getPath());

        rsrc = ResourceIdentifier.forUri(uri);
        assertEquals("https:/bar/baz", rsrc.toString());
        assertNull(rsrc.getPath());

        rsrc = ResourceIdentifier.forUri(url.toString());
        assertEquals("https:/bar/baz", rsrc.toString());
        assertNull(rsrc.getPath());
    }
}
