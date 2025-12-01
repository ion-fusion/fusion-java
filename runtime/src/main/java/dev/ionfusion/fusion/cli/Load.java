// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.fusion.TopLevel;
import java.io.File;


class Load
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Load and evaluate a script.";
    private static final String HELP_USAGE =
        "load FILE";
    private static final String HELP_BODY =
        "Loads and evaluates the Fusion script in the given FILE. Preceding `require`,\n" +
        "`eval`, and `load` commands can affect the results since they share the same\n" +
        "namespace.\n" +
        "\n" +
        "If the result of the last expression is not void, it is sent to standard output\n" +
        "via `write`.";


    Load()
    {
        super("load");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 1) throw usage();

        String fileName = args[0];
        if (fileName.isEmpty()) throw usage();

        File file = new File(fileName);
        if (! (file.canRead() && file.isFile()))
        {
            throw usage("Script is not a readable file: " + fileName);
        }

        return new Executor(globals, file);
    }


    static class Executor
        extends FusionExecutor
    {
        final File myFile;


        Executor(GlobalOptions globals, File file)
        {
            super(globals);

            myFile = file;
        }


        @Override
        public Object execute(TopLevel top)
            throws Exception
        {
            return top.load(myFile);
        }
    }
}
