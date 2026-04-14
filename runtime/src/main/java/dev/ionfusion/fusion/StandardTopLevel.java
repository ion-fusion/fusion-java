// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printQuotedSymbol;
import static dev.ionfusion.fusion.FusionIo.safeWriteToString;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.StandardReader.readSyntax;
import static dev.ionfusion.runtime._private.util.Ordinals.friendlyIndex;
import static dev.ionfusion.runtime.base.ModuleIdentity.isValidAbsoluteModulePath;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonWriter;
import dev.ionfusion.runtime._private.cover.CoverageCollector;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceName;
import dev.ionfusion.runtime.embed.TopLevel;
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
        CoverageCollector collector = globalState.myCoverageCollector;
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

    @FunctionalInterface
    interface EvalTask<T>
    {
        T run(Evaluator eval) throws FusionException;
    }

    <T> T withEvaluator(EvalTask<T> task)
        throws FusionException
    {
        try
        {
            return task.run(myEvaluator);
        }
        catch (FusionInterrupt e)
        {
            throw new FusionInterruptedException(e);
        }
    }


    //========================================================================

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
        return withEvaluator(eval -> {
            Object result = voidValue(eval);

            if (source.getType() == null) source.next();
            while (source.getType() != null)
            {
                SyntaxValue sourceExpr = readSyntax(eval, source, name);

                // This method parameterizes current_namespace for us:
                result = FusionEval.eval(eval, sourceExpr, myNamespace);
                source.next();
            }

            return result;
        });
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
        return withEvaluator(eval -> {
            LoadHandler load = eval.getGlobalState().myLoadHandler;

            // This method parameterizes current_namespace for us:
            return load.loadTopLevel(eval, myNamespace, source.toString());
        });
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

        withEvaluator(eval -> {
            // Make sure we use the registry on our namespace.
            Evaluator parameterized =
                eval.parameterizeCurrentNamespace(myNamespace);

            ModuleNameResolver resolver =
                eval.getGlobalState().myModuleNameResolver;
            ModuleIdentity id =
                ModuleIdentity.forAbsolutePath(absoluteModulePath);
            ModuleLocation loc =
                ModuleLocation.forIonReader(source, name);

            resolver.loadModule(parameterized, id, loc, true /* reload it */);
            return null;
        });
    }

    ModuleIdentity loadModule(String modulePath)
        throws FusionInterruptedException, FusionException
    {
        return withEvaluator(eval ->
                                 myNamespace.resolveAndLoadModule(eval, modulePath));
    }

    /**
     * Get the instance of a previously loaded module.
     */
    ModuleInstance instantiateLoadedModule(ModuleIdentity id)
        throws FusionInterruptedException, FusionException
    {
        return withEvaluator(eval ->
                                 getRegistry().instantiate(eval, id));
    }


    void attachModule(StandardTopLevel src, String modulePath)
        throws FusionInterruptedException, FusionException
    {
        withEvaluator(eval -> {
            myNamespace.attachModule(eval, src.myNamespace, modulePath);
            return null;
        });
    }

    @Override
    public void requireModule(String modulePath)
        throws FusionInterruptedException, FusionException
    {
        withEvaluator(eval -> {
            myNamespace.require(eval, modulePath);
            return null;
        });
    }


    @Override
    public void define(String name, Object value)
        throws FusionException
    {
        Object fv = myEvaluator.injectMaybe(value);
        if (fv == null)
        {
            String msg =
                "Expected injectable Java object but received " +
                value.getClass().getName();
            throw new IllegalArgumentException(msg);
        }

        withEvaluator(eval -> {
            myNamespace.bind(name, fv);
            return null;
        });
    }


    @Override
    public Object lookup(String name)
        throws FusionInterruptedException, FusionException
    {
        return withEvaluator(eval -> myNamespace.lookup(name));
    }


    private Procedure lookupProcedure(String procedureName)
        throws FusionInterruptedException, FusionException
    {
        return withEvaluator(eval -> {
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
                                      safeWriteToString(eval, proc));
        });
    }


    private Object call(Procedure proc, Object... arguments)
        throws FusionInterruptedException, FusionException
    {
        for (int i = 0; i < arguments.length; i++)
        {
            Object arg = arguments[i];
            Object fv  = myEvaluator.injectMaybe(arg);
            if (fv == null)
            {
                String msg =
                    "Expected injectable Java object but received " +
                    arg.getClass().getName() + " for " +
                    friendlyIndex(i) + " argument";
                throw new IllegalArgumentException(msg);
            }
            arguments[i] = fv;
        }

        // TODO Should this set current_namespace?
        return withEvaluator(eval -> eval.callNonTail(proc, arguments));
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


    @Override
    public void ionize(Object value, IonWriter out)
        throws FusionException
    {
        withEvaluator(eval -> {
            FusionIo.ionize(eval, out, value);
            return null;
        });
    }
}
