// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import java.io.IOException;
import java.io.PrintWriter;

class Help
    extends Command
{
    static final String HELP_ONE_LINER =
        "Describe the usage of this program or its commands.";

    private static final String HELP_USAGE =
        "help [COMMAND ...]";

    private static final String HELP_BODY =
        "With no arguments, print the complete list of commands along with brief\n" +
        "descriptions.  If commands are specified, print the full help information\n" +
        "for each one.";

    final static String APP_HELP_TEXT_INTRO =
        "Usage: fusion [OPTIONS ...] <command> [ARGS ...] [; <command> [ARGS ...]] ...\n" +
        "Type 'fusion help <command>' for help on a specific command.\n\n" +
        "Available commands:\n";

    final static String APP_HELP_TEXT_BODY =
        "\n" +
        "Multiple commands can be provided in the same invocation, separated by the\n" +
        "semicolon character. In most shells this character will need escaping:\n" +
        "\n" +
        "    $ fusion require /fusion/io \\; load myscript.fusion\n" +
        "\n" +
        "All commands are evaluated, in order, in the same top-level namespace. Thus the\n" +
        "`require` command above affects the subsequent `load`.";

    private final static String FULL_HELP_SEPARATOR =
        "\n                                *      *      *\n\n";


    //=========================================================================
    // Constructors

    Help()
    {
        super("help", "?", "h");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    //=========================================================================


    @Override
    Executor makeExecutor(GlobalOptions globals, String[] args)
    {
        return new Executor(globals, args);
    }


    private static class Executor
        extends StdioExecutor
    {
        private final String[] myCommands;

        private Executor(GlobalOptions globals, String[] commands)
        {
            super(globals);

            myCommands = commands;
        }


        @Override
        public int execute(PrintWriter out, PrintWriter err)
            throws UsageException, IOException
        {
            out.println();

            if ((myCommands == null) || (myCommands.length == 0))
            {
                renderGeneralHelp(out);
            }
            else
            {
                renderCommands(out);
            }

            return 0;
        }


        private void renderGeneralHelp(Appendable out)
            throws IOException
        {
            out.append(APP_HELP_TEXT_INTRO);

            TablePrinter table = new TablePrinter();
            table.setIndent(2);

            Command[] allCommands = CommandFactory.getAllCommands();
            for (Command command : allCommands)
            {
                String oneLiner = command.getHelpOneLiner();
                if (oneLiner != null)
                {
                    String[] row = { command.getCommand(), oneLiner };

                    table.addRow(row);
                }
            }

            table.render(out);

            out.append(APP_HELP_TEXT_BODY);
            out.append(GlobalOptions.HELP);
        }


        private void renderCommands(Appendable out)
            throws UsageException, IOException
        {
            boolean showGeneralUsage = false;

            for (int i = 0; i < myCommands.length; i++)
            {
                if (i != 0)
                {
                    out.append(FULL_HELP_SEPARATOR);
                }

                String command = myCommands[i];

                Command commandObj =
                    CommandFactory.getMatchingCommand(command);
                if (commandObj == null)
                {
                    out.append("Unknown command: '");
                    out.append(command);
                    out.append("'\n");
                    showGeneralUsage = true;
                }
                else
                {
                    renderFullHelp(commandObj, out);
                }
            }

            if (showGeneralUsage)
            {
                throw new UsageException(null);
            }
        }


        private void renderCommandAndAliases(Command command, Appendable out)
            throws IOException
        {
            out.append(command.getCommand());

            String[] aliases = command.getAliases();
            int len = aliases.length;
            if (len != 0)
            {
                out.append(" (");
                for (int i = 0; i < len; i++ )
                {
                    if (i != 0) out.append(", ");
                    out.append(aliases[i]);
                }
                out.append(")");
            }
        }


        private void renderFullHelp(Command command, Appendable out)
            throws IOException
        {
            String oneLiner = command.getHelpOneLiner();
            if (oneLiner != null)
            {
                renderCommandAndAliases(command, out);
                out.append('\n');
                //            out.print(": ");
                out.append("  ");
                out.append(oneLiner);
                out.append("\n\n");
                out.append("Usage: ");
                out.append(command.getHelpUsage());
                out.append('\n');
            }

            String helpBody = command.getHelpBody();
            if (helpBody != null)
            {
                out.append('\n');
                out.append(helpBody);
                out.append('\n');
            }
        }
    }
}
