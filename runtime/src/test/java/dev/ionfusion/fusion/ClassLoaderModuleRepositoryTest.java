// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.read;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.StandardReader.openIonReader;
import static dev.ionfusion.testing.ProjectLayout.PROJECT_DIRECTORY;
import static dev.ionfusion.testing.ProjectLayout.testRepositoryDirectory;
import static java.nio.file.Files.isRegularFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.ResourceDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ClassLoaderModuleRepositoryTest
    extends CoreTestCase
{

    private ModuleLocation locateModule(ModuleRepository repo, String modulePath)
        throws Exception
    {
        ModuleIdentity id = ModuleIdentity.forAbsolutePath(modulePath);

        return repo.locateModule(evaluator(), id);
    }


    private void checkAbsentModule(ModuleRepository repo)
        throws Exception
    {
        assertNull(locateModule(repo, "/no/such/module"));
    }


    private void checkActualModule(ModuleRepository repo)
        throws Exception
    {
        ModuleLocation loc = locateModule(repo, "/ftst/symbol");
        assertNotNull(loc);
        assertNotNull(loc.toString());

        ResourceDescriptor desc = loc.sourceName();
        assertThat(desc.display(), endsWith("/ftst/symbol.fusion"));

        Evaluator eval       = evaluator();
        IonReader ionReader  = openIonReader(eval, desc.getResourceId());
        Object    moduleSexp = read(eval, ionReader, desc);
        assertTrue(isPair(eval, moduleSexp));
    }


    private void checkRepository(URL url, String pathPrefix)
        throws Exception
    {
        ClassLoader cl = new URLClassLoader(new URL[]{ url }, null);

        ModuleRepository repo = new ClassLoaderModuleRepository(cl, pathPrefix);

        checkAbsentModule(repo);
        checkActualModule(repo);
    }


    @Test
    public void loadModuleFromDirectory()
        throws Exception
    {
        Path dir = testRepositoryDirectory();

        URL url = dir.toUri().toURL();
        assertEquals("file", url.getProtocol());

        // Precondition for URLClassLoader to treat the URL as a directory:
        assertThat(url.getFile(), endsWith("/"));

        checkRepository(url, ".");
    }


    @Test
    public void loadModuleFromJar()
        throws Exception
    {
        // Create a classloader with the Jor constructed by our build logic.
        // TODO We should construct the Jar here to eliminate build-logic
        //   coupling, but that's not possible AFAICT using JDK APIs.
        Path jar = PROJECT_DIRECTORY.resolve("build")
                                    .resolve("libs")
                                    .resolve("ftst-repo.jar");
        assertTrue(isRegularFile(jar), "regular file");

        URL url = jar.toUri().toURL();

        // Precondition for URLClassLoader to treat the URL as a JAR file:
        assertThat(url.getFile(), not(endsWith("/")));

        checkRepository(url, "FUSION-REPO");
    }

    // TODO classpath shadowing
}
