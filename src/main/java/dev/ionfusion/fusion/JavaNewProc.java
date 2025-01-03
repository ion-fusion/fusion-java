// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionText.checkNonEmptyTextArg;
import static java.util.Arrays.copyOfRange;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 *
 */
final class JavaNewProc
    extends Procedure
{
    @Override
    Object doApply(Evaluator eval, Object[] args)
        throws FusionException
    {
        checkArityAtLeast(1, args);
        String className = checkNonEmptyTextArg(eval, this, 0, args);

        Class<?> klass = determineClass(className);

        Object[] constructorArgs = copyOfRange(args, 1, args.length);
        Constructor<?> constructor =
            determineConstructor(klass, constructorArgs);

        return callConstructor(constructor, constructorArgs);
    }


    private Class<?> determineClass(String className)
        throws FusionException
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw contractFailure("Java class isn't found: " + className, e);
        }
    }


    private Constructor<?> determineConstructor(Class<?> klass,
                                                Object[] args)
        throws FusionException
    {
        Class<?>[] parameterTypes = new Class[args.length];
        Arrays.fill(parameterTypes, Object.class);

        try
        {
            // Class.getConstructor() only returns public constructors
            return klass.getDeclaredConstructor(parameterTypes);
        }
        catch (SecurityException e)
        {
            String message =
                "The Java security manager denied access to " + klass;
            throw contractFailure(message, e);
        }
        catch (NoSuchMethodException e)
        {
            String message;
            if (klass.isMemberClass() && ! Modifier.isStatic(klass.getModifiers()))
            {
                message = klass + " is a member class but is not static";
            }
            else if (args.length == 0)
            {
                message = klass + " doesn't have a no-arg constructor";
            }
            else
            {
                message = klass + " doesn't have a constructor accepting " +
                          args.length + " Object args";
            }
            throw contractFailure(message, e);
        }
    }


    private Object callConstructor(Constructor<?> constructor, Object[] args)
        throws FusionException
    {
        try
        {
            return constructor.newInstance(args);
        }
        catch (IllegalAccessException e)
        {
            String message =
                "Java constructor isn't accessible: " + constructor;
            throw contractFailure(message, e);
        }
        catch (IllegalArgumentException e)
        {
            String message =
                "Illegal argument to Java constructor: " + constructor +
                e.getMessage();
            throw contractFailure(message, e);
        }
        catch (InstantiationException e)
        {
            String message =
                "Class cannot be instantiated by constructor: " + constructor;
            throw contractFailure(message, e);
        }
        catch (InvocationTargetException e)
        {
            String message =
                "Exception thrown from Java constructor: " + constructor;
            throw contractFailure(message, e.getCause());
        }
    }
}
