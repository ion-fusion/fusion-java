// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionList.immutableList;
import static dev.ionfusion.fusion.FusionList.nullList;
import static dev.ionfusion.fusion.FusionNumber.makeDecimal;
import static dev.ionfusion.fusion.FusionNumber.makeFloat;
import static dev.ionfusion.fusion.FusionNumber.makeInt;
import static dev.ionfusion.fusion.FusionSexp.immutableSexp;
import static dev.ionfusion.fusion.FusionSexp.nullSexp;
import static dev.ionfusion.fusion.FusionString.makeString;
import static dev.ionfusion.fusion.FusionStruct.nullStruct;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionTimestamp.makeTimestamp;
import static dev.ionfusion.fusion.SourceLocation.forCurrentSpan;
import static dev.ionfusion.fusion.SyntaxValue.STX_PROPERTY_ORIGINAL;
import static com.amazon.ion.IntegerSize.BIG_INTEGER;
import static java.lang.Boolean.TRUE;
import dev.ionfusion.fusion.FusionList.BaseList;
import dev.ionfusion.fusion.FusionSexp.BaseSexp;
import com.amazon.ion.Decimal;
import com.amazon.ion.IntegerSize;
import com.amazon.ion.IonException;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonType;
import com.amazon.ion.Timestamp;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class StandardReader
{
    /**
     * Reads a single Ion datum.
     *
     * @param source must be positioned on the value to be read.
     */
    static Object read(Evaluator eval,
                       IonReader source)
        throws FusionException
    {
        try
        {
            return read(eval, source, null, false);
        }
        catch (IonException e)
        {
            throw new FusionException("Error reading data: " + e.getMessage(),
                                      e);
        }
    }


    /**
     * Reads a single Ion datum as source.
     *
     * @param source must be positioned on the value to be read.
     * @param name may be null.
     */
    static SyntaxValue readSyntax(Evaluator  eval,
                                  IonReader  source,
                                  SourceName name)
        throws FusionException
    {
        try
        {
            return (SyntaxValue) read(eval, source, name, true);
        }
        catch (IonException e)
        {
            // We don't try to create a SourceLocation from the reader because
            // it usually doesn't have a current span when an error is thrown,
            // and since the IonException's message will contain it.
            String nameStr = (name != null ? name.display() : "source");
            String message =
                "Error reading " + nameStr + ":\n" + e.getMessage();
            throw new FusionErrorException(message, e);
        }
    }


    /**
     * Reads a single Ion datum, optionally as syntax objects.
     * @param source must be positioned on the value to be read.
     * @param name may be null.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static Object read(Evaluator  eval,
                               IonReader  source,
                               SourceName name,
                               boolean    readingSyntax)
        throws FusionException, IonException
    {
        IonType type = source.getType();
        assert type != null;

        String[] anns = source.getTypeAnnotations();
        SourceLocation loc =
            (readingSyntax ? forCurrentSpan(source, name) : null);

        BaseValue datum;
        switch (type)
        {
            case NULL:
            {
                datum = FusionNull.makeNullNull(eval, anns);
                break;
            }
            case BOOL:
            {
                if (source.isNullValue())
                {
                    datum = makeBool(eval, anns, null);
                }
                else
                {
                    boolean value = source.booleanValue();
                    datum = makeBool(eval, anns, value);
                }
                break;
            }
            case INT:
            {
                IntegerSize size = source.getIntegerSize();
                if (size == null) size = BIG_INTEGER;
                switch (size)
                {
                    case INT:
                    {
                        int value = source.intValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                    case LONG:
                    {
                        long value = source.longValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                    case BIG_INTEGER:
                    default:
                    {
                        BigInteger value = source.bigIntegerValue();
                        datum = makeInt(eval, anns, value);
                        break;
                    }
                }
                break;
            }
            case DECIMAL:
            {
                Decimal value = source.decimalValue();
                datum = makeDecimal(eval, anns, value);
                break;
            }
            case FLOAT:
            {
                if (source.isNullValue())
                {
                    datum = makeFloat(eval, anns, null);
                }
                else
                {
                    double value = source.doubleValue();
                    datum = makeFloat(eval, anns, value);
                }
                break;
            }
            case TIMESTAMP:
            {
                Timestamp value = source.timestampValue();
                datum = makeTimestamp(eval, anns, value);
                break;
            }
            case STRING:
            {
                String value = source.stringValue();
                datum = makeString(eval, anns, value);
                break;
            }
            case SYMBOL:
            {
                String value = source.stringValue();
                datum = makeSymbol(eval, anns, value);
                break;
            }
            case BLOB:
            {
                byte[] value =
                    (source.isNullValue() ? null : source.newBytes());
                datum = FusionBlob.forBytesNoCopy(eval, anns, value);
                break;
            }
            case CLOB:
            {
                byte[] value =
                    (source.isNullValue() ? null : source.newBytes());
                datum = FusionClob.forBytesNoCopy(eval, anns, value);
                break;
            }
            case LIST:
            {
                datum = readList(eval, source, name, readingSyntax, anns);
                break;
            }
            case SEXP:
            {
                datum = readSexp(eval, source, name, readingSyntax, anns);
                break;
            }
            case STRUCT:
            {
                datum = readStruct(eval, source, name, readingSyntax, anns);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("Bad type: " + type);
            }
        }

        if (readingSyntax)
        {
            SyntaxValue stx = datum.makeOriginalSyntax(eval, loc);
            assert stx.findProperty(eval, STX_PROPERTY_ORIGINAL) == TRUE;
            return stx;
        }

        return datum;
    }



    /**
     * @param source must be positioned on the value to be read, but not
     * stepped-in.  Must not be positioned on a null value.
     * @param name may be null.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static ArrayList<Object> readSequence(Evaluator  eval,
                                                  IonReader  source,
                                                  SourceName name,
                                                  boolean    readingSyntax)
        throws FusionException, IonException
    {
        assert ! source.isNullValue();

        ArrayList<Object> children = new ArrayList<>();

        source.stepIn();
        while (source.next() != null)
        {
            Object child = read(eval, source, name, readingSyntax);
            children.add(child);
        }
        source.stepOut();

        return children;
    }


    /**
     * @return an immutable list of syntax objects.
     *
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseList readList(Evaluator  eval,
                                     IonReader  source,
                                     SourceName name,
                                     boolean    readingSyntax,
                                     String[]   annotations)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullList(eval, annotations);
        }

        ArrayList<Object> elements =
            readSequence(eval, source, name, readingSyntax);
        return immutableList(eval, annotations, elements);
    }


    /**
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseSexp readSexp(Evaluator  eval,
                                     IonReader  source,
                                     SourceName name,
                                     boolean    readingSyntax,
                                     String[]   annotations)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullSexp(eval, annotations);
        }

        List<Object> elements =
            readSequence(eval, source, name, readingSyntax);
        return immutableSexp(eval, annotations, elements);
    }


    /**
     * @throws IonException if there's a problem reading the source data.
     */
    private static BaseValue readStruct(Evaluator  eval,
                                        IonReader  source,
                                        SourceName name,
                                        boolean    readingSyntax,
                                        String[]   anns)
        throws FusionException, IonException
    {
        if (source.isNullValue())
        {
            return nullStruct(eval, anns);
        }

        FusionStruct.Builder builder = FusionStruct.builder(eval);

        source.stepIn();
        {
            while (source.next() != null)
            {
                String key   = source.getFieldName();
                Object value = read(eval, source, name, readingSyntax);
                builder.add(key, value);
            }
        }
        source.stepOut();

        return builder.buildImmutable(anns);
    }
}
