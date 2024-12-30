// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.fusion.FusionException;
import dev.ionfusion.fusion.FusionRuntime;
import dev.ionfusion.fusion.FusionRuntimeBuilder;
import dev.ionfusion.fusion._Private_Trampoline;
import com.amazon.ion.IonException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Stores options and data shared by the entire execution sequence.
 * <p>
 * The properties here are set via reflection by {@link Command#extractOptions}.
 */
final class GlobalOptions
{
    static final String HELP =
        "\n\n"
        + "Global Options\n"
        + "==============\n"
        + "\n"
        + "--bootstrapRepository DIR\n"
        + "\n"
        + "  DEPRECATED: a bootstrap repository is no longer needed.\n"
        + "  If used, the DIR is instead treated as the first user repository.\n"
        + "\n"
        + "--repositories DIR" + File.pathSeparator + "DIR...\n"
        + "\n"
        + "  Repositories of Fusion modules and resources. This option can be given more\n"
        + "  than once and all DIRs will be used.\n"
        + "\n"
        + "--catalogs CATALOG" + File.pathSeparator + "CATALOG...\n"
        + "\n"
        + "  Sources of Ion shared symbol tables. When a CATALOG is a real file, it must\n"
        + "  contain only serialized shared symbol tables. When a CATALOG is a directory,\n"
        + "  it is traversed recursively. This option can be given more than once and all\n"
        + "  CATALOGs will be used.\n"
        ;


    private final InputStream myStdin;
    private final PrintStream myStdout;
    private final PrintStream myStderr;

    private String          myBootstrapPath;
    private ArrayList<File> myRepositories;
    private ArrayList<File> myCatalogs;
    private boolean         myDocsEnabled;
    private FusionRuntime   myRuntime;


    GlobalOptions(InputStream stdin, PrintStream stdout, PrintStream stderr)
    {
        myStdin  = stdin;
        myStdout = stdout;
        myStderr = stderr;
    }

    InputStream stdin()
    {
        return myStdin;
    }

    PrintStream stdout()
    {
        return myStdout;
    }

    PrintStream stderr()
    {
        return myStderr;
    }


    public void setBootstrapRepository(String path)
    {
        myStderr.println("WARNING: The --boostrapRepository option is deprecated.");
        myBootstrapPath = path;
    }


    private static void addFiles(ArrayList<File> files, String pathList)
    {
        StringTokenizer tokenizer =
            new StringTokenizer(pathList, File.pathSeparator);

        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken().trim();

            if (!token.isEmpty())
            {
                File file = new File(token);
                files.add(file);
            }
        }
    }


    public void setRepositories(String pathList)
    {
        if (myRepositories == null) myRepositories = new ArrayList<>();

        addFiles(myRepositories, pathList);
    }


    public void setCatalogs(String pathList)
    {
        if (myCatalogs == null) myCatalogs = new ArrayList<>();

        addFiles(myCatalogs, pathList);
    }


    void collectDocumentation()
    {
        myDocsEnabled = true;
    }


    FusionRuntimeBuilder runtimeBuilder()
        throws UsageException
    {
        FusionRuntimeBuilder builder = FusionRuntimeBuilder.standard();

        // TODO The CLI should not allow setting a bootstrap repo.
        //   For now, we assume this option could be in use where the given repo
        //   has both the bootstrap and user code, so we treat this as if it
        //   were the first user repo.

        if (myBootstrapPath != null)
        {
            File dir = new File(myBootstrapPath);

            try
            {
                builder.addRepositoryDirectory(dir);
            }
            catch (IllegalArgumentException e)
            {
                String message = "bootstrapRepository: " + e.getMessage();
                throw new UsageException(message);
            }
        }

        if (myRepositories != null)
        {
            try
            {
                for (File repo : myRepositories)
                {
                    builder.addRepositoryDirectory(repo);
                }
            }
            catch (IllegalArgumentException e)
            {
                String message = "repositories: " + e.getMessage();
                throw new UsageException(message);
            }
        }

        if (myCatalogs != null)
        {
            try
            {
                IonCatalogLoader loader = new IonCatalogLoader();
                loader.loadFiles(myCatalogs);

                builder.withDefaultIonCatalog(loader.getCatalog());
            }
            catch (IOException | IonException e)
            {
                String message = "catalogs: " + e.getMessage();
                throw new UsageException(message);
            }
        }

        builder.setInitialCurrentOutputPort(myStdout);

        _Private_Trampoline.setDocumenting(builder, myDocsEnabled);

        return builder;
    }

    FusionRuntime runtime()
        throws FusionException, UsageException
    {
        // Lazily loading a runtime that can be global across commands.
        if (myRuntime == null)
        {
            FusionRuntimeBuilder builder = runtimeBuilder();
            myRuntime = builder.build();
        }
        return myRuntime;
    }
}
