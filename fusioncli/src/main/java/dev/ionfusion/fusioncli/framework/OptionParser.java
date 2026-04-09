// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

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

public class OptionParser
{
    /**
     * Parse and remove command-line options. Options are defined to start with
     * <code>'--'</code>, and come in three forms:
     * <pre>
     *     --key=value
     *     --key value
     *     --key
     *  </pre>
     * The first two forms are used for all non-boolean option types. The third form is
     * used for boolean options, which are always set to
     * <code>true</code> when present on the command line.
     * <p>
     * Note that no sanity checking is performed on the value, so if the user types:
     * <pre>
     *      --key = value
     *  </pre>
     * The option <code>"key"</code> will have the value <code>"="</code>.
     * <p>
     * The set of options to recognize is determined by the JavaBeans-style properties
     * of the given {@code target}.
     *
     * @param target an object that both defines the applicable options and receives any
     * associated values among the command line {@code args}.  If null, then any options
     * will cause an error.
     *
     * @return a copy of the {@code args} with any options removed.
     */
    public static String[] extractOptions(Object target,
                                          String[] args,
                                          boolean stopAtNonOption)
        throws UsageException
    {
        PropertyDescriptor[] propDescs = findPropertyDescriptors(target);

        List<String> result = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.startsWith("--"))
            {
                int    sep = arg.indexOf("=", 2);
                String key;
                Object value;
                if (sep > 0)
                {
                    key = arg.substring(2, sep);
                    value = arg.substring(sep + 1);
                }
                else
                {
                    key = arg.substring(2);

                    // If option is boolean this is decremented below to push
                    // the value back for the next iteration.
                    i++;

                    if (i < args.length)
                    {
                        value = args[i];
                    }
                    else
                    {
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
                else if (!propClass.equals(String.class))
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


    private static PropertyDescriptor[] findPropertyDescriptors(Object target)
    {
        if (target == null)
        {
            // No properties, so any option will trigger an error.
            return new PropertyDescriptor[0];
        }

        try
        {
            BeanInfo info = Introspector.getBeanInfo(target.getClass());
            return info.getPropertyDescriptors();
        }
        catch (IntrospectionException e)
        {
            throw new AssertionError("Unable to introspect " + target.getClass(), e);
        }
    }


    /**
     * Looks for a writeable property with a given name amongst the given property
     * descriptors.
     *
     * @param propDescs the descriptors to search.
     * @param propName the desired property name.
     *
     * @return the first descriptor with the given name, else null.
     */
    private static PropertyDescriptor seekWriteableProperty(PropertyDescriptor[] propDescs,
                                                            String propName)
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
        Method method = desc.getWriteMethod();
        method.setAccessible(true);

        try
        {
            method.invoke(target, value);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("Cannot access " + method.getName(), e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof UsageException)
            {
                throw (UsageException) cause;
            }

            throw new AssertionError("Error invoking " + method.getName(),
                                     e.getTargetException());
        }
    }
}
