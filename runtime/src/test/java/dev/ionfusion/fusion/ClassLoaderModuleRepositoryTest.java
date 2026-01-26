// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionIo.read;
import static dev.ionfusion.fusion.FusionSexp.isPair;
import static dev.ionfusion.fusion.TestSetup.PROJECT_DIRECTORY;
import static dev.ionfusion.fusion.TestSetup.testRepositoryDirectory;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceName;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
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

        SourceName name = loc.sourceName();
        assertTrue(name.display().contains("/ftst/symbol.fusion"));

        Evaluator eval       = evaluator();
        IonReader ionReader  = loc.read(eval);
        Object    moduleSexp = read(eval, ionReader);
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
        assert url.getProtocol().equals("file");

        // Precondition for URLClassLoader to treat the URL as a directory:
        assert url.getFile().endsWith("/");

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
        assert Files.isRegularFile(jar);

        URL url = jar.toUri().toURL();

        // Precondition for URLClassLoader to treat the URL as a JAR file:
        assert ! url.getFile().endsWith("/");

        checkRepository(url, "FUSION-REPO");
    }

    // TODO classpath shadowing
}
