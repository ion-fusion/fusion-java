// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion.cli;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class Command
{
    interface Executor
    {
        /**
         * Execute this command.  Any exception that's thrown will cause the
         * CLI to display the stack trace to {@link System#err}.
         *
         * @return zero to indicate success, any other number to indicate an
         *  error code.
         */
        int execute()
            throws Exception;
    }


    private final String   myCommand;
    private final String[] myAliases;

    private String myHelpOneLiner = null;
    private String myHelpUsage = "your guess is as good as mine!";
    private String myHelpBody = "Sorry, this command is undocumented.\n";


    //=========================================================================
    // Construction and Initialization

    Command(String command, String... aliases)
    {
        myCommand = command;
        myAliases = (aliases == null ? new String[0] : aliases);
    }


    /**
     * Sets the help text for this command.
     * <p>
     * Nomenclature: this is called "put" to hide it from the
     * BeanUtils.copyProperty call in extractOptions
     *
     * @param oneLiner must be non-empty. It should start with a verb and end
     * with a period.
     * @param usage must be non-empty, and must start with the primary command.
     * @param body must be non-empty.  It should not be indented, and it must
     * not end with a newline.  It should be (explicitly) wrapped to display
     * within 80 columns.
     */
    void putHelpText(String oneLiner, String usage, String body)
    {
        assert oneLiner != null && !oneLiner.isEmpty();
        assert usage != null && usage.startsWith(myCommand);
        assert body != null && !body.endsWith("\n");

        myHelpOneLiner = oneLiner;
        myHelpUsage    = usage;
        myHelpBody     = body;
    }


    //=========================================================================
    // Property Accessors

    String getCommand()
    {
        return myCommand;
    }

    /**
     * Gets the aliases for this command, generally shortened forms of
     * {@link #getCommand}.
     *
     * @return the array of aliases, not null.
     */
    String[] getAliases()
    {
        return myAliases;
    }

    /**
     * If null, the command will not be listed by `help`.
     */
    String getHelpOneLiner()
    {
        return myHelpOneLiner;
    }

    String getHelpUsage()
    {
        return myHelpUsage;
    }

    String getHelpBody()
    {
        return myHelpBody;
    }


    //=========================================================================
    // Option parsing

    /**
     * Parse and remove command-line options.
     * Options are defined to start with <code>'--'</code>, and come in three
     * forms:
     * <pre>
     *     --key=value
     *     --key value
     *     --key
     *  </pre>
     *  The first two forms are used for all non-boolean option types.
     *  The third form is used for boolean options, which are always set to
     *  <code>true</code> when present on the command line.
     *  <p>
     *  Note that no sanity checking is performed on the value, so if the
     *  user types:
     *  <pre>
     *      --key = value
     *  </pre>
     *   The option <code>"key"</code> will have the value <code>"="</code>.
     * <p>
     * The set of options to recognize is determined by the JavaBeans-style
     * properties of the given {@code target}.
     *
     * @param target an object that both defines the applicable options and
     * receives any associated values among the command line {@code args}.
     *
     * @return a copy of the {@code args} with any options removed.
     */
    static String[] extractOptions(Object target, String[] args,
                                   boolean stopAtNonOption)
        throws UsageException
    {
        PropertyDescriptor[] propDescs;
        try
        {
            BeanInfo info = Introspector.getBeanInfo(target.getClass());
            propDescs = info.getPropertyDescriptors();
        }
        catch (IntrospectionException e)
        {
            String message =
                "Internal error: unable to introspect " + target.getClass();
            throw new RuntimeException(message, e);
        }

        List<String> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith("--"))
            {
                int sep = arg.indexOf("=", 2);
                String key;
                Object value;
                if (sep > 0)
                {
                    key = arg.substring(2, sep);
                    value = arg.substring(sep+1);
                }
                else
                {
                    key = arg.substring(2);

                    // If option is boolean this is decremented below to push
                    // the value back for the next iteration.
                    i++;

                    if (i < args.length) {
                        value = args[i];
                    } else {
                        value = null;
                    }
                }

                PropertyDescriptor desc = seekWriteableProperty(propDescs, key);
                if (desc == null)
                {
                    throw new UsageException("Invalid option: " + arg);
                }

                Class<?> propClass = desc.getPropertyType();
                if (propClass.equals(Boolean.TYPE))
                {
                    // Boolean property
                    if (sep < 0)
                    {
                        // Push the separate value back, we don't want it.
                        i--;
                        value = Boolean.TRUE;
                    }
                    else
                    {
                        throw new UsageException("Erroneous argument: " + arg);
                    }
                }
                else if (value == null)
                {
                    throw new UsageException("Missing argument: " + arg);
                }
                else if (propClass.equals(Path.class))
                {
                    // TODO handle InvalidPathException (etc?)
                    value = Paths.get(value.toString());
                }
                else if (! propClass.equals(String.class))
                {
                    throw new UsageException("Invalid option: " + arg);
                }

                setOption(target, desc, key, value);
            }
            else if (stopAtNonOption)
            {
                // arg is not an option, and we shouldn't go any further
                return Arrays.copyOfRange(args, i, args.length);
            }
            else
            {
                // Not stopping at a "real" argument; keep looking for options.
                result.add(arg);
            }
        }

        return result.toArray(new String[0]);
    }


    /**
     * Looks for a writeable property with a given name amongst the given
     * property descriptors.
     *
     * @param propDescs the descriptors to search.
     * @param propName the desired property name.
     *
     * @return the first descriptor with the given name, else null.
     */
    private static PropertyDescriptor
    seekWriteableProperty(PropertyDescriptor[] propDescs, String propName)
    {
        for (PropertyDescriptor descriptor : propDescs)
        {
            String descriptorName = descriptor.getName();
            if (descriptorName.equals(propName))
            {
                // Ensure that there's an accessible setter.
                Method setter = descriptor.getWriteMethod();
                if (setter != null)
                {
                    return descriptor;
                }
            }
        }

        return null;
    }


    private static void setOption(Object target,
                                  PropertyDescriptor desc,
                                  String option,
                                  Object value)
        throws UsageException
    {
        Method setter = desc.getWriteMethod();

        try
        {
            setter.invoke(target, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof UsageException)
            {
                throw (UsageException) cause;
            }

            String message = "Internal error: " + cause.getMessage();
            throw new RuntimeException(message, cause);
        }
    }


    //=========================================================================
    // CLI Argument Processing Methods

    /**
     * Verify that the command string used to invoke the command matches
     * the command or one of its aliases.
     */
    boolean matches(String command)
    {
        if (getCommand().equals(command)) return true;
        for (String alias : myAliases)
        {
            if (alias.equals(command)) return true;
        }
        return false;
    }


    /**
     * Create a new object to receive command-specific options via injection.
     * Subclasses should override this if they have options.
     *
     * @param globals the populated global options.
     *
     * @return null if there are no command options.
     */
    Object makeOptions(GlobalOptions globals)
    {
        return null;
    }


    /**
     * Perform pre-processing, including in particular argument processing.
     * A null result causes the framework to emit command-specific usage help.
     *
     * @param args to process
     *
     * @return an {@link Executor} to execute the command; null if there are
     * usage errors.
     *
     * @throws UsageException if there are
     * command-line errors preventing the command from being used.
     */
    Executor prepare(GlobalOptions globals, String[] args)
        throws UsageException
    {
        Object options = makeOptions(globals);

        // Even if the command doesn't define options, we still need to look
        // for them on the command line. This "options object" has no setters
        // so any options we find will cause an error.
        if (options == null) options = new Object();

        args = extractOptions(options, args, true);

        return makeExecutor(globals, options, args);
    }


    Executor makeExecutor(GlobalOptions globals,
                          Object        options,
                          String[]      arguments)
        throws UsageException
    {
        return makeExecutor(globals, arguments);
    }


    Executor makeExecutor(GlobalOptions globals,
                          String[]      arguments)
        throws UsageException
    {
        return makeExecutor(arguments);
    }


    /**
     * Parses the command-line arguments to build a {@link Executor} for
     * execution.
     * Note that any options (<em>i.e.</em>, arguments prefixed by
     * {@code "--"}) will have already been extracted from the
     * {@code arguments} array.
     *
     * @param arguments to parse
     * @return null if the arguments are inappropriate or insufficient.
     */
    Executor makeExecutor(String[] arguments)
        throws UsageException
    {
        return null;
    }


    UsageException usage()
    {
        return new UsageException(this, null);
    }

    UsageException usage(String message)
    {
        return new UsageException(this, message);
    }
}
