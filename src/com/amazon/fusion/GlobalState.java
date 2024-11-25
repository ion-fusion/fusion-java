// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionValue.UNDEF;
import com.amazon.fusion.ModuleNamespace.ModuleDefinedBinding;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonReaderBuilder;
import java.io.IOException;
import java.io.InputStream;

/**
 * The core set of objects from that are needed by other parts of the
 * implementation.
 */
final class GlobalState
{
    static final String FUSION_SOURCE_EXTENSION = ".fusion";

    static final String KERNEL_MODULE_NAME = "/fusion/private/kernel";
    static final ModuleIdentity KERNEL_MODULE_IDENTITY =
        ModuleIdentity.forAbsolutePath(KERNEL_MODULE_NAME);

    static final String ALL_DEFINED_OUT       = "all_defined_out";
    static final String BEGIN                 = "begin";
    static final String CLOSED_SECURITY_GUARD = "closed_security_guard";
    static final String DEFINE_SYNTAX         = "define_syntax";
    static final String DEFINE_VALUES         = "define_values";
    static final String EOF                   = "eof";
    static final String LAMBDA                = "lambda";
    static final String MODULE                = "module";
    static final String ONLY_IN               = "only_in";
    static final String PREFIX_IN             = "prefix_in";
    static final String PROVIDE               = "provide";
    static final String RENAME_IN             = "rename_in";
    static final String RENAME_OUT            = "rename_out";
    static final String REQUIRE               = "require";

    final IonSystem                  myIonSystem;
    final IonReaderBuilder           myIonReaderBuilder;
    final FileSystemSpecialist       myFileSystemSpecialist;
    final ModuleInstance             myKernelModule;
    final ModuleNameResolver         myModuleNameResolver;
    final LoadHandler                myLoadHandler;
    final _Private_CoverageCollector myCoverageCollector;

    final DynamicParameter myCurrentIonReaderParam;
    final DynamicParameter myCurrentNamespaceParam;
    final DynamicParameter myCurrentOutputPortParam;
    final DynamicParameter myCurrentSecurityGuardParam;

    final Binding myKernelAllDefinedOutBinding;
    final Binding myKernelBeginBinding;
    final Binding myKernelDefineSyntaxBinding;
    final Binding myKernelDefineValuesBinding;
    final Binding myKernelOnlyInBinding;
    final Binding myKernelModuleBinding;
    final Binding myKernelPrefixInBinding;
    final Binding myKernelProvideBinding;
    final Binding myKernelRenameInBinding;
    final Binding myKernelRenameOutBinding;
    final Binding myKernelRequireBinding;

    private GlobalState(IonSystem                  ionSystem,
                        IonReaderBuilder           ionReaderBuilder,
                        FileSystemSpecialist       fileSystemSpecialist,
                        ModuleInstance             kernel,
                        ModuleNameResolver         resolver,
                        LoadHandler                loadHandler,
                        _Private_CoverageCollector coverageCollector)
    {
        myIonSystem             = ionSystem;
        myIonReaderBuilder      = ionReaderBuilder;
        myFileSystemSpecialist  = fileSystemSpecialist;
        myKernelModule          = kernel;
        myModuleNameResolver    = resolver;
        myLoadHandler           = loadHandler;
        myCoverageCollector     = coverageCollector;

        myCurrentIonReaderParam     = kernelValue("current_ion_reader");
        myCurrentNamespaceParam     = kernelValue("current_namespace");
        myCurrentOutputPortParam    = kernelValue("current_output_port");
        myCurrentSecurityGuardParam = kernelValue("current_security_guard");

        myKernelAllDefinedOutBinding = kernelBinding(ALL_DEFINED_OUT);
        myKernelBeginBinding         = kernelBinding(BEGIN);
        myKernelDefineSyntaxBinding  = kernelBinding(DEFINE_SYNTAX);
        myKernelDefineValuesBinding  = kernelBinding(DEFINE_VALUES);
        myKernelModuleBinding        = kernelBinding(MODULE);
        myKernelOnlyInBinding        = kernelBinding(ONLY_IN);
        myKernelPrefixInBinding      = kernelBinding(PREFIX_IN);
        myKernelProvideBinding       = kernelBinding(PROVIDE);
        myKernelRequireBinding       = kernelBinding(REQUIRE);
        myKernelRenameInBinding      = kernelBinding(RENAME_IN);
        myKernelRenameOutBinding     = kernelBinding(RENAME_OUT);
    }


    static GlobalState initialize(IonSystem system,
                                  FusionRuntimeBuilder builder,
                                  ModuleRegistry registry,
                                  Namespace initialCurrentNamespace)
        throws FusionException
    {
        IonReaderBuilder readerBuilder =
            IonReaderBuilder.standard()
                            .withCatalog(system.getCatalog())
                            .immutable();

        // WARNING: We pass null evaluator because we know its not used.
        //          That is NOT SUPPORTED for user code!
        Object userDir =
            makeString(null, builder.getInitialCurrentDirectory().toString());

        DynamicParameter currentOutputPort =
            new DynamicParameter(builder.getInitialCurrentOutputPort());
        DynamicParameter currentDirectory =
            new DynamicParameter(userDir);
        DynamicParameter currentLoadRelativeDirectory =
            new DynamicParameter(UNDEF);
        DynamicParameter currentModuleDeclareName =
            new DynamicParameter(UNDEF);
        DynamicParameter currentNamespaceParam =
            new DynamicParameter(initialCurrentNamespace);
        DynamicParameter currentSecurityGuard =
            new DynamicParameter(SecurityGuard.OPEN);

        FileSystemSpecialist fs =
            new FileSystemSpecialist(currentSecurityGuard,
                                     currentDirectory);
        LoadHandler loadHandler =
            new LoadHandler(fs, currentLoadRelativeDirectory);
        ModuleNameResolver resolver =
            new ModuleNameResolver(loadHandler,
                                   currentLoadRelativeDirectory,
                                   currentDirectory,
                                   currentModuleDeclareName,
                                   builder.buildModuleRepositories());

        ModuleBuilderImpl ns =
            new ModuleBuilderImpl(resolver, registry, KERNEL_MODULE_IDENTITY,
                                  kernelDocs(system));

        ns.define(ALL_DEFINED_OUT, new ProvideForm.AllDefinedOutForm());
        ns.define(BEGIN, new BeginForm());
        ns.define(CLOSED_SECURITY_GUARD, SecurityGuard.CLOSED);

        ns.define("current_output_port", currentOutputPort);
        ns.define("current_directory", currentDirectory);
        ns.define("current_ion_reader", new CurrentIonReaderParameter());
        ns.define("current_namespace", currentNamespaceParam);
        ns.define("current_security_guard", currentSecurityGuard);

        ns.define(DEFINE_SYNTAX, new DefineSyntaxForm());
        ns.define(DEFINE_VALUES, new DefineValuesForm());
        ns.define(EOF, FusionIo.eof(null));
        ns.define("java_new", new JavaNewProc());
        ns.define(LAMBDA, new LambdaForm());
        ns.define("load", new LoadProc(loadHandler));
        ns.define(MODULE, new ModuleForm(resolver, currentModuleDeclareName));
        ns.define(ONLY_IN, new RequireForm.OnlyInForm());
        ns.define(PREFIX_IN, new RequireForm.PrefixInForm());
        ns.define(PROVIDE, new ProvideForm());
        ns.define(RENAME_IN, new RequireForm.RenameInForm());
        ns.define(RENAME_OUT, new ProvideForm.RenameOutForm());
        ns.define(REQUIRE, new RequireForm(resolver));


        ns.instantiate();

        ModuleInstance kernel = registry.lookup(KERNEL_MODULE_IDENTITY);

        GlobalState globals =
            new GlobalState(system, readerBuilder, fs, kernel, resolver, loadHandler,
                            builder.getCoverageCollector());
        return globals;
    }


    /**
     * Ensure we have a {@linkplain Binding#target target binding} that can be
     * compared with {@code ==}.
     */
    private ModuleDefinedBinding kernelBinding(String name)
    {
        ModuleDefinedBinding b = myKernelModule.resolveProvidedName(name).target();
        assert b != null;
        return b;
    }

    @SuppressWarnings("unchecked")
    private <T> T kernelValue(String name)
    {
        ModuleDefinedBinding b = kernelBinding(name);
        return (T) b.lookup(myKernelModule);
    }

    /**
     * Creates a new identifier that's bound in this kernel.
     *
     * @param eval not null.
     * @param name must be defined by this kernel!
     */
    SyntaxSymbol kernelBoundIdentifier(Evaluator eval, String name)
    {
        SyntaxSymbol sym = SyntaxSymbol.make(eval, name);
        sym = sym.copyReplacingBinding(kernelBinding(name));
        return sym;
    }

    private static IonStruct kernelDocs(IonSystem system)
    {
        try (InputStream stream = GlobalState.class.getResourceAsStream("kernel_docs.ion");
             IonReader reader = system.newReader(stream))
        {
            reader.next();
            return (IonStruct) system.newValue(reader);
        }
        catch (IOException e)
        {
            throw new Error("Should not happen", e);
        }
    }
}
