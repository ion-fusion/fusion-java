// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionEval.callCurrentEval;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.GlobalState.MODULE;
import static dev.ionfusion.fusion.StandardReader.readSyntax;

import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import dev.ionfusion.runtime.base.ModuleIdentity;
import dev.ionfusion.runtime.base.SourceLocation;
import dev.ionfusion.runtime.base.SourceName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parallel to Racket's load handler.
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
            SourceName name = SourceName.forFile(file);
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
     * If the reader doesn't provide exactly one top-level value, an exception
     * is thrown.
     *
     * @param sourceName may be null.
     */
    private SyntaxSexp readModuleDeclaration(Evaluator eval,
                                             ModuleIdentity id,
                                             SourceName sourceName,
                                             IonReader reader)
        throws FusionException
    {
        if (reader.getType() == null && reader.next() == null)
        {
            String message = "Module source has no top-level forms";
            SyntaxException e =
                new SyntaxException(null /* syntax form */, message);
            e.addContext(SourceLocation.forName(sourceName));
            throw e;
        }

        SyntaxValue firstTopLevel = readSyntax(eval, reader, sourceName);
        if (reader.next() != null)
        {
            String message = "Module source has more than one top-level form";
            SyntaxException e =
                new SyntaxException(null /* syntax form */, message);
            e.addContext(SourceLocation.forCurrentSpan(reader, sourceName));
            throw e;
        }

        try
        {
            SyntaxSexp moduleDeclaration = (SyntaxSexp) firstTopLevel;
            if (moduleDeclaration.size() > 1)
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
        throw new SyntaxException(null /* syntax form */, message, firstTopLevel);
    }


    private SyntaxSexp readModuleDeclaration(Evaluator eval,
                                             ModuleIdentity id,
                                             ModuleLocation loc)
        throws FusionException
    {
        SourceName sourceName = loc.sourceName();
        try
        {
            try (IonReader reader = loc.read(eval))
            {
                return readModuleDeclaration(eval, id, sourceName, reader);
            }
        }
        catch (IOException | IonException e)
        {
            String where =
                (sourceName == null ? id.toString() : sourceName.display());
            String message =
                "Error loading " + where + ": " + e.getMessage();
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
     * Reads module source and declares it in the current namespace's registry.
     * The module is not instantiated.
     */
    private void evalModuleDeclaration(Evaluator eval,
                                       ModuleLocation loc,
                                       SyntaxSexp moduleDeclaration)
        throws FusionException
    {
        moduleDeclaration =
            wrapModuleIdentifierWithKernelBindings(eval, moduleDeclaration);

        Evaluator bodyEval = eval;

        // TODO Jar-bundled modules won't have a directory, so `load` with
        //      relative paths won't be able to access sibling resources.
        String dirPath = loc.parentDirectory();
        if (dirPath != null)
        {
            bodyEval =
                eval.markedContinuation(myCurrentLoadRelativeDirectory,
                                        makeString(eval, dirPath));
            // TODO Should this set other params like current_namespace?
        }

        // TODO Do we need an Evaluator with no continuation marks?

        callCurrentEval(bodyEval, moduleDeclaration);
        // TODO TAIL
    }


    /**
     * Reads module source and declares it in the current namespace's registry.
     * The module is not instantiated.
     */
    void loadModule(Evaluator eval, ModuleIdentity id, ModuleLocation loc)
        throws FusionException
    {
        SyntaxSexp moduleDeclaration = readModuleDeclaration(eval, id, loc);

        try
        {
            evalModuleDeclaration(eval, loc, moduleDeclaration);
        }
        catch (AssertionError e)
        {
            // Attempt to make debugging easier.
            // TODO Similarly wrap other unchecked exceptions?
            // TODO this doesn't need to be rewrapped by each module
            throw new AssertionError("Assertion failure in " + loc + ": ", e);
        }
    }
}
