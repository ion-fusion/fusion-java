// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Reflection utilities for testing.
 *
 * In general, all reflection problems are wrapped in {@link AssertionError}.
 */
public class Reflect
{
    public static PropertyDescriptor[] getPropertyDescriptors(Object o)
    {
        try
        {
            return Introspector.getBeanInfo(o.getClass()).getPropertyDescriptors();
        }
        catch (IntrospectionException e)
        {
            throw new AssertionError(e);
        }
    }


    public static Method getMethod(Object o, String name, Class<?>... argTypes)
    {
        Class<?> c = o.getClass();
        try
        {
            return c.getMethod(name, argTypes);
        }
        catch (NoSuchMethodException e)
        {
            throw new AssertionError(e);
        }
    }


    public static Method getterFor(Object o, String property, Class<?> type)
    {
        String prefix = (type == Boolean.class ? "is" : "get");
        return getMethod(o, prefix + property);
    }

    public static Method setterFor(Object o, String property, Class<?> type)
    {
        return getMethod(o, "set" + property, type);
    }

    public static Method witherFor(Object o, String property, Class<?> type)
    {
        return getMethod(o, "with" + property, type);
    }


    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object o, Method method, Object... args)
    {
        try
        {
            return (T) method.invoke(o, args);
        }
        catch (InvocationTargetException e)
        {
            throw new AssertionError(e.getTargetException());
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError(e);
        }
    }


    public static void assertEqualProperties(Object expected, Object actual)
    {
        if (expected == actual) return;

        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(expected);
        for (PropertyDescriptor prop : propertyDescriptors)
        {
            if (prop.getName().equals("class")) continue;

            Method readMethod = prop.getReadMethod();
            Object expectedPropertyValue = invoke(expected, readMethod);
            Object actualPropertyValue   = invoke(actual,   readMethod);

            String message = "property " + prop.getName() + " isn't equal";
            assertEquals(expectedPropertyValue, actualPropertyValue, message);
        }
    }
}
