// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionIo.dispatchWrite;
import static com.amazon.fusion.FusionNull.isNullNull;
import static com.amazon.fusion.FusionNumber.checkIntArgToJavaInt;
import static com.amazon.fusion.FusionNumber.checkNullableIntArg;
import static com.amazon.fusion.FusionSymbol.checkRequiredSymbolArg;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Implementation of records; based on Racket's "structs".
 */
final class Records
{
    private final static class RecordType
        extends BaseValue
        implements NamedObject
    {
        private final BaseSymbol myName;
        private final RecordType mySupertype; // TODO push to subclass?

        /**
         * Number of fields initialized by constructor, excluding supertype.
         */
        private final int myFieldCount;

        /**
         * Number of fields initialized by constructor, including supertype.
         */
        private final int myTotalFieldCount;

        RecordType(BaseSymbol name, RecordType supertype, int fieldCount)
        {
            myName = name;
            mySupertype  = supertype;
            myFieldCount = fieldCount;
            myTotalFieldCount =
                fieldCount + (supertype == null
                                ? 0
                                : supertype.myTotalFieldCount);
        }


        /**
         * Determines whether a given RecordType is a supertype of this one.
         */
        boolean hasSupertype(RecordType supertype)
        {
            for (RecordType type = this; type != null; type = type.mySupertype)
            {
                if (type == supertype) return true;
            }
            return false;
        }

        boolean hasInstance(Object o)
        {
            return (o instanceof RecordInstance) &&
                   ((RecordInstance) o).getType().hasSupertype(this);
        }

        void writeName(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            myName.write(eval, out);
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            out.append("{{{record_type ");
            writeName(eval, out);
            out.append("}}}");
        }


        int getInitFieldCount()
        {
            return myFieldCount;
        }


        int getTotalInitFieldCount()
        {
            return myTotalFieldCount;
        }

        @Override
        public Object objectName(Evaluator eval)
        {
            return myName;
        }
    }


    private interface RecordInstance
    {
        RecordType getType();
        Object unsafe_access(int i);
    }


    /**
     * Implements normal, non-procedure records.
     */
    private final static class PlainRecordInstance
        extends BaseValue
        implements RecordInstance
    {
        private final RecordType myType;
        private final Object[] myFields;

        PlainRecordInstance(RecordType type, Object[] fields)
        {
            assert fields.length == type.myTotalFieldCount;

            myType   = type;
            myFields = fields;
        }

        @Override
        public RecordType getType()
        {
            return myType;
        }

        public Object unsafe_access(int i)
        {
            return myFields[i];
        }

        @Override
        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            // TODO Support opaque records
            myType.writeName(eval, out);
            out.append("::{[");

            int length = myFields.length;
            for (int i = 0; i < length; i++)
            {
                if (i != 0) out.append(", ");
                dispatchWrite(eval, out, myFields[i]);
            }

            out.append("]}");
        }
    }


    /**
     * Implements records that can be applied as procedures.
     */
    private final static class ProcRecordInstance
        extends Procedure
        implements RecordInstance
    {
        private final RecordType myType;
        private final Object[]   myFields;
        private final Procedure  myProc;

        ProcRecordInstance(RecordType type, Object[] fields, Procedure proc)
        {
            assert fields.length == type.myTotalFieldCount;

            myType   = type;
            myFields = fields;
            myProc   = proc;

            // "If a structure is a procedure [...], then its name is the
            // implementing procedure’s name."
            // https://tinyurl.com/object-name

            this.inferName(myProc.getInferredName());

            // I have to wonder if there's contexts in which myProc hasn't had
            // its inferred name assigned yet; perhaps this should be dynamic?
        }

        @Override
        public RecordType getType()
        {
            return myType;
        }

        public Object unsafe_access(int i)
        {
            return myFields[i];
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return eval.bounceTailCall(myProc, args);
        }
    }


    private static class RecordConstructorProc
        extends Procedure
    {
        final RecordType myType;
        final int        myInitFieldCount;

        RecordConstructorProc(RecordType type)
        {
            myType           = type;
            myInitFieldCount = type.myTotalFieldCount;
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(myInitFieldCount, args);
            // TODO apply guardian
            return new PlainRecordInstance(myType, args);
        }
    }


    private final static class ProcRecordConstructorProc
        extends RecordConstructorProc
    {
        private final int myProcIndex;

        ProcRecordConstructorProc(RecordType type, int procIndex)
        {
            super(type);

            myProcIndex = procIndex;
        }

        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(myInitFieldCount, args);

            // TODO apply guardian
            Object proc = args[myProcIndex];
            if (proc instanceof Procedure)
            {
                return new ProcRecordInstance(myType, args, (Procedure) proc);
            }

            // TODO Allow non-proc here, but the record always throws arity error.
            // That requires changing ArityFailure to not require an explicit arity.
            throw argFailure("procedure", myProcIndex, args);
        }
    }


    private final static class RecordPredicateProc
        extends Procedure1
    {
        private final RecordType myType;

        RecordPredicateProc(RecordType type)
        {
            myType = type;
        }

        @Override
        Object doApply(Evaluator eval, Object v)
        {
            return makeBool(eval, myType.hasInstance(v));
        }
    }


    private final static class RecordAccessorProc
        extends Procedure2
    {
        private final RecordType myType;
        private final int        myIndexOffset;

        RecordAccessorProc(RecordType type)
        {
            myType = type;

            RecordType supertype = myType.mySupertype;
            myIndexOffset = (supertype == null) ? 0 : supertype.myTotalFieldCount;
        }

        @Override
        Object doApply(Evaluator eval, Object v, Object index)
            throws FusionException
        {
            if (! myType.hasInstance(v))
            {
                throw new ArgumentException(this, "instance of " + myType.myName,
                                            0, v, index);
            }

            int i = checkIntArgToJavaInt(eval, this, 1, v, index);
            if (i < 0)
            {
                throw new ArgumentException(this, "non-negative int",
                                            1, v, index);
            }

            int max = myType.myFieldCount - 1;
            if (max < 0)
            {
                throw new ArgumentException(this, "no access to fields",
                                            -1, v, index);
            }
            if (max < i)
            {
                throw new ArgumentException(this, "index up to " + max,
                                            1, v, index);
            }

            return ((RecordInstance) v).unsafe_access(i + myIndexOffset);
        }
    }


    /*
     * Currently:
     *   (make_record_type name_sym supertype field_count [ proc-index ])
     *
     * Eventually this should conform to Racket's protocol:
     * (make-struct-type name super-type
     *   init-field-cnt auto-field-cnt
     *   [ auto-v props inspector proc-spec immutables guard constructor-name])
     */
    final static class MakeRecordTypeProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityRange(3, 4, args);

            String name = checkRequiredSymbolArg(eval, this, 0, args);
            if (name.isEmpty())
            {
                throw new ArgumentException(this, "non-empty symbol", 0, args);
            }

            RecordType supertype = null;
            if (args[1] instanceof RecordType)
            {
                supertype = (RecordType) args[1];
            }
            else if (! isNullNull(eval, args[1]))
            {
                String expected = "record type descriptor or null";
                throw new ArgumentException(this, expected, 1, args);
            }

            final int initFieldCount = checkIntArgToJavaInt(eval, this, 2, args);
            if (initFieldCount < 0)
            {
                throw new ArgumentException(this, "non-negative int", 1, args);
            }

            int argCount = args.length;

            int procIndex = -1;
            if (argCount >= 4)
            {
                if (! isNullNull(eval, args[3]))
                {
                    BigInteger bi = checkNullableIntArg(eval, this, 3, args);
                    if (bi != null)
                    {
                        // TODO JDK8 Use bi.intValueExact() to avoid truncation
                        procIndex = bi.intValue();

                        if (procIndex < 0)
                        {
                            throw argFailure("non-negative int", 3, args);
                        }
                        if (procIndex >= initFieldCount)
                        {
                            throw argFailure("int less than field count", 3, args);
                        }
                        if (supertype != null)
                        {
                            procIndex += supertype.getInitFieldCount();
                        }
                    }
                }
            }

            RecordType type
                = new RecordType((BaseSymbol) args[0], supertype, initFieldCount);
            Procedure ctor
                = (procIndex < 0
                       ? new RecordConstructorProc(type)
                       : new ProcRecordConstructorProc(type, procIndex));
            Procedure pred
                = new RecordPredicateProc(type);
            Procedure accessor
                = new RecordAccessorProc(type);

            ctor.inferName("make_" + name);
            pred.inferName("is_" + name);
            accessor.inferName(name + "_element");

            return new Object[] { type, ctor, pred, accessor };
        }
    }
}
