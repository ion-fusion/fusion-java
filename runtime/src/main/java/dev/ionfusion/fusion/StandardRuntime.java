// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.KERNEL_MODULE_IDENTITY;
import static dev.ionfusion.runtime.base.ModuleIdentity.forAbsolutePath;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidAbsoluteModulePath;

import com.amazon.ion.IonCatalog;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.system.IonSystemBuilder;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.embed.FusionRuntime;
import dev.ionfusion.runtime.embed.ModuleBuilder;
import dev.ionfusion.runtime.embed.SandboxBuilder;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.OutputStream;


final class StandardRuntime
    implements FusionRuntime
{
    private final GlobalState      myGlobalState;
    private final ModuleRegistry   myRegistry;
    private final String           myDefaultLanguage;
    private final StandardTopLevel myTopLevel;


    StandardRuntime(FusionRuntimeBuilder builder)
        throws FusionInterrupt
    {
        IonSystem ionSystem =
            IonSystemBuilder.standard()
                .withCatalog(builder.getDefaultIonCatalog())
                .build();
        myRegistry = new ModuleRegistry();

        myDefaultLanguage = builder.getDefaultLanguage();

        try
        {
            // This is the bootstrap top-level namespace, which starts out
            // empty.  It becomes the initial value of current_namespace
            // during global initialization.
            Namespace topNs = new TopLevelNamespace(myRegistry);

            myGlobalState =
                GlobalState.initialize(ionSystem, builder, myRegistry, topNs);

            myTopLevel = makeTopLevel(topNs, myDefaultLanguage);
        }
        catch (FusionException e)
        {
            throw new RuntimeException("Should not happen", e);
        }
    }


    // Not public
    GlobalState getGlobalState()
    {
        return myGlobalState;
    }

    // Not public
    ModuleRegistry getDefaultRegistry()
    {
        return myRegistry;
    }

    @Override
    public String getDefaultLanguage()
    {
        return myDefaultLanguage;
    }

    @Override
    public IonCatalog getDefaultIonCatalog()
    {
        return myGlobalState.myIonSystem.getCatalog();
    }

    // Not public
    _Private_CoverageCollector getCoverageCollector()
    {
        return myGlobalState.myCoverageCollector;
    }

    //========================================================================


    @Override
    public StandardTopLevel getDefaultTopLevel()
        throws FusionException
    {
        return myTopLevel;
    }


    StandardTopLevel makeTopLevel(Namespace namespace,
                                  String initialModulePath,
                                  Object... parameterization)
        throws FusionException
    {
        try
        {
            StandardTopLevel top =
                new StandardTopLevel(myGlobalState, namespace, parameterization);
            if (initialModulePath != null)
            {
                if (! isValidAbsoluteModulePath(initialModulePath))
                {
                    String message =
                        "Not a valid absolute module path: " + initialModulePath;
                    throw new IllegalArgumentException(message);
                }

                top.requireModule(initialModulePath);
            }
            return top;
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }

    StandardTopLevel makeTopLevel(ModuleRegistry registry,
                                  String initialModulePath)
        throws FusionException
    {
        return makeTopLevel(new TopLevelNamespace(registry), initialModulePath);
    }

    @Override
    public TopLevel makeTopLevel(String initialModulePath)
        throws FusionException
    {
        return makeTopLevel(myRegistry, initialModulePath);
    }


    StandardTopLevel makeEmptyTopLevelAndRegistry()
        throws FusionException
    {
        return makeTopLevel(makeModuleRegistry(), null);
    }


    @Override
    public TopLevel makeTopLevel()
        throws FusionException
    {
        return makeTopLevel(getDefaultLanguage());
    }


    private ModuleRegistry makeModuleRegistry()
        throws FusionException
    {
        ModuleNameResolver resolver = myGlobalState.myModuleNameResolver;

        ModuleRegistry r = new ModuleRegistry();
        r.attach(resolver, myRegistry, KERNEL_MODULE_IDENTITY);
        return r;
    }


    @Override
    public SandboxBuilder makeSandboxBuilder()
    {
        return new SandboxBuilderImpl();
    }


    @Override
    public ModuleBuilder makeModuleBuilder(String absoluteModulePath)
    {
        if (! isValidAbsoluteModulePath(absoluteModulePath))
        {
            String message =
                "Invalid absolute module path: " + absoluteModulePath;
            throw new IllegalArgumentException(message);
        }

        ModuleIdentity id = forAbsolutePath(absoluteModulePath);
        return new ModuleBuilderImpl(myGlobalState.myModuleNameResolver,
                                     myRegistry,
                                     id);
    }


    //========================================================================


    @Override
    public IonValue ionize(Object fusionValue, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValue(myTopLevel.getEvaluator(), fusionValue, factory);
    }


    @Override
    public IonValue ionizeMaybe(Object fusionValue, ValueFactory factory)
        throws FusionException
    {
        return FusionValue.copyToIonValueMaybe(myTopLevel.getEvaluator(), fusionValue, factory);
    }


    //========================================================================


    private class SandboxBuilderImpl
        implements SandboxBuilder
    {
        private String myLanguage;

        @Override
        public void setLanguage(String absoluteModulePath)
        {
            if (! isValidAbsoluteModulePath(absoluteModulePath))
            {
                String message =
                    "Not a valid absolute module path: " + absoluteModulePath;
                throw new IllegalArgumentException(message);
            }
            myLanguage = absoluteModulePath;
        }

        @Override
        public TopLevel build()
            throws FusionException
        {
            if (myLanguage == null)
            {
                throw new IllegalStateException("No language specified.");
            }
            ModuleIdentity langId = forAbsolutePath(myLanguage);

            // Our goal here, for now, is to emulate:
            // (make-evaluator `(begin) #:requires (list <language>))

            // That said, we should avoid replicated instances of /fusion/**
            // modules.

            try
            {
                ModuleNameResolver resolver =
                    myGlobalState.myModuleNameResolver;

                Object currentIonReader =
                    myGlobalState.myCurrentIonReaderParam;
                Object currentNamespace =
                    myGlobalState.myCurrentNamespaceParam;
                Object currentOutputPort =
                    myGlobalState.myCurrentOutputPortParam;
                Object currentSecurityGuard =
                    myGlobalState.myCurrentSecurityGuardParam;

                IonReader reader =
                    myGlobalState.myIonReaderBuilder.build("");

                // When language is given as a module path, Racket creates a
                // new module-namespace in that language.

                // SEE make-evaluation-namespace in sandbox.rkt
                //  * Using current-namespace to find the current registry,
                //     * instantiate myLanguage
                //  * Create the new namespace...
                //     * Create a fresh registry, it should contain any core
                //       singleton modules eg #%kernel
                //  * namespace-attach-module the language from current NS.
                //    This attaches transitive dependencies.
                //  * Require the language into the new NS.

                // Hack to force instantiation of myLanguage in myRegistry.
                makeTopLevel(myRegistry, myLanguage);

                ModuleRegistry newRegistry = makeModuleRegistry();
                newRegistry.attach(resolver, myRegistry, langId);

                TopLevelNamespace namespace = new TopLevelNamespace(newRegistry);

                return makeTopLevel(namespace,
                                    myLanguage,
                                    // Continuation mark key/values:
                                    currentIonReader,
                                    reader,
                                    currentNamespace,
                                    namespace,
                                    currentOutputPort,
                                    new NullOutputStream(),
                                    currentSecurityGuard,
                                    SecurityGuard.CLOSED);

                // StandardTopLevel automagically sets current_namespace for
                // some entry points like eval() and load(), but not for others
                // like call() or loadModule().
                // TODO StandardTopLevel should *always* set current_namespace
                //   in its outermost Evaluator, but we need some backwards
                //   compatibility analysis before doing that.
            }
            catch (FusionInterrupt e)
            {
                throw new FusionInterruptedException(e);
            }
        }
    }


    private static final class NullOutputStream
        extends OutputStream
    {
        @Override
        public void write(int b)
        {
        }

        @Override
        public void write(byte[] b)
        {
        }

        @Override
        public void write(byte[] b, int off, int len)
        {
        }
    }
}
