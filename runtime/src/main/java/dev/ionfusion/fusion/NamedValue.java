// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.io.IOException;


abstract class NamedValue
    extends BaseValue
    implements NamedObject
{
    private BaseSymbol myName;


    @Override
    public final BaseSymbol objectName()
    {
        return myName;
    }

    final String getInferredName()
    {
        return myName == null ? null : myName.stringValue();
    }

    final void inferName(BaseSymbol name)
    {
        if (myName == null)
        {
            myName = name;
        }
    }

    static void inferObjectName(Object value, BaseSymbol name)
    {
        if (value instanceof NamedValue)
        {
            ((NamedValue)value).inferName(name);
        }
    }


    /**
     * Identifies this value, usually by name and type.
     * For example, a procedure with inferred name "foo" would give the result
     * {@code "procedure foo"}.
     *
     * @throws IOException
     */
    abstract void identify(Appendable out)
        throws IOException;

    /**
     * Returns the output of {@link #identify(Appendable)} as a {@link String}.
     *
     * @return not null.
     */
    String identify()
    {
        StringBuilder out = new StringBuilder();
        try
        {
            identify(out);
        }
        catch (IOException e) {}
        return out.toString();
    }


    @Override
    public final void write(Evaluator eval, Appendable out)
        throws IOException
    {
        out.append("{{{");
        identify(out);
        out.append("}}}");
    }
}
