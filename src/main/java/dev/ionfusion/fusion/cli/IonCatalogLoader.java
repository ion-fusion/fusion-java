// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import static com.amazon.ion.SystemSymbols.ION_SHARED_SYMBOL_TABLE;
import dev.ionfusion.fusion.SourceLocation;
import dev.ionfusion.fusion.SourceName;
import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonType;
import com.amazon.ion.SymbolTable;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.system.SimpleCatalog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Utility for loading files into a {@link SimpleCatalog}.
 * <p>
 * NOT FOR APPLICATION USE.
 */
final class IonCatalogLoader
{
    private final IonSystem        mySystem;
    private final SimpleCatalog    myCatalog;
    private final IonReaderBuilder myReaderBuilder;

    IonCatalogLoader()
    {
        mySystem = IonSystemBuilder.standard().build();
        myCatalog = new SimpleCatalog();
        myReaderBuilder = IonReaderBuilder.standard().withCatalog(myCatalog);
    }


    SimpleCatalog getCatalog()
    {
        return myCatalog;
    }


    private static boolean hasSharedSymtabAnnotation(IonReader reader)
    {
        for (String ann : reader.getTypeAnnotations())
        {
            if (ION_SHARED_SYMBOL_TABLE.equals(ann))
            {
                return true;
            }
        }
        return false;
    }


    private String prefix(SourceLocation loc)
    {
        return "Error reading symbol table at " + loc.display() + ":\n";
    }


    void loadSymtab(SourceName name, IonReader reader)
    {
        SourceLocation loc = SourceLocation.forCurrentSpan(reader, name);

        if (reader.getType() != IonType.STRUCT)
        {
            String message = prefix(loc)
                + "Expected " + ION_SHARED_SYMBOL_TABLE + " struct, "
                + "found " + reader.getType().name().toLowerCase();
            throw new IonException(message);
        }

        // Enforce the Ion spec regarding this annotation.
        // TODO This should be checked by Ion, but it's not.
        if (! hasSharedSymtabAnnotation(reader))
        {
            String message = prefix(loc)
                + "Expected " + ION_SHARED_SYMBOL_TABLE + " annotation "
                + "on struct, but it is missing";
            throw new IonException(message);
        }

        try
        {
            SymbolTable symtab = mySystem.newSharedSymbolTable(reader, true);

            // TODO Check for duplicates?
            myCatalog.putTable(symtab);
        }
        catch (IonException e)
        {
            // We use the location of the symtab as a whole, because when things
            // fail the IonReader's cursor is usually in between values and thus
            // there's no current span.
            String message = prefix(loc) + e.getMessage();
            throw new IonException(message, e);
        }
    }


    private void loadFile(File file)
        throws IOException
    {
        SourceName name = SourceName.forFile(file.getPath());

        try (FileInputStream stream = new FileInputStream(file);
             IonReader reader = myReaderBuilder.build(stream))
        {
            while (reader.next() != null)
            {
                loadSymtab(name, reader);
            }
        }
    }

    private void loadDirectory(File dir)
        throws IOException
    {
        for (File file : dir.listFiles())
        {
            loadFileOrDirectory(file);
        }
    }

    void loadFileOrDirectory(File file)
        throws IOException
    {
        if (file.isDirectory())
        {
            loadDirectory(file);
        }
        else
        {
            loadFile(file);
        }
    }

    void loadFiles(List<File> catalogFiles)
        throws IOException
    {
        for (File file : catalogFiles)
        {
            loadFileOrDirectory(file);
        }
    }
}
