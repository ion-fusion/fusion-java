// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;

import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import com.amazon.ion.util.IonTextUtils;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionLob.BaseLob;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.runtime.embed.TopLevel;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utilities for working with Fusion {@code blob} values.
 *
 * @see FusionLob
 * @see FusionClob
 * @see FusionValue
 */
public final class FusionBlob
{
    private FusionBlob() {}


    abstract static class BaseBlob
        extends BaseLob<BaseBlob>
        implements AnnotatableValue<BaseBlob>
    {
        private BaseBlob() {}

        @Override
        public BaseBlob annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return this;
            return new AnnotatedBlob(annotations, this);
        }
    }


    private static class NullBlob
        extends BaseBlob
    {
        private NullBlob() {}

        @Override
        boolean isAnyNull()
        {
            return true;
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
        {
            boolean b = (right instanceof BaseBlob
                         && ((BaseBlob) right).isAnyNull());
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
            throws FusionException
        {
            return factory.newNullBlob();
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeNull(IonType.BLOB);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException
        {
            out.append("null.blob");
        }
    }


    private static class ActualBlob
        extends BaseBlob
    {
        private final byte[] myContent;

        private ActualBlob(byte[] content)
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
            if (right instanceof BaseBlob)
            {
                return actualLobEquals(eval, myContent, right);
            }
            return falseBool(eval);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            return factory.newBlob(myContent);
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
        {
            out.writeBlob(myContent);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonTextUtils.printBlob(out, myContent);
        }
    }


    private static class AnnotatedBlob
        extends BaseBlob
    {
        /** Not null or empty */
        final BaseSymbol[] myAnnotations;

        /** Not null, and not AnnotatedBlob */
        final BaseBlob  myValue;

        private AnnotatedBlob(BaseSymbol[] annotations, BaseBlob value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public boolean isAnnotated()
        {
            return true;
        }

        @Override
        public BaseSymbol[] getAnnotations()
        {
            return myAnnotations;
        }

        @Override
        public BaseBlob annotate(Evaluator eval, BaseSymbol[] annotations)
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
            throws FusionException
        {
            IonValue iv = myValue.copyToIonValue(factory,
                                                 throwOnConversionFailure);
            iv.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException
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


    private static final BaseBlob NULL_BLOB = new NullBlob();


    /**
     * Returns a blob with the given byte content.
     * This method assumes ownership of the array and it must not be modified
     * later.
     *
     * @param value may be null to make {@code null.blob}.
     *
     * @return not null.
     */
    static BaseBlob forBytesNoCopy(Evaluator eval, byte[] value)
    {
        return (value == null ? NULL_BLOB : new ActualBlob(value));
    }


    /**
     * Returns a blob with the given byte content.
     * This method assumes ownership of the array, and it must not be modified
     * later.
     *
     * @param top the {@link TopLevel} in which to make the blob
     * @param value may be null to make {@code null.blob}.
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
     * @param value may be null to make {@code null.blob}.
     * This method assumes ownership of the array and it must not be modified
     * later.
     *
     * @return not null.
     */
    static BaseBlob forBytesNoCopy(Evaluator eval,
                                   String[]  annotations,
                                   byte[]    value)
    {
        return forBytesNoCopy(eval, value).annotate(eval, annotations);
    }


    /**
     * @param fusionBlob must be a Fusion blob.
     * @param annotations must not be null and must not contain elements
     * that are null or empty. This method assumes ownership of the array
     * and it must not be modified later.
     *
     * @return not null.
     */
    static BaseBlob unsafeBlobAnnotate(Evaluator eval,
                                       Object fusionBlob,
                                       String[] annotations)
    {
        return ((BaseBlob) fusionBlob).annotate(eval, annotations);
    }

    static BaseBlob unsafeBlobAnnotate(TopLevel top,
                                       Object fusionBlob,
                                       String[] annotations)
        throws FusionException
    {
        Evaluator eval = StandardTopLevel.toEvaluator(top);
        return unsafeBlobAnnotate(eval, fusionBlob, annotations);
    }


    //========================================================================
    // Predicates


    /**
     * Determines whether a given Fusion value is a blob.
     *
     * @param top the {@link TopLevel} in which to test the value
     * @param value the value to test
     *
     * @return {@code true} if the value is a Fusion blob,
     * otherwise {@code false}
     *
     * @throws FusionException if an error occurs during evaluation
     */
    public static boolean isBlob(TopLevel top, Object value)
        throws FusionException
    {
        return (value instanceof BaseBlob);
    }

    static boolean isBlob(Evaluator eval, Object value)
        throws FusionException
    {
        return (value instanceof BaseBlob);
    }


    //========================================================================
    // Conversions




    //========================================================================
    // Procedure Helpers




    //========================================================================
    // Procedures


    static final class IsBlobProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isBlob(eval, arg);
            return makeBool(eval, r);
        }
    }
}
