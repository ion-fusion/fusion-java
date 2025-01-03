// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionLob.BaseLob;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utilities for working with Fusion {@code clob} values.
 *
 * @see FusionLob
 * @see FusionBlob
 * @see FusionValue
 */
public final class FusionClob
{
    private FusionClob() {}


    abstract static class BaseClob
        extends BaseLob
    {
        private BaseClob() {}

        @Override
        BaseClob annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedClob(annotations, this);
        }
    }


    private static class NullClob
        extends BaseClob
    {
        private NullClob() {}

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            boolean b = (right instanceof BaseClob
                         && ((BaseClob) right).isAnyNull());
            return makeBool(eval, b);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newNullClob();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeNull(IonType.CLOB);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("null.clob");
        }
    }


    private static class ActualClob
        extends BaseClob
    {
        private final byte[] myContent;

        private ActualClob(byte[] content)
        {
            assert content != null;
            myContent = content;
        }

        @Override
        byte[] bytesNoCopy()
        {
            return myContent;
        }

        @Override
        byte[] bytesCopy()
        {
            return Arrays.copyOf(myContent, myContent.length);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
        {
            if (right instanceof BaseClob)
            {
                return actualLobEquals(eval, myContent, right);
            }
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            return factory.newClob(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.writeClob(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonTextUtils.printClob(out, myContent);
        }
    }


    private static class AnnotatedClob
        extends BaseClob
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedClob */
        final BaseClob  myValue;

        private AnnotatedClob(BaseSymbol[] annotations, BaseClob value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        BaseClob annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return myValue.annotate(eval, annotations);
        }

        @Override
        boolean isAnyNull()
        {
            return myValue.isAnyNull();
        }

        @Override
        byte[] bytesNoCopy()
        {
            return myValue.bytesNoCopy();
        }

        @Override
        byte[] bytesCopy()
        {
            return myValue.bytesCopy();
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.tightEquals(eval, right);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return myValue.looseEquals(eval, right);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException, IonizeFailure
        {
            IonValue iv = myValue.copyToIonValue(factory,
                                                 throwOnConversionFailure);
            iv.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            myValue.ionize(eval, out);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            myValue.write(eval, out);
        }
    }


    //========================================================================
    // Constructors


    private static final BaseClob NULL_CLOB = new NullClob();


    /**
     * Returns a clob with the given byte content.
     * This method assumes ownership of the array and it must not be modified
     * later.
     *
     * @param value may be null to make {@code null.clob}.
     *
     * @return not null.
     */
    static BaseClob forBytesNoCopy(Evaluator eval, byte[] value)
    {
        return (value == null ? NULL_CLOB : new ActualClob(value));
    }


    /**
     * Returns a clob with the given byte content.
     * This method assumes ownership of the array, and it must not be modified
     * later.
     *
     * @param top the {@link TopLevel} in which to make the clob
     * @param value may be null to make {@code null.clob}.
     *
     * @return not null.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static Object forBytesNoCopy(TopLevel top, byte[] value)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return forBytesNoCopy(eval, value);
    }


    /**
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     * @param value may be null to make {@code null.clob}.
     * This method assumes ownership of the array and it must not be modified
     * later.
     *
     * @return not null.
     */
    static BaseClob forBytesNoCopy(Evaluator eval,
                                   String[]  annotations,
                                   byte[]    value)
    {
        BaseClob base = forBytesNoCopy(eval, value);
        return base.annotate(eval, internSymbols(annotations));
    }


    /**
     * @param fusionClob must be a Fusion clob.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseClob unsafeClobAnnotate(Evaluator eval,
                                       Object fusionClob,
                                       String[] annotations)
    {
        BaseClob base = (BaseClob) fusionClob;
        return base.annotate(eval, internSymbols(annotations));
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a given Fusion value is a clob.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is a Fusion clob,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isClob(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseClob);
    }

    static boolean isClob(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseClob);
    }


    //========================================================================
    // Conversions




    //========================================================================
    // Procedure Helpers




    //========================================================================
    // Procedures


    static final class IsClobProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isClob(eval, arg);
            return makeBool(eval, r);
        }
    }
}
