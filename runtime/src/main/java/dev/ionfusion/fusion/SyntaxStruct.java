// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonException;
import com.amazon.ion.IonWriter;
import dev.ionfusion.fusion.FusionStruct.ImmutableStruct;
import dev.ionfusion.fusion.FusionStruct.StructFieldVisitor;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;

final class SyntaxStruct
    extends SyntaxContainer
{
    private ImmutableStruct myStruct;


    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(SourceLocation  loc,
                         Object[] properties,
                         SyntaxWraps wraps,
                         ImmutableStruct struct)
    {
        super(loc, properties, wraps);
        myStruct = struct;
    }

    /**
     * @param struct must not be null.
     */
    private SyntaxStruct(SourceLocation  loc,
                         ImmutableStruct struct)
    {
        super(loc);
        myStruct = struct;
    }



    static SyntaxStruct makeOriginal(Evaluator       eval,
                                     SourceLocation  loc,
                                     ImmutableStruct struct)
    {
        return new SyntaxStruct(loc, ORIGINAL_STX_PROPS, null, struct);
    }


    /**
     * @param datum must be an immutable struct
     */
    static SyntaxStruct make(Evaluator eval,
                             SourceLocation loc,
                             Object datum)
    {
        return new SyntaxStruct(loc, (ImmutableStruct) datum);
    }


    //========================================================================


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    boolean hasNoChildren()
    {
        return myStruct.size() == 0;
    }


    @Override
    SyntaxStruct copyReplacingProperties(Object[] properties)
    {
        return new SyntaxStruct(getLocation(), properties, myWraps, myStruct);
    }

    @Override
    SyntaxStruct copyReplacingWraps(SyntaxWraps wraps)
    {
        return new SyntaxStruct(getLocation(), getProperties(), wraps,
                                myStruct);
    }


    @Override
    SyntaxStruct stripWraps(final Evaluator eval)
        throws FusionException
    {
        if (hasNoChildren()) return this;  // No children, no marks, all okay!

        StructFieldVisitor visitor = new StructFieldVisitor() {
            @Override
            public Object visit(String name, Object value)
                    throws FusionException
            {
                return ((SyntaxValue) value).stripWraps(eval);
            }
        };

        ImmutableStruct s = myStruct.transformFields(eval, visitor);
        if (s == myStruct) return this;

        return new SyntaxStruct(getLocation(), getProperties(), null, s);
    }


    SyntaxValue get(Evaluator eval, String fieldName)
        throws FusionException
    {
        // This should only be called at runtime, after wraps are pushed.
        assert myWraps == null;

        return (SyntaxValue) myStruct.elt(eval, fieldName);
    }


    @Override
    Object unwrap(Evaluator eval)
        throws FusionException
    {
        if (myWraps == null)
        {
            return myStruct;
        }

        // We have wraps to propagate (and therefore children).
        // Idea: keep track of when there are symbols contained (recursively),
        // when there's not, maybe we can skip all this.

        StructFieldVisitor visitor = new StructFieldVisitor() {
            @Override
            public Object visit(String name, Object value)
                    throws FusionException
            {
                return ((SyntaxValue) value).addWraps(myWraps);
            }
        };

        myStruct = myStruct.transformFields(eval, visitor);
        myWraps = null;

        return myStruct;
    }


    @Override
    Object syntaxToDatum(final Evaluator eval)
        throws FusionException
    {
        if (myStruct.size() == 0)
        {
            return myStruct;
        }

        // We have children, and wraps to propagate (when not recursing)

        StructFieldVisitor visitor = new StructFieldVisitor() {
            @Override
            public Object visit(String name, Object value)
                    throws FusionException
            {
                return ((SyntaxValue) value).syntaxToDatum(eval);
            }
        };

        return myStruct.transformFields(eval, visitor);
    }


    @Override
    SyntaxValue doExpand(final Expander expander, final Environment env)
        throws FusionException
    {
        final Evaluator eval = expander.getEvaluator();
        if (myStruct.size() == 0)
        {
            return this;
        }

        StructFieldVisitor visitor = new StructFieldVisitor() {
            @Override
            public Object visit(String name, Object value)
                throws FusionException
            {
                SyntaxValue subform = (SyntaxValue) value;
                if (myWraps != null)
                {
                    subform = subform.addWraps(myWraps);
                }
                return expander.expandExpression(env, subform);
            }
        };

        ImmutableStruct s = myStruct.transformFields(eval, visitor);

        // Wraps have been pushed down so the copy doesn't need them.
        return new SyntaxStruct(getLocation(), s);
    }


    @Override
    void ionize(Evaluator eval, IonWriter writer)
        throws IOException, IonException, FusionException
    {
        myStruct.ionize(eval, writer);
    }

    @Override
    final void write(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        myStruct.write(eval, out);
    }
}
