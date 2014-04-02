// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion.cli;

import static com.amazon.fusion.FusionIo.write;
import static com.amazon.fusion.FusionVoid.isVoid;
import com.amazon.fusion.ExitException;
import com.amazon.fusion.FusionException;
import com.amazon.fusion.TopLevel;
import java.io.File;
import java.io.IOException;


class Load
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Load and evaluate a script.";
    private static final String HELP_USAGE =
        "load FILE";
    private static final String HELP_BODY =
        "Loads and evaluates the Fusion script in the given FILE.  If the result of the\n" +
        "last expression is not void, it is sent to standard output via `write`.";

    Load(String command)
    {
        super(command);
    }

    Load()
    {
        this("load");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }

    @Override
    Executor processArguments(String[] args)
    {
        if (args.length != 1) return null;

        String fileName = args[0];
        if (fileName.length() == 0) return null;

        return new Executor(fileName);
    }


    static class Executor
        extends FusionExecutor
    {
        final String myFileName;


        Executor(String fileName)
        {
            super(/* documenting */ false);

            myFileName = fileName;
        }


        @Override
        public int execute()
            throws Exception
        {
            TopLevel top = runtime().getDefaultTopLevel();

            try
            {
                Object result = loadFile(top, myFileName);
                if (result instanceof Object[])
                {
                    Object[] results = (Object[]) result;
                    for (Object r : results)
                    {
                        write(top, r, System.out);
                        System.out.println();
                    }
                }
                else if (result != null && ! isVoid(top, result))
                {
                    write(top, result, System.out);
                    System.out.println();
                }

                System.out.flush();
            }
            catch (ExitException e)
            {
                // Do nothing, just return successfully.
            }
            catch (FusionException e)
            {
                // TODO optionally display the stack trace
                System.err.println(e.getMessage());
                return 1;
            }

            return 0;
        }


        /**
         * @return may be null (when no values are returned) or an
         * {@code Object[]} (when multiple values are returned).
         */
        private Object loadFile(TopLevel top, String fileName)
            throws FusionException, IOException
        {
            File file = new File(fileName);
            return top.load(file);
        }
    }
}
