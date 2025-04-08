// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import dev.ionfusion.fusion.TopLevel;


class Eval
    extends Command
{
    //=+===============================================================================
    private static final String HELP_ONE_LINER =
        "Evaluate an inline script.";
    private static final String HELP_USAGE =
        "eval EXPRESSIONS";
    private static final String HELP_BODY =
        "Evaluates the EXPRESSIONS as top-level form(s). Preceding `require`, `eval`, and\n" +
        "`load` commands can affect the results since they share the same namespace.\n" +
        "\n" +
        "If the result of the last expression is not void, it is sent to standard output\n" +
        "via `write`.\n" +
        "\n" +
        "Be careful to follow your shell's quoting and escaping rules.";


    Eval()
    {
        super("eval");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
        throws UsageException
    {
        if (args.length != 1) throw usage();

        String evalText = args[0];
        if (evalText.isEmpty()) throw usage();

        return new Executor(globals, evalText);
    }


    static class Executor
        extends FusionExecutor
    {
        final String myEvalText;


        Executor(GlobalOptions globals, String evalText)
        {
            super(globals);

            myEvalText = evalText;
        }


        @Override
        public Object execute(TopLevel top)
            throws Exception
        {
            return top.eval(myEvalText);
        }
    }
}
