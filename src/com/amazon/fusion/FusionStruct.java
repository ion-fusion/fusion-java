// Copyright (c) 2012-2020 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionBool.falseBool;
import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionBool.trueBool;
import static com.amazon.fusion.FusionCompare.EqualityTier.LOOSE_EQUAL;
import static com.amazon.fusion.FusionCompare.EqualityTier.STRICT_EQUAL;
import static com.amazon.fusion.FusionCompare.EqualityTier.TIGHT_EQUAL;
import static com.amazon.fusion.FusionIo.dispatchIonize;
import static com.amazon.fusion.FusionIo.dispatchWrite;
import static com.amazon.fusion.FusionIterator.iterate;
import static com.amazon.fusion.FusionList.checkNullableListArg;
import static com.amazon.fusion.FusionList.unsafeJavaIterate;
import static com.amazon.fusion.FusionSymbol.makeSymbol;
import static com.amazon.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static com.amazon.fusion.FusionText.checkNonEmptyTextArg;
import static com.amazon.fusion.FusionText.textToJavaString;
import static com.amazon.fusion.FusionText.unsafeTextToJavaString;
import static com.amazon.fusion.FusionUtils.EMPTY_STRING_ARRAY;
import static com.amazon.fusion.FusionVoid.voidValue;
import static com.amazon.ion.util.IonTextUtils.printSymbol;
import static java.util.AbstractMap.SimpleEntry;
import com.amazon.fusion.FusionBool.BaseBool;
import com.amazon.fusion.FusionCollection.BaseCollection;
import com.amazon.fusion.FusionCompare.EqualityTier;
import com.amazon.fusion.FusionIterator.AbstractIterator;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.ion.IonException;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


final class FusionStruct
{
    // Go away you big meany!
    private FusionStruct() {}


    private static final NullStruct             NULL_STRUCT  = new NullStruct();
    private static final NonNullImmutableStruct EMPTY_STRUCT =
        new FunctionalStruct(FunctionalHashTrie.EMPTY, BaseSymbol.EMPTY_ARRAY, 0);

    // Utility method.
    static final BiFunction<Object, Object, Object> STRUCT_MERGE_FUNCTION =
        new BiFunction<Object, Object, Object>()
        {
            @Override
            public Object apply(Object o, Object o2)
            {
                return mergeValuesForKey(o, o2);
            }
        };

    //========================================================================
    // Constructors

    static Object structFromIonStruct(Evaluator eval, IonStruct struct)
    {
        String[] annStrings = struct.getTypeAnnotations();
        BaseSymbol[] annotations = internSymbols(annStrings);

        // There's no benefit to being lazy injecting null.struct or {}.
        if (struct.isNullValue())
        {
            return nullStruct(eval, annotations);
        }

        if (struct.isEmpty())
        {
            Map<String, Object> map = Collections.emptyMap();
            return immutableStruct(map, annotations);
        }

        return new LazyInjectingStruct(annotations, struct);
    }


    static NullStruct nullStruct(Evaluator eval)
    {
        return NULL_STRUCT;
    }


    static NullStruct nullStruct(Evaluator eval, BaseSymbol[] annotations)
    {
        if (annotations.length == 0) return NULL_STRUCT;
        return new NullStruct(annotations);
    }

    static NullStruct nullStruct(Evaluator eval, String[] annotations)
    {
        if (annotations.length == 0) return NULL_STRUCT;
        return new NullStruct(internSymbols(annotations));
    }


    static NonNullImmutableStruct immutableStruct(FunctionalHashTrie<String, Object> map)
    {
        return immutableStruct(map, BaseSymbol.EMPTY_ARRAY, computeSize(map));
    }

    static NonNullImmutableStruct immutableStruct(FunctionalHashTrie<String, Object> map,
                                                  int size)
    {
        return immutableStruct(map, BaseSymbol.EMPTY_ARRAY, size);
    }

    static NonNullImmutableStruct immutableStruct(FunctionalHashTrie<String, Object> map,
                                                  String[] anns)
    {
        return immutableStruct(map, internSymbols(anns), computeSize(map));
    }

    static NonNullImmutableStruct immutableStruct(FunctionalHashTrie<String, Object> map,
                                                  BaseSymbol[] anns)
    {
        return immutableStruct(map, anns, computeSize(map));
    }

    static NonNullImmutableStruct immutableStruct(FunctionalHashTrie<String, Object> map,
                                                  BaseSymbol[] anns,
                                                  int size)
    {
        if (map.size() == 0)
        {
            if (anns.length == 0)
            {
                return EMPTY_STRUCT;
            }

            map = FunctionalHashTrie.EMPTY;
        }

        return new FunctionalStruct(map, anns, size);
    }

    static NonNullImmutableStruct immutableStruct(Map<String, Object> map)
    {
        if (map.size() == 0) return EMPTY_STRUCT;
        return new FunctionalStruct(map, BaseSymbol.EMPTY_ARRAY);
    }

    static NonNullImmutableStruct immutableStruct(Map<String, Object> map, int size)
    {
        if (map.size() == 0) return EMPTY_STRUCT;
        return new FunctionalStruct(map, BaseSymbol.EMPTY_ARRAY, size);
    }

    static NonNullImmutableStruct immutableStruct(Map<String, Object> map,
                                                  BaseSymbol[] anns)
    {
        return immutableStruct(map, anns, computeSize(map));
    }


    static NonNullImmutableStruct immutableStruct(Map<String, Object> map,
                                                  BaseSymbol[] anns,
                                                  int size)
    {
        if (map.size() == 0)
        {
            if (anns.length == 0)
            {
                return EMPTY_STRUCT;
            }

            map = Collections.emptyMap(); // Just to normalize
        }

        return new FunctionalStruct(map, anns, size);
    }

    static NonNullImmutableStruct immutableStruct(Map<String, Object> map,
                                                  String[] anns)
    {
        return immutableStruct(map, internSymbols(anns));
    }


    static NonNullImmutableStruct immutableStruct(String[] names,
                                                  Object[] values,
                                                  BaseSymbol[] anns)
    {
        FunctionalHashTrie<String, Object> map = FunctionalHashTrie.EMPTY;
        if (names.length == 0)
        {
            if (anns.length == 0)
            {
                return EMPTY_STRUCT;
            }
        }
        else
        {
            for (int i = 0; i < names.length; i++)
            {
                String field  = names[i];
                Object newElt = values[i];

                map = structImplAdd(map, field, newElt);
            }
        }

        return new FunctionalStruct(map, anns, names.length);
    }

    static NonNullImmutableStruct immutableStruct(String[] names,
                                                  Object[] values,
                                                  String[] anns)
    {
        return immutableStruct(names, values, internSymbols(anns));
    }


    /**
     * Returns an immutable struct with the same content as {@code struct}.
     * This is not a deep conversion: if any elements within {@code struct}
     * are mutable, the same mutable instances will be in the result.
     *
     * @param eval must not be null.
     * @param struct must be a Fusion struct.
     */
    static Object asImmutableStruct(Evaluator eval, Object struct)
        throws FusionException
    {
        if (struct instanceof ImmutableStruct) return struct;

        MutableStruct s = (MutableStruct) struct;
        return immutableStruct(s.getMap(eval), s.size());
    }


    /**
     * Returns a new, empty mutable struct.  This is equivalent to the Fusion
     * code {@code (mutable_struct)}.
     */
    static Object mutableStruct(Evaluator eval)
        throws FusionException
    {
        return new MutableStruct(FunctionalHashTrie.EMPTY,
                                 BaseSymbol.EMPTY_ARRAY,
                                 0);
    }

    static MutableStruct mutableStruct(Map<String, Object> map)
    {
        return new MutableStruct(map, BaseSymbol.EMPTY_ARRAY);
    }

    static MutableStruct mutableStruct(FunctionalHashTrie<String, Object> map)
    {
        return new MutableStruct(map, BaseSymbol.EMPTY_ARRAY);
    }

    static MutableStruct mutableStruct(String[] names,
                                       Object[] values,
                                       String[] anns)
    {
        FunctionalHashTrie<String, Object> map = FunctionalHashTrie.EMPTY;
        if (names.length != 0)
        {
            for (int i = 0; i < names.length; i++)
            {
                String field  = names[i];
                Object newElt = values[i];

                map = structImplAdd(map, field, newElt);
            }
        }

        return new MutableStruct(map, internSymbols(anns), names.length);
    }


    /**
     * Merges one or more new values for a key with one or more existing values.
     *
     * @param prev may be a single value, or an array of values.
     * @param value may be a single value, or an array of values.
     *
     * @return the merged array of values.
     */
    private static Object[] mergeValuesForKey(Object prev, Object value)
    {
        Object[] multi;
        if (prev instanceof Object[])
        {
            Object[] prevArray = (Object[]) prev;
            if (value instanceof Object[])
            {
                Object[] moreArray = (Object[]) value;

                int prevLen = prevArray.length;
                int moreLen = moreArray.length;
                multi = Arrays.copyOf(prevArray, prevLen + moreLen);
                System.arraycopy(moreArray, 0, multi, prevLen, moreLen);
            }
            else
            {
                multi = extend(prevArray, value);
            }
        }
        else if (value instanceof Object[])
        {
            multi = extend((Object[]) value, prev);
        }
        else
        {
            multi = new Object[] { prev, value };
        }
        return multi;
    }


    /**
     * Adds an element to the struct, allowing for repeated fields in the result.
     *
     * @param value may be an array (for repeated fields)
     */
    static FunctionalHashTrie<String, Object> structImplAdd(FunctionalHashTrie<String, Object> map,
                                                            String name,
                                                            Object value)
    {
        // TODO This should be done with a single traversal of the trie.
        Object prev = map.get(name);
        if (prev != null)
        {
            Object[] multi = (Object[]) mergeValuesForKey(prev, value);
            return map.with(name, multi);
        }
        else
        {
            return map.with(name, value);
        }
    }

    private static
    FunctionalHashTrie<String, Object> structImplMerge(FunctionalHashTrie<String, Object> map1,
                                                       Map<String, Object> map2)
    {
        for (Map.Entry<String,Object> entry : map2.entrySet())
        {
            String key   = entry.getKey();
            Object value = entry.getValue();
            map1 = structImplAdd(map1, key, value);
        }
        return map1;
    }

    private static
    FunctionalHashTrie<String, Object> structImplOneify(FunctionalHashTrie<String, Object> map)
    {
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof Object[])
            {
                Object first = ((Object[]) value)[0];
                map = map.with(entry.getKey(), first);
            }
        }
        return map;
    }

    private static
    FunctionalHashTrie<String, Object> structImplMerge1(FunctionalHashTrie<String, Object> map1,
                                                        Map<String, Object> map2)
    {
        for (Map.Entry<String, Object> entry : map2.entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Object[])
            {
                value = ((Object[]) value)[0];
            }
            map1 = map1.with(key, value);
        }
        return map1;
    }


    //========================================================================
    // Utilities

    private static int computeSize(Map map)
    {
        int size = map.size();
        for (Object values : map.values())
        {
            if (values instanceof Object[])
            {
                size += ((Object[]) values).length - 1;
            }
        }
        return size;
    }

    private static Object[] extend(Object[] array, Object element)
    {
        int len = array.length;
        array = Arrays.copyOf(array, len+1);
        array[len] = element;
        return array;
    }

    private static Object bounceDefaultResult(Evaluator eval, Object def)
        throws FusionException
    {
        if (def instanceof Procedure)
        {
            def = eval.bounceTailCall((Procedure) def);
        }
        return def;
    }


    //========================================================================
    // Predicates


    static boolean isStruct(Evaluator eval, Object v)
    {
        return (v instanceof BaseStruct);
    }

    static boolean isImmutableStruct(Evaluator eval, Object v)
    {
        return (v instanceof ImmutableStruct);
    }

    static boolean isMutableStruct(Evaluator eval, Object v)
    {
        return (v instanceof MutableStruct);
    }


    //========================================================================
    // Accessors


    static int unsafeStructSize(Evaluator eval, Object struct)
    {
        return ((BaseStruct) struct).size();
    }


    static Set<String> unsafeStructKeys(Evaluator eval, Object struct)
        throws FusionException
    {
        return ((BaseStruct) struct).keys(eval);
    }


    static boolean unsafeStructHasKey(Evaluator eval, Object struct, String key)
        throws FusionException
    {
        return ((BaseStruct) struct).hasKey(eval, key);
    }


    static void unsafeStructFieldVisit(Evaluator eval, Object struct,
                                       StructFieldVisitor visitor)
        throws FusionException
    {
        ((BaseStruct) struct).visitFields(eval, visitor);
    }

    /**
     * @param struct must be a {@link BaseStruct}.
     * @return void if the position is out of bounds.
     *
     * @deprecated
     * Renamed to {@link #unsafeStructElt(Evaluator, Object, String)}.
     */
    @Deprecated
    static Object unsafeStructDot(Evaluator eval, Object struct, String field)
        throws FusionException
    {
        return ((BaseStruct) struct).elt(eval, field);
    }

    /**
     * Equivalent to {@code (elt struct field)}.
     *
     * @param struct must be a struct; it may be {@code null.struct} or empty.
     *
     * @return void if the struct is null or if the field name doesn't
     * exist in the struct.
     */
    static Object unsafeStructElt(Evaluator eval, Object struct, String field)
        throws FusionException
    {
        return ((BaseStruct) struct).elt(eval, field);
    }


    //========================================================================
    // Element modification


    static Object unsafeStructPut(Evaluator eval, Object struct,
                                  String key, Object value)
        throws FusionException
    {
        return ((BaseStruct) struct).put(eval, key, value);
    }


    static Object unsafeStructPutM(Evaluator eval, Object struct,
                                   String key, Object value)
        throws FusionException
    {
        return ((BaseStruct) struct).putM(eval, key, value);
    }


    static Object unsafeStructPuts(Evaluator eval, Object struct,
                                   String key, Object value)
        throws FusionException
    {
        return ((BaseStruct) struct).puts(eval, key, value);
    }


    static Object unsafeStructPutsM(Evaluator eval, Object struct,
                                    String key, Object value)
        throws FusionException
    {
        return ((BaseStruct) struct).putsM(eval, key, value);
    }


    //========================================================================
    // Bulk modification

    static Object unsafeStructRemoveKeys(Evaluator eval, Object struct,
                                         String[] keys)
        throws FusionException
    {
        if (keys.length == 0) return struct;

        return ((BaseStruct) struct).removeKeys(eval, keys);
    }


    static Object unsafeStructRemoveKeysM(Evaluator eval, Object struct,
                                        String[] keys)
        throws FusionException
    {
        if (keys.length == 0) return struct;

        return ((BaseStruct) struct).removeKeysM(eval, keys);
    }


    static Object unsafeStructRetainKeys(Evaluator eval, Object struct,
                                         String[] keys)
        throws FusionException
    {
        return ((BaseStruct) struct).retainKeys(eval, keys);
    }

    static Object unsafeStructRetainKeysM(Evaluator eval, Object struct,
                                          String[] keys)
        throws FusionException
    {
        return ((BaseStruct) struct).retainKeysM(eval, keys);
    }


    static Object unsafeStructMerge(Evaluator eval, Object struct1,
                                    Object struct2)
        throws FusionException
    {
        return ((BaseStruct) struct1).merge(eval, (BaseStruct) struct2);
    }

    static Object unsafeStructMergeM(Evaluator eval, Object struct1,
                                     Object struct2)
        throws FusionException
    {
        return ((BaseStruct) struct1).mergeM(eval, (BaseStruct) struct2);
    }


    static Object unsafeStructMerge1(Evaluator eval, Object struct1,
                                     Object struct2)
        throws FusionException
    {
        return ((BaseStruct) struct1).merge1(eval, (BaseStruct) struct2);
    }

    static Object unsafeStructMerge1M(Evaluator eval, Object struct1,
                                      Object struct2)
        throws FusionException
    {
        return ((BaseStruct) struct1).merge1M(eval, (BaseStruct) struct2);
    }


    //========================================================================


    static interface StructFieldVisitor
    {
        /**
         * @return null means continue visiting, non-null means abort visiting.
         */
        Object visit(String name, Object value)
            throws FusionException;
    }


    static interface BaseStruct
    {
        boolean isAnnotated()
            throws FusionException;

        boolean isAnnotatable()
            throws FusionException;

        BaseSymbol[] getAnnotations();

        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure;

        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException;

        int size(); // Doesn't throw

        Set<String> keys(Evaluator eval);

        /**
         * Visits each field in the struct, stopping as soon as the visitation
         * returns non-null.
         */
        void visitFields(Evaluator eval, StructFieldVisitor visitor)
            throws FusionException;

        // Return type isn't really right
        ImmutableStruct transformFields(Evaluator eval,
                                        StructFieldVisitor visitor)
            throws FusionException;

        boolean hasKey(Evaluator eval, String key)
            throws FusionException;

        /** Returns void if the field doesn't exist. */
        Object elt(Evaluator eval, String field)
            throws FusionException;

        /**
         * Finds the value for a given name.
         *
         * @param def the default result. If its a procedure, it's called with
         * no arguments to determine the result. Otherwise it's returned as-is.
         */
        Object ref(Evaluator eval, String name, Object def)
            throws FusionException;

        Object put(Evaluator eval, String key, Object value)
            throws FusionException;

        Object putM(Evaluator eval, String key, Object value)
            throws FusionException;

        Object puts(Evaluator eval, String key, Object value)
            throws FusionException;

        Object putsM(Evaluator eval, String key, Object value)
            throws FusionException;

        Object removeKeys(Evaluator eval, String[] keys)
            throws FusionException;

        Object removeKeysM(Evaluator eval, String[] keys)
            throws FusionException;

        Object retainKeys(Evaluator eval, String[] keys)
            throws FusionException;

        Object retainKeysM(Evaluator eval, String[] keys)
            throws FusionException;

        Object merge(Evaluator eval, BaseStruct other)
            throws FusionException;

        Object mergeM(Evaluator eval, BaseStruct other)
            throws FusionException;

        Object merge1(Evaluator eval, BaseStruct other)
            throws FusionException;

        Object merge1M(Evaluator eval, BaseStruct other)
            throws FusionException;
    }


    static interface ImmutableStruct
        extends BaseStruct
    {
    }


    private static final class NullStruct
        extends BaseCollection
        implements ImmutableStruct
    {
        NullStruct() {}

        NullStruct(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        @Override
        public boolean isAnyNull()
        {
            return true;
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxStruct.makeOriginal(eval, loc, this);
        }

        @Override
        SyntaxValue datumToSyntaxMaybe(Evaluator      eval,
                                       SyntaxSymbol   context,
                                       SourceLocation loc)
            throws FusionException
        {
            SyntaxValue stx = SyntaxStruct.make(eval, loc, this);

            // TODO FUSION-329 This should retain context, but not push it
            //      down to the current children (which already have it).
            //return Syntax.applyContext(eval, context, stx);

            return stx;
        }


        @Override
        public int size()
        {
            return 0;
        }

        @Override
        public Set<String> keys(Evaluator eval)
        {
            return Collections.emptySet();
        }

        @Override
        Object annotate(Evaluator eval, BaseSymbol[] annotations)
            throws FusionException
        {
            return new NullStruct(annotations);
        }

        @Override
        public void visitFields(Evaluator eval, StructFieldVisitor visitor)
        {
        }

        @Override
        public NullStruct transformFields(Evaluator eval,
                                          StructFieldVisitor visitor)
        {
            return this;
        }

        @Override
        public boolean hasKey(Evaluator eval, String key)
            throws FusionException
        {
            return false;
        }

        @Override
        public Object elt(Evaluator eval, String field)
            throws FusionException
        {
            return voidValue(eval);
        }

        @Override
        public Object ref(Evaluator eval, String name, Object def)
            throws FusionException
        {
            return bounceDefaultResult(eval, def);
        }

        @Override
        public Object put(Evaluator eval, String key, Object value)
            throws FusionException
        {
            Map<String,Object> map = new HashMap<>(1);
            map.put(key, value);
            return immutableStruct(map, myAnnotations, 1);
        }

        @Override
        public Object putM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            return put(eval, key, value);
        }

        @Override
        public Object puts(Evaluator eval, String key, Object value)
            throws FusionException
        {
            return put(eval, key, value);
        }

        @Override
        public Object putsM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            return put(eval, key, value);
        }

        @Override
        public Object removeKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            return this;
        }

        @Override
        public Object removeKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            return this;
        }

        @Override
        public Object retainKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            return this;
        }

        @Override
        public Object retainKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            return this;
        }

        // TODO: Optimize this. If the other is immutable and has same annotations, return other.
        @Override
        public Object merge(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            if (other.size() == 0) return this;

            MapBasedStruct is = (MapBasedStruct) other;
            return new FunctionalStruct(is.getMap(eval), myAnnotations, other.size());
        }

        @Override
        public Object mergeM(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            return merge(eval, other);
        }

        @Override
        public Object merge1(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            if (other.size() == 0) return this;

            MapBasedStruct is = (MapBasedStruct) other;
            Map<String,Object> map = structImplOneify(is.getMap(eval));
            return new FunctionalStruct(map, myAnnotations, map.size());
        }

        @Override
        public Object merge1M(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            return merge1(eval, other);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return makeBool(eval, right instanceof NullStruct);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            return isAnyNull(eval, right);
        }

        @Override
        public void write(Evaluator eval, Appendable out)
            throws IOException
        {
            writeAnnotations(out, myAnnotations);
            out.append("null.struct");
        }

        @Override
        public void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());
            out.writeNull(IonType.STRUCT);
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            IonStruct is = factory.newNullStruct();
            is.setTypeAnnotations(getAnnotationsAsJavaStrings());
            return is;
        }
    }


    private abstract static class MapBasedStruct
        extends BaseCollection
        implements BaseStruct
    {
        /**
         * We can't use {@link #getMap}().size() because that doesn't count
         * repeated fields.
         */
        int mySize;

        private MapBasedStruct(BaseSymbol[] annotations, int size)
        {
            super(annotations);
            mySize = size;
        }

        /**
         * Gets the implementation map, first injecting elements if needed.
         *
         * @return not null; perhaps empty if this is mutable.
         */
        abstract FunctionalHashTrie<String, Object> getMap(Evaluator eval);

        abstract MapBasedStruct makeSimilar(FunctionalHashTrie<String, Object> map,
                                            BaseSymbol[] annotations,
                                            int size);

        MapBasedStruct makeSimilar(FunctionalHashTrie<String, Object> map)
        {
            return makeSimilar(map, myAnnotations, computeSize(map));
        }

        MapBasedStruct makeSimilar(FunctionalHashTrie<String, Object> map, int size)
        {
            return makeSimilar(map, myAnnotations, size);
        }

        @Override
        public int size()
        {
            return mySize;
        }

        @Override
        public Set<String> keys(Evaluator eval)
        {
            return Collections.unmodifiableSet(getMap(eval).keySet());
        }

        @Override
        public void visitFields(Evaluator eval, StructFieldVisitor visitor)
            throws FusionException
        {
            for (Map.Entry<String, Object> entry : getMap(eval).entrySet())
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                if (value instanceof Object[])
                {
                    for (Object element : (Object[]) value)
                    {
                        if (visitor.visit(fieldName, element) != null) return;
                    }
                }
                else
                {
                    if (visitor.visit(fieldName, value) != null) return;
                }
            }
        }


        @Override
        public
        NonNullImmutableStruct transformFields(Evaluator eval,
                                               StructFieldVisitor visitor)
            throws FusionException
        {
            boolean mustReplaceThis = (this instanceof MutableStruct);

            if (mySize == 0 && !mustReplaceThis)
            {
                return (NonNullImmutableStruct) this;
            }

            // Replace children in map as necessary.
            FunctionalHashTrie<String, Object> oldMap = getMap(eval);
            FunctionalHashTrie<String, Object> newMap = oldMap;

            for (Map.Entry<String, Object> entry : oldMap.entrySet())
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                if (value instanceof Object[])
                {
                    Object[] children = (Object[]) value;
                    int childCount = children.length;

                    boolean mustReplaceArray = false;
                    Object[] newChildren = new Object[childCount];
                    for (int i = 0; i < childCount; i++)
                    {
                        Object child = children[i];
                        Object newChild = visitor.visit(fieldName, child);
                        if (newChild != child)
                        {
                            mustReplaceArray = true;
                        }
                        newChildren[i] = newChild;
                    }

                    if (mustReplaceArray)
                    {
                        newMap = newMap.with(entry.getKey(), newChildren);
                        mustReplaceThis = true;
                    }
                }
                else
                {
                    Object newChild = visitor.visit(fieldName, value);
                    if (newChild != value)
                    {
                        newMap = newMap.with(entry.getKey(), newChild);
                        mustReplaceThis = true;
                    }
                }
            }

            if (! mustReplaceThis)
            {
                return (NonNullImmutableStruct) this;
            }

            return new FunctionalStruct(newMap, myAnnotations);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            throw new IllegalStateException("Cannot wrap mutable struct as syntax");
        }

        @SuppressWarnings("serial")
        private static final class VisitFailure extends RuntimeException
        {
        }

        /**
         * TODO FUSION-242 This needs to do cycle detection.
         *
         * @return null if an element can't be converted into syntax.
         */
        @Override
        SyntaxValue datumToSyntaxMaybe(final Evaluator      eval,
                                       final SyntaxSymbol   context,
                                       final SourceLocation loc)
            throws FusionException
        {
            StructFieldVisitor visitor = new StructFieldVisitor()
            {
                @Override
                public Object visit(String name, Object value)
                    throws FusionException
                {
                    SyntaxValue converted =
                        Syntax.datumToSyntaxMaybe(eval, value, context, loc);
                    if (converted == null)
                    {
                        // Hit something that's not syntax-able
                        throw new VisitFailure();
                    }
                    return converted;
                }
            };

            try
            {
                ImmutableStruct datum = transformFields(eval, visitor);
                return SyntaxStruct.make(eval, loc, datum);
            }
            catch (VisitFailure e)  // This is crazy.
            {
                return null;
            }
        }

        @Override
        public boolean hasKey(Evaluator eval, String key)
            throws FusionException
        {
            // There's no real need to inject, but otherwise it's hard to find
            // a good way to synchronize properly to read from the map.
            return getMap(eval).get(key) != null;
        }

        Object get(Evaluator eval, String fieldName)
        {
            Object result = getMap(eval).get(fieldName);
            if (result instanceof Object[])
            {
                return ((Object[]) result)[0];
            }

            return result;
        }

        @Override
        public Object elt(Evaluator eval, String field)
            throws FusionException
        {
            Object result = getMap(eval).get(field);
            if (result == null)
            {
                return voidValue(eval);
            }
            if (result instanceof Object[])
            {
                return ((Object[]) result)[0];
            }

            return result;
        }

        @Override
        public Object ref(Evaluator eval, String name, Object def)
            throws FusionException
        {
            Object result = getMap(eval).get(name);
            if (result == null)
            {
                return bounceDefaultResult(eval, def);
            }
            if (result instanceof Object[])
            {
                return ((Object[]) result)[0];
            }

            return result;
        }

        @Override
        public Object put(Evaluator eval, String key, Object value)
            throws FusionException
        {
            FunctionalHashTrie<String, Object> map = getMap(eval);
            Object prior = map.get(key);
            int newSize = size() + computeSizeDifference(prior);

            FunctionalHashTrie<String, Object> newMap = map.with(key, value);

            return makeSimilar(newMap, newSize);
        }

        @Override
        public Object puts(Evaluator eval, String key, Object value)
            throws FusionException
        {
            FunctionalHashTrie<String, Object> map = getMap(eval);
            FunctionalHashTrie<String, Object> newMap = structImplAdd(map, key, value);

            return makeSimilar(newMap, size() + 1);
        }

        @Override
        public Object removeKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            if (keys.length == 0)
            {
                return this;
            }

            int newSize = mySize;
            FunctionalHashTrie<String, Object> oldMap = getMap(eval);
            FunctionalHashTrie<String, Object> newMap = oldMap;

            for (String key : keys)
            {
                Object old = newMap.get(key);
                if (old != null)
                {
                    if (old instanceof Object[])
                    {
                        newSize -= ((Object[]) old).length;
                    }
                    else
                    {
                        newSize--;
                    }
                    newMap = newMap.without(key);
                }
            }

            if (newMap == oldMap)
            {
                return this;
            }

            return makeSimilar(newMap, newSize);
        }

        @Override
        public Object retainKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            if (keys.length == 0)
            {
                return makeSimilar(FunctionalHashTrie.EMPTY, 0);
            }

            FunctionalHashTrie<String, Object> oldMap = getMap(eval);
            FunctionalHashTrie<String, Object> newMap = oldMap;

            Set<String> keysToRetain = new HashSet<>(Arrays.asList(keys));
            Set<String> existingKeys = oldMap.keySet();

            for (String key : existingKeys)
            {
                if (!keysToRetain.contains(key))
                {
                    newMap = newMap.without(key);
                }
            }

            if (newMap == oldMap)
            {
                return this;
            }

            return makeSimilar(newMap);
        }

        @Override
        public Object merge(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            if (other.size() == 0) return this;

            FunctionalHashTrie<String, Object> newMap = getMap(eval);

            // We know it has children.
            MapBasedStruct is = (MapBasedStruct) other;
            for (Map.Entry<String,Object> entry : is.getMap(eval).entrySet())
            {
                String key   = entry.getKey();
                Object value = entry.getValue();
                newMap = structImplAdd(newMap, key, value);
            }

            return makeSimilar(newMap, this.size() + other.size());
        }

        @Override
        public Object merge1(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            FunctionalHashTrie origMap = getMap(eval);
            FunctionalHashTrie newMap = structImplOneify(origMap);

            if (other.size() == 0)
            {
                if (newMap == origMap) return this;
            }
            else  // Other is not empty, we are going to make a change
            {
                MapBasedStruct is = (MapBasedStruct) other;
                newMap = structImplMerge1(newMap, is.getMap(eval));
            }

            return makeSimilar(newMap, newMap.size());
        }

        @Override
        BaseBool strictEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof MapBasedStruct)
            {
                MapBasedStruct rs = (MapBasedStruct) right;
                return rs.actualStructEqual(eval, STRICT_EQUAL, this);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool tightEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof MapBasedStruct)
            {
                MapBasedStruct rs = (MapBasedStruct) right;
                return rs.actualStructEqual(eval, TIGHT_EQUAL, this);
            }
            return falseBool(eval);
        }

        @Override
        BaseBool looseEquals(Evaluator eval, Object right)
            throws FusionException
        {
            if (right instanceof MapBasedStruct)
            {
                MapBasedStruct rs = (MapBasedStruct) right;
                return rs.actualStructEqual(eval, LOOSE_EQUAL, this);
            }
            return falseBool(eval);
        }

        private BaseBool actualStructEqual(Evaluator eval,
                                           EqualityTier tier,
                                           MapBasedStruct left)
            throws FusionException
        {
            if (size() != left.size()) return falseBool(eval);

            Map<String, Object> leftMap = left.getMap(eval);

            for (Map.Entry<String, Object> entry : getMap(eval).entrySet())
            {
                String fieldName = entry.getKey();

                Object lv = leftMap.get(fieldName);
                Object rv = entry.getValue();
                if (lv instanceof Object[])
                {
                    if (! (rv instanceof Object[])) return falseBool(eval);

                    Object[] lArray = (Object[]) lv;
                    Object[] rArray = (Object[]) rv;

                    int lCount = lArray.length;
                    int rCount = rArray.length;
                    if (lCount != rCount) return falseBool(eval);

                    rArray = Arrays.copyOf(rArray, rCount);
                    for (int i = 0; i < lCount; i++)
                    {
                        lv = lArray[i];

                        // Seek a matching element from rArray
                        boolean found = false;
                        for (int j = 0; j < rCount; j++)
                        {
                            rv = rArray[j];
                            BaseBool b = tier.eval(eval, lv, rv);
                            if (b.isTrue())
                            {
                                found = true;
                                rArray[j] = rArray[--rCount];
                                break;
                            }
                        }

                        if (!found) return falseBool(eval);
                    }

                    // By now we've found a match for everything!
                    assert rCount == 0;
                }
                else
                {
                    if (rv instanceof Object[]) return falseBool(eval);

                    BaseBool b = tier.eval(eval, lv, rv);
                    if (b.isFalse()) return b;
                }
            }

            return trueBool(eval);
        }

        /**
         * Unlike {@link #ionize} and {@link #copyToIonValue}, here we route
         * through {@link #getMap} rather than writing any lazily-injected
         * IonStruct directly. That's to ensure that the output looks the same
         * in both cases.
         */
        @Override
        public void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            writeAnnotations(out, myAnnotations);
            out.append('{');

            boolean comma = false;
            for (Map.Entry<String, Object> entry : getMap(eval).entrySet())
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                if (value instanceof Object[])
                {
                    for (Object element : (Object[]) value)
                    {
                        if (comma) out.append(',');

                        printSymbol(out, fieldName);
                        out.append(':');
                        dispatchWrite(eval, out, element);
                        comma = true;
                    }
                }
                else
                {
                    if (comma) out.append(',');

                    printSymbol(out, fieldName);
                    out.append(':');
                    dispatchWrite(eval, out, value);
                    comma = true;
                }
            }
            out.append('}');
        }

        @Override
        public void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());

            out.stepIn(IonType.STRUCT);
            for (Map.Entry<String, Object> entry : getMap(eval).entrySet())
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                if (value instanceof Object[])
                {
                    Object[] children = (Object[]) value;
                    for (Object child : children)
                    {
                        out.setFieldName(fieldName);
                        dispatchIonize(eval, out, child);
                    }
                }
                else
                {
                    out.setFieldName(fieldName);
                    dispatchIonize(eval, out, value);
                }
            }
            out.stepOut();
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            IonStruct is = factory.newEmptyStruct();
            is.setTypeAnnotations(getAnnotationsAsJavaStrings());

            for (Map.Entry<String, Object> entry : getMap(null).entrySet())
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                if (value instanceof Object[])
                {
                    Object[] children = (Object[]) value;
                    for (Object child : children)
                    {
                        IonValue ion =
                            copyToIonValue(child, factory,
                                           throwOnConversionFailure);
                        if (ion == null) return null;
                        is.add(fieldName, ion);
                    }
                }
                else
                {
                    IonValue ion = copyToIonValue(value, factory,
                                                  throwOnConversionFailure);
                    if (ion == null) return null;
                    is.add(fieldName, ion);
                }
            }

            return is;
        }

        protected int computeSizeDifference(Object prior)
        {
            if (prior == null)
            {
                return 1;
            }
            else if (prior instanceof Object[])
            {
                return 1 - ((Object[]) prior).length;
            }
            else
            {
                return 0;
            }
        }
    }


    static abstract class NonNullImmutableStruct
        extends MapBasedStruct
        implements ImmutableStruct
    {
        private NonNullImmutableStruct(BaseSymbol[] annotations, int size)
        {
            super(annotations, size);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxStruct.makeOriginal(eval, loc, this);
        }

        @Override
        MapBasedStruct makeSimilar(FunctionalHashTrie<String, Object> map,
                                   BaseSymbol[] annotations,
                                   int size)
        {
            return immutableStruct(map, annotations, size);
        }

        @Override
        Object annotate(Evaluator eval, BaseSymbol[] annotations)
            throws FusionException
        {
            return makeSimilar(getMap(eval), annotations, size());
        }

        @Override
        public Object putM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            return put(eval, key, value);
        }

        @Override
        public Object putsM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            return puts(eval, key, value);
        }

        @Override
        public Object removeKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            return removeKeys(eval, keys);
        }

        @Override
        public Object retainKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            return retainKeys(eval, keys);
        }

        @Override
        public Object mergeM(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            return merge(eval, other);
        }

        @Override
        public Object merge1M(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            return merge1(eval, other);
        }
    }

    private static class FunctionalStruct
        extends NonNullImmutableStruct
    {
        /**
         * For repeated fields, the value is Object[] otherwise it's a
         * non-array Object.
         */
        private final FunctionalHashTrie<String, Object> myMap;


        public FunctionalStruct(FunctionalHashTrie<String, Object> map,
                                BaseSymbol[] annotations,
                                int size)
        {
            super(annotations, size);
            myMap = map;
        }

        public FunctionalStruct(Map<String, Object> map,
                                BaseSymbol[] annotations)
        {
            super(annotations, computeSize(map));
            myMap = FunctionalHashTrie.create(map);
        }

        public FunctionalStruct(Map<String, Object> map,
                                BaseSymbol[] annotations,
                                int size)
        {
            super(annotations, size);
            myMap = FunctionalHashTrie.create(map);
        }

        @Override
        FunctionalHashTrie<String, Object> getMap(Evaluator eval)
        {
            return myMap;
        }
    }

    private static final class LazyInjectingStruct
        extends NonNullImmutableStruct
    {
        /**
         * For repeated fields, the value is Object[] otherwise it's a
         * non-array Object.
         * <p>
         * This map is empty until an element is accessed.
         * Access to this field should be routed through {@link #getMap}
         * to force injection into this map!</b>
         */
        FunctionalHashTrie<String, Object> myMap;
        private IonStruct myIonStruct;

        public LazyInjectingStruct(BaseSymbol[] annotations, IonStruct struct)
        {
            super(annotations, struct.size());
            myIonStruct = struct;
        }

        private synchronized IonStruct getIonStruct()
        {
            return myIonStruct;
        }

        private Iterator<Map.Entry<String, Object>> makeInjectingIterator(final Evaluator eval)
        {
            final Iterator<IonValue> internal = getIonStruct().iterator();
            return new Iterator<Map.Entry<String, Object>>()
            {
                @Override
                public boolean hasNext()
                {
                    return internal.hasNext();
                }

                @Override
                public Map.Entry<String, Object> next()
                {
                    IonValue next = internal.next();
                    return new SimpleEntry<>(next.getFieldName(),
                                             eval.inject(next));
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * Synchronized so this immutable class is thread-safe for reads.
         */
        @Override
        synchronized FunctionalHashTrie<String, Object> getMap(Evaluator eval)
        {
            if (myIonStruct != null)
            {
                myMap = FunctionalHashTrie.merge(makeInjectingIterator(eval),
                                                 STRUCT_MERGE_FUNCTION);
                myIonStruct = null;
            }

            return myMap;
        }

        @Override
        public void write(Evaluator eval, Appendable out)
            throws IOException, FusionException
        {
            IonStruct s = getIonStruct();
            if (s != null)
            {
                IonWriter iw = WRITER_BUILDER.build(out);
                s.writeTo(iw);
                iw.finish();
            }
            else
            {
                super.write(eval, out);
            }
        }

        @Override
        public void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            IonStruct s = getIonStruct();
            if (s != null)
            {
                s.writeTo(out);
            }
            else
            {
                super.ionize(eval, out);
            }
        }

        @Override
        IonValue copyToIonValue(ValueFactory factory,
                                boolean throwOnConversionFailure)
            throws FusionException
        {
            IonStruct s = getIonStruct();
            if (s != null)
            {
                return factory.clone(s);
            }

            return super.copyToIonValue(factory, throwOnConversionFailure);
        }
    }


    private static final class MutableStruct
        extends MapBasedStruct
    {
        /**
         * For repeated fields, the value is Object[] otherwise it's a
         * non-array Object.
         */
        private FunctionalHashTrie<String, Object> myMap;

        public MutableStruct(Map<String, Object> map,
                             BaseSymbol[] annotations)
        {
            super(annotations, computeSize(map));
            myMap = FunctionalHashTrie.create(map);
        }

        public MutableStruct(FunctionalHashTrie<String, Object> map,
                             BaseSymbol[] annotations)
        {
            super(annotations, computeSize(map));
            myMap = map;
        }

        public MutableStruct(Map<String, Object> map,
                             BaseSymbol[] annotations,
                             int size)
        {
            super(annotations, size);
            myMap = FunctionalHashTrie.create(map);
        }

        public MutableStruct(FunctionalHashTrie<String, Object> map,
                             BaseSymbol[] annotations,
                             int size)
        {
            super(annotations, size);
            myMap = map;
        }

        /**
         * Recomputes {@link #mySize} based on what's in {@link #myMap}.
         * This takes time proportional to the number of unique field names.
         */
        void updateSize()
        {
            mySize = computeSize(myMap);
        }

        @Override
        FunctionalHashTrie<String, Object> getMap(Evaluator eval)
        {
            return myMap;
        }

        @Override
        MapBasedStruct makeSimilar(FunctionalHashTrie<String, Object> map,
                                   BaseSymbol[] annotations,
                                   int size)
        {
            return new MutableStruct(map, annotations, size);
        }

        @Override
        Object annotate(Evaluator eval, BaseSymbol[] annotations)
            throws FusionException
        {
            return makeSimilar(getMap(eval), annotations, size());
        }

        @Override
        public Object putM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            Object prior = myMap.get(key);

            myMap = myMap.with(key, value);
            mySize += computeSizeDifference(prior);

            return this;
        }

        @Override
        public Object putsM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            myMap = structImplAdd(myMap, key, value);
            mySize++;

            return this;
        }

        @Override
        public Object removeKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            if (keys.length != 0)
            {
                for (String key : keys)
                {
                    Object old = myMap.get(key);
                    if (old != null)
                    {
                        if (old instanceof Object[])
                        {
                            mySize -= ((Object[]) old).length;
                        }
                        else
                        {
                            mySize--;
                        }
                        myMap = myMap.without(key);
                    }
                }
            }

            return this;
        }

        @Override
        public Object retainKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            if (keys.length == 0)
            {
                myMap = FunctionalHashTrie.EMPTY;
                mySize = 0;
            }
            else
            {
                Set<String> keysToRetain = new HashSet<>(Arrays.asList(keys));
                Set<String> existingKeys = myMap.keySet();

                for (String key : existingKeys)
                {
                    if (!keysToRetain.contains(key))
                    {
                        myMap = myMap.without(key);
                    }
                }
                updateSize();
            }

            return this;
        }

        @Override
        public Object mergeM(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            int otherSize = other.size();
            if (otherSize != 0)
            {
                MapBasedStruct is = (MapBasedStruct) other;
                myMap = structImplMerge(myMap, is.getMap(eval));
                mySize += otherSize;
            }

            return this;
        }

        @Override
        public Object merge1M(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            // Remove any existing repeated fields.
            myMap = structImplOneify(myMap);

            if (other.size() != 0)
            {
                MapBasedStruct is = (MapBasedStruct) other;
                myMap = structImplMerge1(myMap, is.getMap(eval));
            }

            mySize = myMap.size();
            return this;
        }
    }


    //========================================================================
    // Procedure Helpers


    /**
     * @param expectation must not be null.
     * @return the Fusion struct, not null.
     */
    static Object checkStructArg(Evaluator eval,
                                 Procedure who,
                                 String    expectation,
                                 int       argNum,
                                 Object... args)
        throws FusionException, ArgumentException
    {
        Object arg = args[argNum];
        if (arg instanceof BaseStruct)
        {
            return arg;
        }

        throw who.argFailure(expectation, argNum, args);
    }


    /**
     * @return the Fusion struct, not null.
     */
    static Object checkNullableStructArg(Evaluator eval,
                                         Procedure who,
                                         int       argNum,
                                         Object... args)
        throws FusionException, ArgumentException
    {
        String expectation = "nullable struct";
        return checkStructArg(eval, who, expectation, argNum, args);
    }


    //========================================================================
    // Procedures


    static final class IsStructProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object arg)
            throws FusionException
        {
            boolean result = isStruct(eval, arg);
            return makeBool(eval, result);
        }
    }



    static final class IsImmutableStructProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isImmutableStruct(eval, value);
            return makeBool(eval, result);
        }
    }



    static final class IsMutableStructProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object value)
            throws FusionException
        {
            boolean result = isMutableStruct(eval, value);
            return makeBool(eval, result);
        }
    }



    static final class UnsafeStructHasKeyProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object s, Object k)
            throws FusionException
        {
            BaseStruct struct = (BaseStruct) s;
            String key = unsafeTextToJavaString(eval, k);
            return makeBool(eval, struct.hasKey(eval, key));
        }
    }



    static final class UnsafeStructRefProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            BaseStruct s = (BaseStruct) args[0];
            String name = unsafeTextToJavaString(eval, args[1]);
            return s.ref(eval, name, args[2]);
        }
    }



    static final class UnsafeStructPutProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            String key = unsafeTextToJavaString(eval, args[1]);
            return unsafeStructPut(eval, args[0], key, args[2]);
        }
    }



    static final class UnsafeStructPutMProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            String key = unsafeTextToJavaString(eval, args[1]);
            return unsafeStructPutM(eval, args[0], key, args[2]);
        }
    }



    static final class UnsafeStructPutsProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            String key = unsafeTextToJavaString(eval, args[1]);
            return unsafeStructPuts(eval, args[0], key, args[2]);
        }
    }

    static final class UnsafeStructPutsMProc
        extends Procedure
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            String key = unsafeTextToJavaString(eval, args[1]);
            return unsafeStructPutsM(eval, args[0], key, args[2]);
        }
    }



    private static final class StructIterator
        extends AbstractIterator
    {
        private final Iterator<Map.Entry<String,Object>> myEntryIterator;
        private Iterator<Object>                         myMultiIterator;
        private Object                                   myCurrentKey;

        private StructIterator(Map<String,Object> map)
        {
            myEntryIterator = map.entrySet().iterator();
        }

        @Override
        boolean hasNext(Evaluator eval)
            throws FusionException
        {
            if (myMultiIterator != null)
            {
                if (myMultiIterator.hasNext())
                {
                    return true;
                }
                myMultiIterator = null;
            }
            return myEntryIterator.hasNext();
        }

        @Override
        Object next(Evaluator eval)
            throws FusionException
        {
            Object value;
            if (myMultiIterator != null)
            {
                value = myMultiIterator.next();
            }
            else
            {
                Map.Entry<String, Object> entry = myEntryIterator.next();
                myCurrentKey = makeSymbol(eval, entry.getKey());

                value = entry.getValue();
                if (value instanceof Object[])
                {
                    Object[] vals = (Object[]) value;
                    myMultiIterator = Arrays.asList(vals).iterator();

                    // Safe since we have at least 1 element in the array:
                    value = myMultiIterator.next();
                }
            }

            // TODO route multi-values through the evaluator
            return new Object[] { myCurrentKey, value };
        }
    }

    static final class UnsafeStructIteratorProc
        extends Procedure1
    {
        @Override
        Object doApply(final Evaluator eval, Object struct)
            throws FusionException
        {
            BaseStruct s = (BaseStruct) struct;
            if (s.size() == 0)
            {
                return iterate(eval, Arrays.asList().iterator());
            }

            Map<String, Object> map = ((MapBasedStruct) s).getMap(eval);
            return new StructIterator(map);
        }
    }



    abstract static class BaseStructProc
        extends Procedure
    {
        void checkArityEven(Object... args)
            throws FusionException
        {
            if ((args.length % 2) == 1)
            {
                String message =
                    "Expected even number of args, observed " + args.length;
                throw contractFailure(message);
            }
        }

        abstract Object makeIt(String[] names, Object[] values)
            throws FusionException;

        @Override
        final Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityEven(args);

            int fieldCount = (args.length / 2);
            String[] names  = new String[fieldCount];
            Object[] values = new Object[fieldCount];

            int fieldPos = 0;
            for (int i = 0; i < args.length; i++, fieldPos++)
            {
                names [fieldPos] = checkNonEmptyTextArg(eval, this, i, args);
                values[fieldPos] = args[++i];
            }
            assert fieldPos == fieldCount;

            return makeIt(names, values);
        }
    }



    static final class StructProc
        extends BaseStructProc
    {
        @Override
        Object makeIt(String[] names, Object[] values)
            throws FusionException
        {
            return immutableStruct(names, values, EMPTY_STRING_ARRAY);
        }
    }



    static final class MutableStructProc
        extends BaseStructProc
    {
        @Override
        Object makeIt(String[] names, Object[] values)
            throws FusionException
        {
            return mutableStruct(names, values, EMPTY_STRING_ARRAY);
        }
    }



    static final class StructMergeProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object struct1, Object struct2)
            throws FusionException
        {
            checkNullableStructArg(eval, this, 0, struct1, struct2);
            checkNullableStructArg(eval, this, 1, struct1, struct2);

            return unsafeStructMerge(eval, struct1, struct2);
        }
    }

    static final class StructMergeMProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object struct1, Object struct2)
            throws FusionException
        {
            checkNullableStructArg(eval, this, 0, struct1, struct2);
            checkNullableStructArg(eval, this, 1, struct1, struct2);

            return unsafeStructMergeM(eval, struct1, struct2);
        }
    }



    static final class StructMerge1Proc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object struct1, Object struct2)
            throws FusionException
        {
            checkNullableStructArg(eval, this, 0, struct1, struct2);
            checkNullableStructArg(eval, this, 1, struct1, struct2);

            return unsafeStructMerge1(eval, struct1, struct2);
        }
    }

    static final class StructMerge1MProc
        extends Procedure2
    {
        @Override
        Object doApply(Evaluator eval, Object struct1, Object struct2)
            throws FusionException
        {
            checkNullableStructArg(eval, this, 0, struct1, struct2);
            checkNullableStructArg(eval, this, 1, struct1, struct2);

            return unsafeStructMerge1M(eval, struct1, struct2);
        }
    }



    static abstract class AbstractZipProc
        extends Procedure
    {
        final FunctionalHashTrie<String,Object> _doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);
            Object names =  checkNullableListArg(eval, this, 0, args);
            Object values = checkNullableListArg(eval, this, 1, args);

            Iterator<?> fieldIterator = unsafeJavaIterate(eval, names);
            Iterator<?> valueIterator = unsafeJavaIterate(eval, values);

            FunctionalHashTrie<String, Object> map = FunctionalHashTrie.EMPTY;

            while (fieldIterator.hasNext() && valueIterator.hasNext())
            {
                Object nameObj = fieldIterator.next();
                String name = textToJavaString(eval, nameObj);
                if (name == null || name.isEmpty())
                {
                    String expectation =
                        "sequence of non-empty strings or symbols";
                    throw new ArgumentException(this, expectation, 0, args);
                }

                Object valueObj = valueIterator.next();
                map = structImplAdd(map, name, valueObj);
            }

            return map;
        }
    }


    static final class StructZipProc
        extends AbstractZipProc
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return immutableStruct(_doApply(eval, args));
        }
    }


    static final class MutableStructZipProc
        extends AbstractZipProc
    {
        @Override
        Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            return mutableStruct(_doApply(eval, args));
        }
    }



    abstract static class BaseKeysProc
        extends Procedure
    {
        abstract Object doIt(Evaluator eval, Object struct, String[] keys)
            throws FusionException;

        @Override
        final Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityAtLeast(1, args);

            Object struct = checkNullableStructArg(eval, this, 0, args);

            String[] keys = new String[args.length - 1];
            for (int i = 1; i < args.length; i++)
            {
                keys[i-1] = checkNonEmptyTextArg(eval, this, i, args);
            }

            return doIt(eval, struct, keys);
        }
    }



    static final class RemoveKeysProc
        extends BaseKeysProc
    {
        @Override
        Object doIt(Evaluator eval, Object struct, String[] keys)
            throws FusionException
        {
            return unsafeStructRemoveKeys(eval, struct, keys);
        }
    }



    static final class RemoveKeysMProc
        extends BaseKeysProc
    {
        @Override
        Object doIt(Evaluator eval, Object struct, String[] keys)
            throws FusionException
        {
             return unsafeStructRemoveKeysM(eval, struct, keys);
        }
    }



    static final class RetainKeysProc
        extends BaseKeysProc
    {
        @Override
        Object doIt(Evaluator eval, Object struct, String[] keys)
            throws FusionException
        {
            return unsafeStructRetainKeys(eval, struct, keys);
        }
    }



    static final class RetainKeysMProc
        extends BaseKeysProc
    {
        @Override
        Object doIt(Evaluator eval, Object struct, String[] keys)
            throws FusionException
        {
            return unsafeStructRetainKeysM(eval, struct, keys);
        }
    }
}
