// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionEval.callCurrentEval;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.GlobalState.MODULE;
import static dev.ionfusion.fusion.StandardReader.readSyntax;
import static dev.ionfusion.fusion.SyntaxException.makeSyntaxError;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import dev.ionfusion.fusion.Evaluator.Thunk;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.ResourceDescriptor;
import dev.ionfusion.runtime.base.ResourceIdentifier;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Parallel to Racket's default load handler, usually {@code (current-load)}.
 */
final class LoadHandler
{
    private final FileSystemSpecialist myFileSystem;
    private final DynamicParameter     myCurrentLoadRelativeDirectory;

    LoadHandler(FileSystemSpecialist fileSystemSpecialist,
                DynamicParameter     currentLoadRelativeDirectory)
    {
        myFileSystem                   = fileSystemSpecialist;
        myCurrentLoadRelativeDirectory = currentLoadRelativeDirectory;
    }


    /**
     * Reads top-level syntax forms from a file, evaluating each in sequence.
     * The file is resolved relative to {@code current_directory}.
     * Loading is parameterized to set {@code current_load_relative_directory}
     * to the parent of the resolved path.
     *
     * @param eval
     * @param namespace may be null, to use
     *   {@link Evaluator#findCurrentNamespace()}.
     * @param path the file to read; may be relative, in which case it is
     * resolved relative to the {@code current_directory} parameter.

     * @return the value of the last form in the file, or null if the file
     * contains no forms.
     */
    Object loadTopLevel(Evaluator eval, Namespace namespace, String path)
        throws FusionException
    {
        File file = myFileSystem.resolvePath(eval, "load", path);
        File parent = file.getParentFile();

        // TODO this shouldn't be done in the standard load handler.
        // It should done in `load` (etc) before calling here.
        eval = eval.markedContinuation(myCurrentLoadRelativeDirectory,
                                       makeString(eval, parent.getAbsolutePath()));

        try (InputStream in = myFileSystem.openInputFile(eval, "load", file))
        {
            ResourceDescriptor name = SourceName.forFile(file);
            Object result = null;

            try (IonReader reader = eval.getIonReaderBuilder().build(in))
            {
                while (reader.next() != null)
                {
                    result = null;  // Don't hold onto garbage
                    SyntaxValue fileExpr = readSyntax(eval, reader, name);
                    result = FusionEval.eval(eval, fileExpr, namespace);
                    // TODO TAIL
                }
            }

            return result;
        }
        catch (IOException | IonException e)
        {
            String message =
                "Error loading file " + file + ": " + e.getMessage();
            throw new FusionException(message, e);
        }
    }


    /**
     * If the reader is positioned on a value, it will be read; otherwise the
     * {@linkplain IonReader#next() next} value will be read.
     * <p>
     * If the reader doesn't provide exactly one top-level value, an exception is
     * thrown.
     *
     * @param desc must not be null.
     */
    private SyntaxSexp readModuleDeclaration(Evaluator eval,
                                             IonReader reader,
                                             ResourceDescriptor desc,
                                             ModuleIdentity id)
        throws FusionException
    {
        assert desc != null;

        if (reader.getType() == null && reader.next() == null)
        {
            String message = "Module source has no top-level forms";
            SyntaxException e = makeSyntaxError(message);
            e.addContext(SourceLocation.forName(desc));
            throw e;
        }

        SyntaxValue firstTopLevel = readSyntax(eval, reader, desc);
        if (reader.next() != null)
        {
            String message = "Module source has more than one top-level form";
            SyntaxException e = makeSyntaxError(message);
            e.addContext(SourceLocation.forCurrentSpan(reader, desc));
            throw e;
        }

        try
        {
            SyntaxSexp moduleDeclaration = (SyntaxSexp) firstTopLevel;
            if (moduleDeclaration.size(eval) > 1)
            {
                SyntaxSymbol moduleSym = (SyntaxSymbol)
                    moduleDeclaration.get(eval, 0);
                if (MODULE.equals(moduleSym.stringValue()))
                {
                    return moduleDeclaration;
                }
            }
        }
        catch (ClassCastException e) { /* fall through */ }

        String message = "Top-level form isn't (module ...)";
        throw makeSyntaxError(eval, null /* syntax form */, message, firstTopLevel);
    }


    /**
     * @param desc must not be null.
     */
    private SyntaxSexp readModuleDeclaration(Evaluator eval,
                                             Thunk<IonReader> readerMaker,
                                             ResourceDescriptor desc,
                                             ModuleIdentity id)
        throws FusionException
    {
        assert desc != null;
        try (IonReader reader = readerMaker.eval(eval))
        {
            return readModuleDeclaration(eval, reader, desc, id);
        }
        catch (IOException | IonException e)
        {
            String message = "Error loading " + id + " from " + desc.display() + ": " +
                             e.getMessage();
            throw new FusionException(message, e);
        }
    }


    private SyntaxSexp
    wrapModuleIdentifierWithKernelBindings(Evaluator eval,
                                           SyntaxSexp moduleStx)
        throws FusionException
    {
        SyntaxValue[] children = moduleStx.extract(eval);

        // We already verified this type-safety
        SyntaxSymbol moduleSym = (SyntaxSymbol) children[0];
        assert moduleSym.stringValue().equals(MODULE);

        children[0] =
            moduleSym.copyReplacingBinding(eval.getGlobalState().myKernelModuleBinding);

        return moduleStx.copyReplacingChildren(eval, children);
    }


    /**
     * Declares a module in the current namespace's registry. The
     * module is not instantiated. Its identity is determined by the
     * current-module-declare-name parameter.
     *
     * @param rsrcId determines {@code current_load_relative_directory}; can be null.
     */
    private void evalModuleDeclaration(Evaluator eval,
                                       ResourceIdentifier rsrcId,
                                       SyntaxSexp moduleDeclaration)
        throws FusionException
    {
        moduleDeclaration =
            wrapModuleIdentifierWithKernelBindings(eval, moduleDeclaration);

        Evaluator bodyEval = eval;

        // TODO Jar-bundled modules won't have a directory, so `load` with
        //      relative paths won't be able to access sibling resources.
        if (rsrcId != null)
        {
            Path srcPath = rsrcId.getPath();
            if (srcPath != null)
            {
                String dirPath = srcPath.getParent().toAbsolutePath().toString();
                bodyEval = eval.markedContinuation(myCurrentLoadRelativeDirectory,
                                                   makeString(eval, dirPath));
                // TODO Should this set other params like current_namespace?
            }
        }

        // TODO Do we need an Evaluator with no continuation marks?

        callCurrentEval(bodyEval, moduleDeclaration);
        // TODO TAIL
    }


    /**
     * Reads a {@code module} form and declares it in the current namespace's registry.
     * The module is not instantiated.
     *
     * @param desc must not be null.
     */
    void loadModule(Evaluator eval,
                    Thunk<IonReader> reader,
                    ResourceDescriptor desc,
                    ModuleIdentity id)
        throws FusionException
    {
        ResourceIdentifier rsrcId = desc.getResourceId();

        SyntaxSexp decl = readModuleDeclaration(eval, reader, desc, id);
        assert decl.getLocation().getResourceDesc().getResourceId() == rsrcId;
        try
        {
            evalModuleDeclaration(eval, rsrcId, decl);
        }
        catch (AssertionError e)
        {
            // Attempt to make debugging easier.
            // TODO Similarly wrap other unchecked exceptions?
            // TODO this doesn't need to be rewrapped by each module
            String message =
                "Assertion failure loading " + id + " from " + desc.display() + ": ";
            throw new AssertionError(message, e);
        }
    }
}
