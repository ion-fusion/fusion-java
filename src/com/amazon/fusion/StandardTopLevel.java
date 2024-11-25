// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionIo.safeWriteToString;
import static com.amazon.fusion.FusionVoid.voidValue;
import static com.amazon.fusion.ModuleIdentity.isValidAbsoluteModulePath;
import static com.amazon.fusion.StandardReader.readSyntax;
import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import com.amazon.ion.IonReader;
import java.io.File;
import java.io.IOException;


final class StandardTopLevel
    implements TopLevel
{
    private final Evaluator myEvaluator;
    private final Namespace myNamespace;


    StandardTopLevel(GlobalState globalState,
                     Namespace namespace,
                     Object... continuationMarks)
        throws FusionInterrupt, FusionException
    {
        _Private_CoverageCollector collector = globalState.myCoverageCollector;
        Evaluator eval = (collector == null
                            ? new Evaluator(globalState)
                            : new CoverageEvaluator(globalState, collector));

        if (continuationMarks.length != 0)
        {
            eval = eval.markedContinuation(continuationMarks);
        }

        myEvaluator = eval;
        myNamespace = namespace;
    }


    //========================================================================

    /**
     * Helper method for internal APIs
     */
    static Evaluator toEvaluator(TopLevel top)
    {
        return ((StandardTopLevel) top).myEvaluator;
    }


    // NOT PUBLIC
    Evaluator getEvaluator()
    {
        return myEvaluator;
    }

    // NOT PUBLIC
    Namespace getNamespace()
    {
        return myNamespace;
    }

    // NOT PUBLIC
    ModuleRegistry getRegistry()
    {
        return myNamespace.getRegistry();
    }

    @Override
    public Object eval(String source, SourceName name)
        throws FusionInterruptedException, FusionException
    {
        try (IonReader i = myEvaluator.getIonReaderBuilder().build(source))
        {
            return eval(i, name);
        }
        catch (IOException e)
        {
            String message =
                "Error closing " + (name == null ? "source" : name.display());
            throw new ContractException(message, e);
        }
    }


    @Override
    public Object eval(String source)
        throws FusionInterruptedException, FusionException
    {
        return eval(source, null);
    }


    @Override
    public Object eval(IonReader source, SourceName name)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            Object result = voidValue(myEvaluator);

            if (source.getType() == null) source.next();
            while (source.getType() != null)
            {
                SyntaxValue sourceExpr = readSyntax(myEvaluator, source, name);

                // This method parameterizes current_namespace for us:
                result = FusionEval.eval(myEvaluator, sourceExpr, myNamespace);
                source.next();
            }

            return result;
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    @Override
    public Object eval(IonReader source)
        throws FusionInterruptedException, FusionException
    {
        return eval(source, null);
    }


    @Override
    public Object load(File source)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            LoadHandler load = myEvaluator.getGlobalState().myLoadHandler;

            // This method parameterizes current_namespace for us:
            return load.loadTopLevel(myEvaluator,
                                     myNamespace,
                                     source.toString());
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    @Override
    public void loadModule(String     absoluteModulePath,
                           IonReader  source,
                           SourceName name)
        throws FusionInterruptedException, FusionException
    {
        if (! isValidAbsoluteModulePath(absoluteModulePath))
        {
            String message =
                "Invalid absolute module path: " + absoluteModulePath;
            throw new IllegalArgumentException(message);
        }

        try
        {
            // Make sure we use the registry on our namespace.
            Evaluator eval =
                myEvaluator.parameterizeCurrentNamespace(myNamespace);

            ModuleNameResolver resolver =
                myEvaluator.getGlobalState().myModuleNameResolver;
            ModuleIdentity id =
                ModuleIdentity.forAbsolutePath(absoluteModulePath);
            ModuleLocation loc =
                ModuleLocation.forIonReader(source, name);

            resolver.loadModule(eval, id, loc, true /* reload it */);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }

    ModuleIdentity loadModule(String modulePath)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            return myNamespace.resolveAndLoadModule(myEvaluator, modulePath);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    void attachModule(StandardTopLevel src, String modulePath)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            myNamespace.attachModule(myEvaluator, src.myNamespace, modulePath);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }

    @Override
    public void requireModule(String modulePath)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            myNamespace.require(myEvaluator, modulePath);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    @Override
    public void define(String name, Object value)
        throws FusionException
    {
        try
        {
            Object fv = myEvaluator.injectMaybe(value);
            if (fv == null)
            {
                String expected =
                    "injectable Java type but received " +
                        value.getClass().getName();
                throw new ArgumentException("TopLevel.define", expected,
                                            -1, value);
            }

            myNamespace.bind(name, fv);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    @Override
    public Object lookup(String name)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            return myNamespace.lookup(name);
        }
        catch (FusionInterrupt e)
        {
            // I don't think this can happen, but I prefer to be consistent
            // throughout this class to be more resilient to changes.
            throw new FusionInterruptedException(e);
        }
    }


    private Procedure lookupProcedure(String procedureName)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            Object proc = lookup(procedureName);
            if (proc instanceof Procedure)
            {
                return (Procedure) proc;
            }

            if (proc == null)
            {
                throw new FusionException(printQuotedSymbol(procedureName) +
                                          " is not defined");
            }

            throw new FusionException(printQuotedSymbol(procedureName) +
                                      " is not a procedure: " +
                                      safeWriteToString(myEvaluator, proc));
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    private Object call(Procedure proc, Object... arguments)
        throws FusionInterruptedException, FusionException
    {
        try
        {
            for (int i = 0; i < arguments.length; i++)
            {
                Object arg = arguments[i];
                Object fv = myEvaluator.injectMaybe(arg);
                if (fv == null)
                {
                    String expected =
                        "injectable Java type but received " +
                            arg.getClass().getName();
                    throw new ArgumentException("TopLevel.call",
                                                expected,
                                                i, arguments);
                }
                arguments[i] = fv;
            }

            // TODO Should this set current_namespace?
            return myEvaluator.callNonTail(proc, arguments);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    @Override
    public Object call(String procedureName, Object... arguments)
        throws FusionInterruptedException, FusionException
    {
        Procedure proc = lookupProcedure(procedureName);

        return call(proc, arguments);
    }


    @Override
    public Object call(Object procedure, Object... arguments)
        throws FusionInterruptedException, FusionException
    {
        if (! (procedure instanceof Procedure))
        {
            throw new IllegalArgumentException("Not a procedure: " + procedure);
        }

        return call((Procedure) procedure, arguments);
    }
}
