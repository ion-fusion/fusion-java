// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionRuntimeBuilder.standard;
import static dev.ionfusion.fusion.FusionString.unsafeStringToJavaString;
import static dev.ionfusion.fusion.TestSetup.fusionBootstrapDirectory;
import static dev.ionfusion.testing.Reflect.assertEqualProperties;
import static dev.ionfusion.testing.Reflect.getterFor;
import static dev.ionfusion.testing.Reflect.invoke;
import static dev.ionfusion.testing.Reflect.setterFor;
import static dev.ionfusion.testing.Reflect.witherFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazon.ion.IonCatalog;
import com.amazon.ion.system.SimpleCatalog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


public class FusionRuntimeBuilderTest
    extends CoreTestCase
{
    @TempDir
    public File tmpDir;


    private FusionRuntime build(FusionRuntimeBuilder b)
        throws FusionException
    {
        b = b.withBootstrapRepository(fusionBootstrapDirectory().toFile());
        return b.build();
    }


    private static void assertCopiesAreEqual(FusionRuntimeBuilder b)
    {
        assertEqualProperties(b, b.copy());
        assertEqualProperties(b, b.mutable());
        assertEqualProperties(b, b.immutable());
    }


    /**
     * Test combinations of get/set/with methods for a builder property.
     */
    private <T> void changeProperty(FusionRuntimeBuilder orig,
                                    String property,
                                    Class<T> type,
                                    T newValue,
                                    T expectedNormalizedValue)
    {
        Method getter = getterFor(orig, property, type);
        Method setter = setterFor(orig, property, type);
        Method wither = witherFor(orig, property, type);

        Object defaultValue = invoke(orig, getter);

        FusionRuntimeBuilder b = orig.copy();
        invoke(b, setter, newValue);
        assertEquals(expectedNormalizedValue, invoke(b, getter));
        assertEquals(defaultValue, invoke(orig, getter));  // No aliasing w/orig
        assertCopiesAreEqual(b);

        FusionRuntimeBuilder c = orig.copy();
        assertSame(c, invoke(c, wither, newValue));
        assertEqualProperties(b, c);
        assertEquals(defaultValue, invoke(orig, getter));  // No aliasing w/orig
        assertCopiesAreEqual(c);

        // Immutable builder is copied on modification
        FusionRuntimeBuilder i = orig.copy().immutable();
        FusionRuntimeBuilder d = invoke(i, wither, newValue);
        assertNotSame(i, d);
        assertEqualProperties(orig, i); // the immutable hasn't changed
        assertEqualProperties(b, d);
        assertCopiesAreEqual(d);

        invoke(b, setter, defaultValue);
        assertEquals(defaultValue, invoke(b, getter));
        assertEqualProperties(orig, b);
        assertCopiesAreEqual(b);

        invoke(c, wither, defaultValue);
        assertEquals(defaultValue, invoke(c, getter));
        assertEqualProperties(orig, c);
        assertCopiesAreEqual(c);
    }


    /**
     * Creates a temporary normal (non-directory) file.
     *
     * @return a file that exists.
     */
    private File normalFile()
    {
        try
        {
            // JUnit docs aren't explicit that newFile() creates a physical
            // file, not just a File reference, so assert that assumption.
            File file = File.createTempFile("junit", null, tmpDir);
            assertTrue(file.exists());
            return file;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Generates a path to a non-existing file.
     *
     * @return a file that doesn't exist.
     */
    private File noSuchFile()
    {
        Path p = tmpDir.toPath().resolve("no-such-file");
        assertTrue(Files.notExists(p));
        return p.toFile();
    }


    //========================================================================


    @Test
    public void testDefaultBuilder()
    {
        FusionRuntimeBuilder b = standard();
        assertSame(b, b.mutable());

        assertEquals("/fusion", b.getDefaultLanguage());
        assertNull(b.getInitialCurrentOutputPort());
        assertNull(b.getInitialCurrentDirectory());
        assertNull(b.getBootstrapRepository());
        assertNull(b.getCoverageDataDirectory());
        assertNull(b.getCoverageCollector());
        assertFalse(b.isDocumenting());

        assertCopiesAreEqual(b);
    }


    //========================================================================


    @Test
    public void testSetDefaultLanguage()
    {
        changeProperty(standard(), "DefaultLanguage", String.class,
                       "/fusion/base", "/fusion/base");
    }

    @Test
    public void testNullDefaultLanguage()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setDefaultLanguage(null));
    }

    @Test
    public void testEmptyDefaultLanguage()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setDefaultLanguage(""));
    }

    @Test
    public void testMalformedDefaultLanguage()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setDefaultLanguage("fusion"));
    }

    @Test
    public void testDefaultLanguageImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable()
                                     .setDefaultLanguage("/fusion/base"));
    }


    @Test
    public void testBuildingDefaultLanguage()
        throws Exception
    {
        FusionRuntimeBuilder b = standard();
        FusionRuntime r = build(b);
        assertEquals("/fusion", r.getDefaultLanguage());

        // Change the default
        b.setDefaultLanguage("/fusion/base");
        r = build(b);
        assertEquals("/fusion/base", r.getDefaultLanguage());

        // Test effect by looking for something in /fusion but not /fusion/base
        TopLevel top = r.getDefaultTopLevel();
        assertThrows(UnboundIdentifierException.class,
                     () -> top.eval("always"));
    }


    //========================================================================


    private void changeDefaultIonCatalog(FusionRuntimeBuilder orig,
                                         IonCatalog catalog)
    {
        changeProperty(orig, "DefaultIonCatalog", IonCatalog.class,
                       catalog, catalog);
    }


    @Test
    public void testSetDefaultIonCatalog()
    {
        IonCatalog catalog = new SimpleCatalog();

        changeDefaultIonCatalog(standard(), catalog);
    }

    @Test
    public void testDefaultIonCatalogImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable()
                                     .setDefaultIonCatalog(new SimpleCatalog()));
    }

    @Test
    public void testBuildingDefaultIonCatalog()
        throws FusionException
    {
        FusionRuntimeBuilder b = standard();
        FusionRuntime r = build(b);
        assertThat(r.getDefaultIonCatalog(), instanceOf(SimpleCatalog.class));

        SimpleCatalog catalog = new SimpleCatalog();
        b.withDefaultIonCatalog(catalog);
        r = build(b);
        assertSame(catalog, r.getDefaultIonCatalog());
    }


    //========================================================================


    private void changeInitialCurrentOutputPort(FusionRuntimeBuilder orig,
                                                OutputStream out)
    {
        changeProperty(orig, "InitialCurrentOutputPort", OutputStream.class,
                       out, out);
    }


    @Test
    public void testSetInitialCurrentOutputPort()
    {
        OutputStream out = new ByteArrayOutputStream();
        changeInitialCurrentOutputPort(standard(), out);
    }


    private void checkCurrentOutputPort(OutputStream expected,
                                        FusionRuntimeBuilder b)
        throws FusionException
    {
        TopLevel t = build(b).makeTopLevel(GlobalState.KERNEL_MODULE_NAME);
        Object actual = t.eval("(current_output_port)");
        assertSame(expected, actual);
    }

    @Test
    public void testBuildingInitialCurrentOutputPort()
        throws Exception
    {
        FusionRuntimeBuilder b = standard();
        checkCurrentOutputPort(System.out, b);

        OutputStream out = new ByteArrayOutputStream();
        b.setInitialCurrentOutputPort(out);
        checkCurrentOutputPort(out, b);
    }

    @Test
    public void testInitialCurrentOutputPortImmutability()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable().setInitialCurrentOutputPort(out));
    }



    //========================================================================


    private void changeInitialCurrentDirectory(FusionRuntimeBuilder orig,
                                               File dir)
    {
        changeProperty(orig, "InitialCurrentDirectory", File.class,
                       dir, dir.getAbsoluteFile());
    }


    @Test
    public void testSetInitialCurrentDirectory()
    {
        changeInitialCurrentDirectory(standard(), tmpDir);
    }


    private void checkCurrentDirectory(String expectedDir, FusionRuntimeBuilder b)
        throws FusionException
    {
        TopLevel t = build(b).getDefaultTopLevel();
        t.requireModule("/fusion/io");
        Object actualDir = t.eval("(current_directory)");
        assertEquals(expectedDir, unsafeStringToJavaString(t, actualDir));
    }

    @Test
    public void testBuildingInitialCurrentDirectory()
        throws Exception
    {
        FusionRuntimeBuilder b = standard();
        checkCurrentDirectory(System.getProperty("user.dir"), b);

        b.setInitialCurrentDirectory(tmpDir);
        checkCurrentDirectory(tmpDir.getAbsolutePath(), b);
    }


    @Test
    public void testCurrentDirectoryDoesNotExist()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setInitialCurrentDirectory(noSuchFile()));
    }


    @Test
    public void testCurrentDirectoryIsNormalFile()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setInitialCurrentDirectory(normalFile()));
    }

    @Test
    public void testInitialCurrentDirectoryImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable()
                                     .setInitialCurrentDirectory(tmpDir));
    }


    //========================================================================


    private void changeBootstrapRepository(FusionRuntimeBuilder orig, File dir)
    {
        changeProperty(orig, "BootstrapRepository", File.class,
                       dir, dir.getAbsoluteFile());
    }


    @Test
    public void testSetBootstrapRepository()
    {
        changeBootstrapRepository(standard(), fusionBootstrapDirectory().toFile());
    }


    @Test
    public void testBootstrapRepositoryDoesNotExist()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setBootstrapRepository(noSuchFile()));
    }


    @Test
    public void testBootstrapRepositoryIsNormalFile()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setBootstrapRepository(normalFile()));
    }

    @Test
    public void testBootstrapRepositoryImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable()
                                     .setBootstrapRepository(tmpDir));
    }


    //========================================================================


    private void changeCoverageDataDirectory(FusionRuntimeBuilder orig,
                                             File dir)
    {
        changeProperty(orig, "CoverageDataDirectory", File.class,
                       dir, dir.getAbsoluteFile());
    }


    @Test
    public void testSetCoverageDataDirectory()
    {
        changeCoverageDataDirectory(standard(), tmpDir);
    }


    /**
     * Ensures that different paths to the same directory don't result in
     * different collectors that would interfere with each other.
     */
    @Test
    public void testCoverageDataDirectoryCanonicalization()
        throws Exception
    {
        Path dir1 = tmpDir.toPath();
        Path dir2 = newFolder(tmpDir, "linkholder").toPath();

        Path link = dir2.resolve("link");
        Files.createSymbolicLink(link, dir1);

        _Private_CoverageCollector c1 = makeCollector(dir1);
        _Private_CoverageCollector c2 = makeCollector(dir2);
        _Private_CoverageCollector c3 = makeCollector(link);

        assertNotSame(c1, c2, "different dirs");
        assertSame(c1, c3, "canonicalized symlink");
    }

    private _Private_CoverageCollector makeCollector(Path dir)
        throws FusionException
    {
        FusionRuntime r =
            runtimeBuilder().copy()
                            .withCoverageDataDirectory(dir.toFile())
                            .build();
        _Private_CoverageCollector c =
            ((StandardRuntime) r).getCoverageCollector();
        assertNotNull(c);
        return c;
    }


    @Test
    public void testCoverageDataDirectoryDoesNotExist()
    {
        // Directory doesn't have to exist
        changeCoverageDataDirectory(standard(), noSuchFile());
    }


    @Test
    public void testCoverageDataDirectoryIsNormalFile()
    {
        assertThrows(IllegalArgumentException.class,
                     () -> standard().setCoverageDataDirectory(normalFile()));
    }


    @Test
    public void testCoverageDataDirectoryImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable()
                                     .setCoverageDataDirectory(tmpDir));
    }


    //========================================================================


    @Test
    public void testSetDocumenting()
    {
        FusionRuntimeBuilder std = standard();

        FusionRuntimeBuilder b = std.copy();
        b.setDocumenting(true);
        assertTrue(b.isDocumenting());
        assertCopiesAreEqual(b);
    }

    @Test
    public void testDocumentingImmutability()
    {
        assertThrows(UnsupportedOperationException.class,
                     () -> standard().immutable().setDocumenting(true));
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
