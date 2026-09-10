// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AttributeSetTest
{
    private final AttributeSet attribs = new AttributeSet();


    @Test
    void addAttributeRejectsNullArguments()
    {
        assertThrows(NullPointerException.class,
                     () -> attribs.addAttribute(null, new Object()));
        assertThrows(NullPointerException.class,
                     () -> attribs.addAttribute(Attribute.ofType(Object.class), null));
    }

    @Test
    void getReturnsAddedInstance()
    {
        Attribute<String> attr = Attribute.ofType(String.class);
        assertNull(attribs.getAttribute(attr));

        String stringValue = "foo";
        attribs.addAttribute(attr, stringValue);
        assertSame(stringValue, attribs.getAttribute(attr));
    }

    @Test
    void cannotReplacePresentAttribute()
    {
        Attribute<String> attr = Attribute.ofType(String.class);
        String value = "foo";
        attribs.addAttribute(attr, value);

        // Can't replace an attribute
        assertThrows(IllegalStateException.class,
                     () -> attribs.addAttribute(attr, "bar"));
        assertSame(value, attribs.getAttribute(attr));

        // Can't null one out either
        assertThrows(NullPointerException.class,
                     () -> attribs.addAttribute(attr, null));
        assertSame(value, attribs.getAttribute(attr));
    }

    @Test
    void attributesMatchByIdentity()
    {
        // Can't exfiltrate with another attribute of the same type
        Attribute<String> attr = Attribute.ofType(String.class);
        attribs.addAttribute(attr, "foo");

        assertNull(attribs.getAttribute(Attribute.ofType(String.class)));
    }

    @Test
    void testPrimitiveAttribute()
    {
        Attribute<Boolean> boolFacet = Attribute.ofType(Boolean.class);
        assertNull(attribs.getAttribute(boolFacet));

        attribs.addAttribute(boolFacet, true);
        assertSame(true, attribs.getAttribute(boolFacet));
    }

    @Test
    void valuesAreTypeChecked()
    {
        Attribute<Long> longFacet = Attribute.ofType(Long.class);
        Attribute<String> notAString = recastAttribute(longFacet);
        assertThrows(ClassCastException.class, () -> attribs.addAttribute(notAString, "42"));
        assertNull(attribs.getAttribute(longFacet));
    }

    /**
     * Forcibly recast an attribute to defeat compiler type checking.
     */
    @SuppressWarnings("unchecked")
    static <T> Attribute<T> recastAttribute(Attribute<?> attribute)
    {
        return (Attribute<T>) attribute;
    }
}
