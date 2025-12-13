// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import java.util.Objects;

public class InternedString
{
    private final String myValue;

    InternedString(String value) {
        // We don't want to assume that the interned value keeps the key alive.
        myValue = new String(value);
    }

    public String getValue() { return myValue; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof InternedString &&
                             Objects.equals(myValue, ((InternedString) o).myValue));
    }

    @Override
    public int hashCode() { return Objects.hashCode(myValue); }
}
