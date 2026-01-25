// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.runtime.embed.TopLevel;


class Require
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Import bindings from a Fusion module.";
    private static final String HELP_USAGE =
        "require MODULE_ID";
    private static final String HELP_BODY =
        "Imports bindings from a module into the top-level namespace, equivalent to\n" +
        "evaluating the expression `(require MODULE_ID)`.\n" +
        "\n" +
        "In general, this command will be followed by others that make use of the\n" +
        "imported features. For example:\n" +
        "\n" +
        "    $ fusion require /fusion/struct ';' \\\n" +
        "      eval '(define s (mutable_struct \"a\" 1)) (put_m s \"modified\" true) s'\n" +
        "\n" +
        "    {a:1,modified:true}\n" +
        "\n" +
        "Be careful to follow your shell's quoting and escaping rules.";


    Require()
    {
        super("require");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 1) throw usage();

        String moduleName = args[0];
        if (moduleName.isEmpty()) throw usage();

        return new Executor(globals, moduleName);
    }


    static class Executor
        extends FusionExecutor
    {
        final String myModulePath;


        Executor(GlobalOptions globals, String modulePath)
        {
            super(globals);

            myModulePath = modulePath;
        }


        @Override
        public Object execute(TopLevel top)
            throws Exception
        {
            top.requireModule(myModulePath);
            return null;
        }
    }
}
