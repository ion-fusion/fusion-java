// Copyright (c) 2013-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.falseBool;
import static com.amazon.fusion.FusionBool.makeBool;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.fusion.FusionLob.BaseLob;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;

/**
 *
 */
final class FusionClob
{
    private FusionClob() {}


    abstract static class BaseClob
        extends BaseLob
    {
        private BaseClob() {}
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
            // TODO WORKAROUND ION-398
            // TODO FUSION-247 Write output without building an IonWriter.
            IonWriter iw = WRITER_BUILDER.build(out);
            iw.writeClob(myContent);
            iw.finish();
        }
    }


    private static class AnnotatedClob
        extends BaseClob
        implements Annotated
    {
        /** Not null or empty */
        final String[] myAnnotations;

        /** Not null, and not AnnotatedBool */
        final BaseClob  myValue;

        private AnnotatedClob(String[] annotations, BaseClob value)
        {
            assert annotations.length != 0;
            myAnnotations = annotations;
            myValue = value;
        }

        @Override
        public String[] annotationsAsJavaStrings()
        {
            return myAnnotations;
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
            iv.setTypeAnnotations(myAnnotations);
            return iv;
        }

        @Override
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure
        {
            out.setTypeAnnotations(myAnnotations);
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
     * @param value may be null to make {@code null.clob}.
     * This method assumes ownership of the array and it must not be modified
     * later.
     *
     * @return not null.
     */
    static BaseClob makeClob(Evaluator eval, byte[] value)
    {
        return (value == null ? NULL_CLOB : new ActualClob(value));
    }


    private static BaseClob annotate(BaseClob unannotated,
                                     String[] annotations)
    {
        assert ! (unannotated instanceof AnnotatedClob);

        if (annotations.length == 0) return unannotated;

        return new AnnotatedClob(annotations, unannotated);
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
    static BaseClob makeClob(Evaluator eval,
                             String[]  annotations,
                             byte[]    value)
    {
        BaseClob base = makeClob(eval, value);
        return annotate(base, annotations);
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
        if (base instanceof AnnotatedClob)
        {
            base = ((AnnotatedClob) base).myValue;
        }
        return annotate(base, annotations);
    }


    //========================================================================
    // Predicates


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
        IsClobProc()
        {
            //    "                                                                               |
            super("Determines whether a `value` is of type `Clob`, returning `true` or `false`.",
                  "value");
        }

        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean r = isClob(eval, arg);
            return makeBool(eval, r);
        }
    }
}
