// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.repl.cmd;

import dev.ionfusion.fusioncli.TablePrinter;
import dev.ionfusion.fusioncli.framework.Command;
import dev.ionfusion.fusioncli.framework.Executor;
import dev.ionfusion.fusioncli.framework.UsageException;
import dev.ionfusion.fusioncli.repl.ReplContext;
import dev.ionfusion.fusioncli.repl.ReplExecutor;
import java.io.IOException;
import java.io.PrintWriter;

public class ReplHelpCmd
    extends Command<ReplContext>
{
    static final String HELP_ONE_LINER =
        "Describe available REPL commands";

    private static final String HELP_USAGE =
        "help [COMMAND]";

    private static final String HELP_BODY =
        "With no argument, print the complete list of commands along with brief\n" +
        "descriptions.  If a command is given, print its documentation.";


    public ReplHelpCmd()
    {
        super("help", "h", "?");
        putHelpText(HELP_ONE_LINER, HELP_USAGE, HELP_BODY);
    }


    @Override
    public Executor makeExecutor(ReplContext context, String[] args)
        throws UsageException
    {
        // The RepLoop only splits out the first word
        assert args.length <= 1;
        String commandName = null;
        if (args.length == 1)
        {
            commandName = args[0];
            if (commandName.split(" ").length != 1)
            {
                throw usage("Expected at most one command name");
            }
        }

        return new HelpExecutor(context, commandName);
    }


    //==================================================================================


    private class HelpExecutor
        extends ReplExecutor
    {
        private final String myCommandName;

        private HelpExecutor(ReplContext context, String commandName)
        {
            super(context);
            myCommandName = commandName;
        }


        @Override
        public int execute()
            throws Exception
        {
            if (myCommandName != null)
            {
                Command<?> cmd = context().commandSuite().getMatchingCommand(myCommandName);
                if (cmd != null)
                {
                    printCommandHelp(cmd);
                    return 0;
                }

                context().errorln("Unknown command: '" + myCommandName + "'");
            }

            printAllCommandSummaries();
            return 0;
        }


        private void printCommandHelp(Command<?> command)
        {
            PrintWriter out = context().out();

            String oneLiner = command.getHelpOneLiner();
            if (oneLiner != null)
            {
                printCommandAndAliases(command);
                out.print(":  ");
                out.println(oneLiner);

                out.println();

                out.print("Usage: ,");
                out.println(command.getHelpUsage());
            }

            String helpBody = command.getHelpBody();
            if (helpBody != null)
            {
                out.println();
                out.println(helpBody);
            }
        }


        private void printCommandAndAliases(Command<?> command)
        {
            PrintWriter out = context().out();

            out.append(command.getName());

            String[] aliases = command.getAliases();
            int len = aliases.length;
            if (len != 0)
            {
                out.append(" (");
                for (int i = 0; i < len; i++ )
                {
                    if (i != 0) out.append(' ');
                    out.append(aliases[i]);
                }
                out.append(")");
            }
        }


        private void printAllCommandSummaries()
            throws IOException
        {
            PrintWriter out = context().out();

            out.println("Available commands:");

            TablePrinter table = new TablePrinter();
            table.setIndent(2);

            for (Command<?> command : context().commandSuite().getAllCommands())
            {
                String oneLiner = command.getHelpOneLiner();
                if (oneLiner != null)
                {
                    String[] row = { command.getName(), oneLiner };

                    table.addRow(row);
                }
            }

            table.render(out);

            out.println("Use `,help COMMAND` for more information on a specific command.");
        }
    }
}
