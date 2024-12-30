// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.KERNEL_MODULE_IDENTITY;
import static dev.ionfusion.fusion.GlobalState.KERNEL_MODULE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.amazon.ion.IonReader;
import com.amazon.ion.system.IonReaderBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NamespaceTest
    extends CoreTestCase
{
    @BeforeEach
    public void requires()
        throws FusionException
    {
        useTstRepo();
        topLevel().requireModule("/fusion/namespace");
    }

    private IonReader read(String ionText)
    {
        return IonReaderBuilder.standard().build(ionText);
    }


    @Test
    public void testBaseNamespace()
        throws Exception
    {
        topLevel().define("callback", new Procedure1()
        {
            @Override
            Object doApply(Evaluator eval, Object arg)
                throws FusionException
            {
                Namespace ns = (Namespace) arg;
                Object moduleForm = ns.lookup(GlobalState.MODULE);
                assertTrue(moduleForm instanceof ModuleForm);

                ModuleRegistry reg = ns.getRegistry();
                ModuleInstance kernel = reg.lookup(KERNEL_MODULE_IDENTITY);
                assertEquals(KERNEL_MODULE_NAME, kernel.getInferredName());

                return null;
            }
        });

        eval("(callback (make_namespace_with_language \"/fusion\"))");
    }


    @Test
    public void testBadLanguage()
        throws Exception
    {
        expectArgumentExn("(make_namespace_with_language 23)", 0);
        expectArgumentExn("(make_namespace_with_language \"\")", 0);

        // Not absolute module path
        expectArgumentExn("(make_namespace_with_language \"foo\")", 0);
    }


    private void assertLoaded(StandardTopLevel top, String module)
        throws Exception
    {
        assertTrue(top.getRegistry().isLoaded(module));
    }

    private void assertNotLoaded(StandardTopLevel top, String module)
        throws Exception
    {
        assertFalse(top.getRegistry().isLoaded(module));
    }

    private void assertNotAttached(StandardTopLevel top, String module)
        throws Exception
    {
        assertNull(top.getRegistry().lookup(module));
    }

    private void assertAttached(StandardTopLevel top, String module)
        throws Exception
    {
        assertNotNull(top.getRegistry().lookup(module));
    }


    private void assertSameInstances(StandardTopLevel top0,
                                     StandardTopLevel top1,
                                     String module)
    {
        ModuleInstance inst0 = top0.getRegistry().lookup(module);
        ModuleInstance inst1 = top1.getRegistry().lookup(module);
        assertNotNull(inst0);
        assertSame(inst0, inst1);
    }

    private void assertDifferentInstances(StandardTopLevel top0,
                                          StandardTopLevel top1,
                                          String module)
    {
        ModuleInstance inst0 = top0.getRegistry().lookup(module);
        ModuleInstance inst1 = top1.getRegistry().lookup(module);
        assertNotNull(inst0);
        assertNotNull(inst1);
        assertNotSame(inst0, inst1);
    }



    /**
     * New registries must contain any essential low-level modules that must not
     * be duplicated.
     */
    @Test
    public void testNewRegistryHasKernel()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.getDefaultTopLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        assertSameInstances(top0, top1, KERNEL_MODULE_NAME);
        assertNotLoaded(top1, "/fusion");
        assertNotLoaded(top1, "/fusion/base");
    }


    @Test
    public void testRegistrySeparation()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.getDefaultTopLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        // Since top1's registry is empty modulo the kernel, we should get a
        // separate copy of any module we require.
        top1.requireModule("/fusion/base");
        assertDifferentInstances(top0, top1, "/fusion/base");
    }


    @Test
    public void testTopLevelLoadSeparation()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.makeEmptyTopLevelAndRegistry();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        top1.loadModule("/grain");
        assertNotLoaded(top0, "/grain");
        assertLoaded(top1, "/grain");
        assertNotAttached(top1, "/grain"); // loadModule() doesn't instantiate.

        top1.requireModule("/grain");
        assertAttached(top1, "/grain");
        assertNotLoaded(top0, "/grain");

        top0.requireModule("/grain");
        assertDifferentInstances(top0, top1, "/grain");
    }


    @Test
    public void testManualDeclarationSeparation()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.getDefaultTopLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        top1.loadModule("/manual",
                        read("(module M '/tinylang' true)"),
                        null);
        assertNotLoaded(top0, "/manual");

        top1.requireModule("/manual");
        assertAttached(top1, "/manual");
        assertNotLoaded(top0, "/manual");
    }


    @Test
    public void testNamespaceAttachModule()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.getDefaultTopLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        // Pull a non-dependency of /fusion into the startup registry,
        // to ensure it's not copied by attach().
        assertNotLoaded(top0, "/fusion/eval");
        top0.requireModule("/fusion/eval");
        assertAttached(top0, "/fusion/eval");

        top1.attachModule(top0, "/fusion");
        assertSameInstances(top0, top1, "/fusion");
        // Transient dependencies are attached as well.
        assertSameInstances(top0, top1, "/fusion/for");
        // But not things that are not transient dependencies!
        assertNotLoaded(top1, "/fusion/eval");

        // Module name resolver will load a new module on demand.
        top1.loadModule("/fusion/eval");
        assertLoaded(top1, "/fusion/eval");
        assertNotAttached(top1, "/fusion/eval");

        // And the registry can instantiate it.
        top1.requireModule("/fusion/eval");
        assertDifferentInstances(top0, top1, "/fusion/eval");
    }


    @Test
    public void testAttachManuallyDeclaredModule()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.makeEmptyTopLevelAndRegistry();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        top0.loadModule("/manual",
                        read("(module M '/tinylang' true)"),
                        null);
        top0.requireModule("/manual");

        top1.attachModule(top0, "/manual");
        assertAttached(top1, "/manual");
        assertAttached(top1, "/tinylang");
    }


    @Test
    public void testAttachFromNonDefaultRegistry()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.makeEmptyTopLevelAndRegistry();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        top0.requireModule("/grain");

        top1.attachModule(top0, "/grain");
        assertAttached(top1, "/grain");

        // Make sure we don't side-effect the default registry.
        assertNotLoaded((StandardTopLevel) topLevel(), "/grain");
    }


    @Test
    public void testAttachFailsWhenModuleNotDefinedInSource()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.makeEmptyTopLevelAndRegistry();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        try
        {
            top1.attachModule(top0, "/grain");
            fail("Expected ContractException");
        }
        catch (ContractException e)
        {
            assertThat(e.getMessage(),
                       containsString("Source registry has no instantiation of module /grain"));
        }

        assertNotLoaded((StandardTopLevel) topLevel(), "/grain");
        assertNotLoaded(top0, "/grain");
        assertNotLoaded(top1, "/grain");
    }


    @Test
    public void testAttachFailsWhenModuleNotInstantiatedInSource()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = runtime.makeEmptyTopLevelAndRegistry();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        top0.loadModule("/grain");  // Loaded but not instantiated.

        try
        {
            top1.attachModule(top0, "/grain");
            fail("Expected ContractException");
        }
        catch (ContractException e)
        {
            assertThat(e.getMessage(),
                       containsString("Source registry has no instantiation of module /grain"));
        }

        assertNotLoaded((StandardTopLevel) topLevel(), "/grain");
        assertNotLoaded(top1, "/grain");
    }


    @Test
    public void testAttachFailsWhenModuleDiffersInTarget()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = (StandardTopLevel) topLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();

        // Instantiate two copies of the same module.
        top0.requireModule("/grain");
        top1.requireModule("/grain");

        Throwable e =
            assertThrows(ContractException.class,
                         () -> top1.attachModule(top0, "/grain"));
        assertTrue(e.getMessage().contains("Destination registry already has a module with identity /grain"));
    }


    @Test
    public void testAttachFailsWhenDependencyDiffersInTarget()
        throws Exception
    {
        StandardRuntime runtime = (StandardRuntime) runtime();

        StandardTopLevel top0 = (StandardTopLevel) topLevel();
        StandardTopLevel top1 = runtime.makeEmptyTopLevelAndRegistry();
        top1.requireModule("/fusion");
        assertDifferentInstances(top0, top1, "/fusion");

        top0.requireModule("/grain"); // which requires /fusion

        Throwable e =
            assertThrows(ContractException.class,
                         () -> top1.attachModule(top0, "/grain"));
        assertTrue(e.getMessage().contains("Destination registry already has a module with identity /fusion"));
    }
}
