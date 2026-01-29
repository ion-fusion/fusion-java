// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static com.amazon.ion.util.IonTextUtils.printSymbol;
import static dev.ionfusion.fusion.FusionBool.falseBool;
import static dev.ionfusion.fusion.FusionBool.makeBool;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.LOOSE_EQUAL;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.STRICT_EQUAL;
import static dev.ionfusion.fusion.FusionCompare.EqualityTier.TIGHT_EQUAL;
import static dev.ionfusion.fusion.FusionIo.dispatchIonize;
import static dev.ionfusion.fusion.FusionIo.dispatchWrite;
import static dev.ionfusion.fusion.FusionList.checkNullableListArg;
import static dev.ionfusion.fusion.FusionList.unsafeJavaIterate;
import static dev.ionfusion.fusion.FusionList.unsafeListSize;
import static dev.ionfusion.fusion.FusionSymbol.BaseSymbol.internSymbols;
import static dev.ionfusion.fusion.FusionSymbol.makeSymbol;
import static dev.ionfusion.fusion.FusionText.checkRequiredTextArg;
import static dev.ionfusion.fusion.FusionText.textToJavaString;
import static dev.ionfusion.fusion.FusionText.unsafeTextToJavaString;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static java.util.AbstractMap.SimpleEntry;

import com.amazon.ion.IonException;
import com.amazon.ion.IonStruct;
import com.amazon.ion.IonType;
import com.amazon.ion.IonValue;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueFactory;
import dev.ionfusion.fusion.FusionBool.BaseBool;
import dev.ionfusion.fusion.FusionCollection.BaseCollection;
import dev.ionfusion.fusion.FusionCompare.EqualityTier;
import dev.ionfusion.fusion.FusionIterator.AbstractIterator;
import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.util.hamt.MultiHashTrie;
import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;


@SuppressWarnings({"unused", "RedundantThrows", "DuplicateThrows"})
final class FusionStruct
{
    // Go away you big meany!
    private FusionStruct() {}


    private static final NullStruct             NULL_STRUCT  = new NullStruct();
    private static final NonNullImmutableStruct EMPTY_STRUCT =
        new FunctionalStruct(MultiHashTrie.empty(), BaseSymbol.EMPTY_ARRAY);


    //========================================================================
    // Constructors

    static Object structFromIonStruct(Evaluator eval, IonStruct struct)
    {
        String[] annStrings = struct.getTypeAnnotations();
        BaseSymbol[] annotations = internSymbols(annStrings);

        // There's no benefit to being lazy with null.struct or {}.
        if (struct.isNullValue())
        {
            return nullStruct(eval, annotations);
        }

        if (struct.isEmpty())
        {
            return EMPTY_STRUCT.annotate(eval, annotations);
        }

        return new LazyInjectingStruct(annotations, struct);
    }


    static NullStruct nullStruct(Evaluator eval)
    {
        return NULL_STRUCT;
    }


    static NullStruct nullStruct(Evaluator eval, BaseSymbol[] annotations)
    {
        return NULL_STRUCT.annotate(eval, annotations);
    }

    static NullStruct nullStruct(Evaluator eval, String[] annotations)
    {
        return NULL_STRUCT.annotate(eval, annotations);
    }


    static final class Builder
    {
        private final MultiHashTrie.Builder<String, Object> myTrieBuilder =
            MultiHashTrie.builderMulti();

        private Builder() {}

        /**
         * Adds a new field to the struct, retaining existing fields with the
         * same name.
         */
        void add(String fieldName, Object value)
        {
            myTrieBuilder.withMulti(fieldName, value);
        }

        // TODO add(String[], Object[])


        NonNullImmutableStruct buildImmutable()
        {
            return buildImmutable(BaseSymbol.EMPTY_ARRAY);
        }

        NonNullImmutableStruct buildImmutable(String[] annotations)
        {
            return buildImmutable(internSymbols(annotations));
        }

        NonNullImmutableStruct buildImmutable(BaseSymbol[] annotations)
        {
            MultiHashTrie<String, Object> trie = myTrieBuilder.build();
            return immutableStruct(trie, annotations);
        }


        MutableStruct buildMutable()
        {
            return buildMutable(BaseSymbol.EMPTY_ARRAY);
        }

        MutableStruct buildMutable(String[] annotations)
        {
            return buildMutable(internSymbols(annotations));
        }

        MutableStruct buildMutable(BaseSymbol[] annotations)
        {
            MultiHashTrie<String, Object> trie = myTrieBuilder.build();
            return new MutableStruct(trie, annotations);
        }
    }

    static Builder builder(Evaluator eval)
    {
        Objects.requireNonNull(eval);
        return new Builder();
    }


    static NonNullImmutableStruct emptyStruct(Evaluator eval)
        throws FusionException
    {
        return EMPTY_STRUCT;
    }

    static Object immutableStruct(Evaluator eval,
                                  String name,
                                  Object value)
        throws FusionException
    {
        return new FunctionalStruct(MultiHashTrie.singleEntry(name, value),
                                    BaseSymbol.EMPTY_ARRAY);
    }

    private static NonNullImmutableStruct
    immutableStruct(MultiHashTrie<String, Object> map, BaseSymbol[] anns)
    {
        if (map.isEmpty() && anns.length == 0) return EMPTY_STRUCT;
        return new FunctionalStruct(map, anns);
    }

    static NonNullImmutableStruct immutableStruct(String[] names,
                                                  Object[] values,
                                                  BaseSymbol[] anns)
    {
        return immutableStruct(MultiHashTrie.fromArrays(names, values), anns);
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

        return ((MutableStruct) struct).asImmutable();
    }

    // TODO Add asMutableStruct (must throw given null.struct!)

    /**
     * Returns a new, empty mutable struct.  This is equivalent to the Fusion
     * code {@code (mutable_struct)}.
     */
    static Object mutableStruct(Evaluator eval)
        throws FusionException
    {
        return new MutableStruct();
    }

    static Object mutableStruct(Evaluator eval, String name, Object value)
        throws FusionException
    {
        return new MutableStruct(MultiHashTrie.singleEntry(name, value),
                                 BaseSymbol.EMPTY_ARRAY);
    }


    //========================================================================
    // Utilities


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


    /**
     * Visit all fields of a struct, in no particular order.
     * If the visitor returns a non-null result, no more fields will be visited.
     */
    static void unsafeStructFieldVisit(Evaluator eval, Object struct,
                                       StructFieldVisitor visitor)
        throws FusionException
    {
        ((BaseStruct) struct).visitFields(eval, visitor);
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


    interface StructFieldVisitor
    {
        /**
         * @return The meaning of the return value depends on the invoking
         * process.
         */
        Object visit(String name, Object value)
            throws FusionException;
    }

    @SuppressWarnings("serial")
    static class TunneledFusionException
        extends RuntimeException
    {
        private final FusionException myFusionException;

        TunneledFusionException(FusionException e)
        {
            myFusionException = e;
        }

        public FusionException getFusionException()
        {
            return myFusionException;
        }
    }


    /**
     * Adapts {@link StructFieldVisitor} to HAMT transform visitor.
     */
    private static class StructFieldXform
        implements BiFunction<String, Object, Object>
    {
        private final StructFieldVisitor visitor;

        public StructFieldXform(StructFieldVisitor visitor)
        {
            this.visitor = visitor;
        }

        @Override
        public Object apply(String key, Object value)
        {
            try
            {
                return visitor.visit(key, value);
            }
            catch (FusionException e)
            {
                throw new TunneledFusionException(e);
            }
        }
    }


    interface BaseStruct<Self extends BaseStruct<Self>>
        extends AnnotatableValue<Self>
    {
        void ionize(Evaluator eval, IonWriter out)
            throws IOException, IonException, FusionException, IonizeFailure;

        void write(Evaluator eval, Appendable out)
            throws IOException, FusionException;

        int size(); // Doesn't throw

        Set<String> keys(Evaluator eval);

        /**
         * Gets the implementation map, first injecting elements if needed.
         *
         * @return not null; perhaps empty if this is mutable.
         */
        MultiHashTrie<String, Object> getMap(Evaluator eval);

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


    interface ImmutableStruct<Self extends ImmutableStruct<Self>>
        extends BaseStruct<Self>
    {
    }


    private static final class NullStruct
        extends BaseCollection<NullStruct>
        implements ImmutableStruct<NullStruct>
    {
        private NullStruct() {}

        private NullStruct(BaseSymbol[] annotations)
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

            // TODO This should retain context, but not push it
            //      down to the current children (which already have it).
            //      https://github.com/ion-fusion/fusion-java/issues/68
            //return Syntax.applyContext(eval, context, stx);

            return stx;
        }


        @Override
        public int size(Evaluator eval)
        {
            return 0;
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
        public MultiHashTrie<String, Object> getMap(Evaluator eval)
        {
            return MultiHashTrie.empty();
        }

        @Override
        public NullStruct annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            if (annotations.length == 0) return NULL_STRUCT;
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
            return new FunctionalStruct(key, value, myAnnotations);
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

            return new FunctionalStruct(other.getMap(eval), myAnnotations);
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

            MultiHashTrie<String, Object> map = other.getMap(eval).oneify();
            return new FunctionalStruct(map, myAnnotations);
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


    private abstract static class MapBasedStruct<Self extends MapBasedStruct<Self>>
        extends BaseCollection<Self>
        implements BaseStruct<Self>
    {
        private MapBasedStruct(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        abstract MapBasedStruct makeSimilar(MultiHashTrie<String, Object> map,
                                            BaseSymbol[] annotations);

        MapBasedStruct makeSimilar(MultiHashTrie<String, Object> map)
        {
            return makeSimilar(map, myAnnotations);
        }

        @Override
        public final int size(Evaluator eval)
        {
            return size();
        }

        @Override
        public abstract int size();

        @Override
        public Set<String> keys(Evaluator eval)
        {
            return Collections.unmodifiableSet(getMap(eval).keySet());
        }

        // TODO Add visitation to HAMT.
        @Override
        public void visitFields(Evaluator eval, StructFieldVisitor visitor)
            throws FusionException
        {
            for (Map.Entry<String, Object> entry : getMap(eval))
            {
                String fieldName = entry.getKey();
                Object value     = entry.getValue();

                if (visitor.visit(fieldName, value) != null) return;
            }
        }


        @Override
        public
        NonNullImmutableStruct transformFields(Evaluator eval,
                                               StructFieldVisitor visitor)
            throws FusionException
        {
            try
            {
                MultiHashTrie<String, Object> oldMap = getMap(eval);
                MultiHashTrie<String, Object> newMap =
                    oldMap.transform(new StructFieldXform(visitor));
                if (newMap != oldMap || this instanceof MutableStruct)
                {
                    return new FunctionalStruct(newMap, myAnnotations);
                }
                return (NonNullImmutableStruct) this;
            }
            catch (TunneledFusionException e)
            {
                throw e.getFusionException();
            }
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            throw new IllegalStateException("Cannot wrap mutable struct as syntax");
        }

        @SuppressWarnings("serial")
        private static final class VisitFailure
            extends RuntimeException
        { }

        /**
         * TODO This needs to do cycle detection.
         *   https://github.com/ion-fusion/fusion-java/issues/65
         *
         * @return null if an element can't be converted into syntax.
         */
        @Override
        SyntaxValue datumToSyntaxMaybe(final Evaluator      eval,
                                       final SyntaxSymbol   context,
                                       final SourceLocation loc)
            throws FusionException
        {
            StructFieldVisitor visitor = (name, value) -> {
                SyntaxValue converted = Syntax.datumToSyntaxMaybe(eval, value, context, loc);
                if (converted == null)
                {
                    // Hit something that's not syntax-able
                    throw new VisitFailure();
                }
                return converted;
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
            return getMap(eval).containsKey(key);
        }

        Object get(Evaluator eval, String fieldName)
        {
            return getMap(eval).get(fieldName);
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
            return result;
        }

        @Override
        public Object put(Evaluator eval, String key, Object value)
            throws FusionException
        {
            // `put` replaces existing values at the key; don't allow duplicates.
            MultiHashTrie<String, Object> newMap = getMap(eval).with1(key, value);

            return makeSimilar(newMap);
        }

        @Override
        public Object puts(Evaluator eval, String key, Object value)
            throws FusionException
        {
            MultiHashTrie<String, Object> newMap = getMap(eval).withMulti(key, value);

            return makeSimilar(newMap);
        }

        @Override
        public Object removeKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            MultiHashTrie<String, Object> oldMap = getMap(eval);
            MultiHashTrie<String, Object> newMap = oldMap.withoutKeys(keys);

            if (newMap == oldMap) return this;

            return makeSimilar(newMap);
        }

        @Override
        public Object retainKeys(Evaluator eval, String[] keys)
            throws FusionException
        {
            MultiHashTrie<String, Object> oldMap = getMap(eval);
            MultiHashTrie<String, Object> newMap =
                MultiHashTrie.fromSelectedKeys(oldMap, keys);

            return makeSimilar(newMap);
        }

        @Override
        public Object merge(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            if (other.size() == 0) return this;

            MultiHashTrie<String, Object> otherMap = other.getMap(eval);
            MultiHashTrie<String, Object> origMap = getMap(eval);
            MultiHashTrie<String, Object> newMap = origMap.mergeMulti(otherMap);

            return makeSimilar(newMap);
        }

        @Override
        public Object merge1(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            MultiHashTrie<String, Object> otherMap = other.getMap(eval);
            MultiHashTrie<String, Object> origMap = getMap(eval);
            MultiHashTrie<String, Object> newMap  = origMap.merge1(otherMap);

            if (newMap == origMap) return this;

            return makeSimilar(newMap);
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

        private BaseBool actualStructEqual(final Evaluator eval,
                                           final EqualityTier tier,
                                           final MapBasedStruct left)
            throws FusionException
        {
            BiPredicate<Object, Object> comp = (lv, rv) -> {
                try
                {
                    return tier.eval(eval, lv, rv).isTrue();
                }
                catch (FusionException e)
                {
                    throw new TunneledFusionException(e);
                }
            };

            try
            {
                MultiHashTrie<String, Object> leftMap  = left.getMap(eval);
                MultiHashTrie<String, Object> rightMap = getMap(eval);

                boolean result = leftMap.equals(rightMap, comp);
                return makeBool(eval, result);
            }
            catch (TunneledFusionException e)
            {
                throw e.getFusionException();
            }
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

            // TODO PERF: Visitor would be more efficient
            boolean comma = false;
            for (Map.Entry<String, Object> entry : getMap(eval))
            {
                String fieldName = entry.getKey();
                Object value     = entry.getValue();

                if (comma) out.append(',');

                printSymbol(out, fieldName);
                out.append(':');
                dispatchWrite(eval, out, value);
                comma = true;
            }
            out.append('}');
        }

        @Override
        public void ionize(Evaluator eval, IonWriter out)
            throws IOException, FusionException
        {
            out.setTypeAnnotations(getAnnotationsAsJavaStrings());

            // TODO PERF: Visitor would be more efficient
            out.stepIn(IonType.STRUCT);
            for (Map.Entry<String, Object> entry : getMap(eval))
            {
                String fieldName = entry.getKey();
                Object value     = entry.getValue();

                out.setFieldName(fieldName);
                dispatchIonize(eval, out, value);
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

            // TODO PERF: Visitor would be more efficient
            for (Map.Entry<String, Object> entry : getMap(null))
            {
                String fieldName = entry.getKey();

                Object value = entry.getValue();
                IonValue ion = copyToIonValue(value, factory,
                                              throwOnConversionFailure);
                if (ion == null) return null;
                is.add(fieldName, ion);
            }

            return is;
        }
    }


    private static abstract class NonNullImmutableStruct
        extends MapBasedStruct<NonNullImmutableStruct>
        implements ImmutableStruct<NonNullImmutableStruct>
    {
        private NonNullImmutableStruct(BaseSymbol[] annotations)
        {
            super(annotations);
        }

        @Override
        SyntaxValue makeOriginalSyntax(Evaluator eval, SourceLocation loc)
        {
            return SyntaxStruct.makeOriginal(eval, loc, this);
        }

        @Override
        NonNullImmutableStruct makeSimilar(MultiHashTrie<String, Object> map,
                                           BaseSymbol[] annotations)
        {
            return immutableStruct(map, annotations);
        }

        @Override
        public NonNullImmutableStruct annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return makeSimilar(getMap(eval), annotations);
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
        private final MultiHashTrie<String, Object> myMap;


        private FunctionalStruct(MultiHashTrie<String, Object> map,
                                 BaseSymbol[] annotations)
        {
            super(annotations);
            myMap = map;
        }

        /**
         * Create a struct with one element.
         */
        private FunctionalStruct(String key, Object value, BaseSymbol[] annotations)
        {
            this(MultiHashTrie.<String, Object>empty().with1(key, value), annotations);
        }

        @Override
        public int size()
        {
            return myMap.size();
        }

        @Override
        public MultiHashTrie<String, Object> getMap(Evaluator eval)
        {
            return myMap;
        }
    }

    private static final class LazyInjectingStruct
        extends NonNullImmutableStruct
    {
        /**
         * This map is empty until an element is accessed.
         * Access to this field should be routed through {@link #getMap}
         * to force injection into this map!</b>
         */
        private MultiHashTrie<String, Object> myMap;
        private IonStruct myIonStruct;

        private LazyInjectingStruct(BaseSymbol[] annotations, IonStruct struct)
        {
            super(annotations);
            myIonStruct = struct;
        }

        @Override
        public synchronized int size()
        {
            return (myIonStruct != null ? myIonStruct.size() : myMap.size());
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
        public synchronized MultiHashTrie<String, Object> getMap(Evaluator eval)
        {
            if (myIonStruct != null)
            {
                myMap       = MultiHashTrie.fromEntries(makeInjectingIterator(eval));
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
        extends MapBasedStruct<MutableStruct>
    {
        private MultiHashTrie<String, Object> myMap;

        private MutableStruct(MultiHashTrie<String, Object> map,
                              BaseSymbol[] annotations)
        {
            super(annotations);
            myMap = map;
        }

        MutableStruct()
        {
            super(BaseSymbol.EMPTY_ARRAY);
            myMap = MultiHashTrie.empty();
        }


        @Override
        public int size()
        {
            return myMap.size();
        }

        @Override
        public MultiHashTrie<String, Object> getMap(Evaluator eval)
        {
            return myMap;
        }

        @Override
        MutableStruct makeSimilar(MultiHashTrie<String, Object> map,
                                  BaseSymbol[] annotations)
        {
            return new MutableStruct(map, annotations);
        }

        NonNullImmutableStruct asImmutable()
        {
            return new FunctionalStruct(myMap, myAnnotations);
        }

        @Override
        public MutableStruct annotate(Evaluator eval, BaseSymbol[] annotations)
        {
            return makeSimilar(getMap(eval), annotations);
        }

        @Override
        public Object putM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            // `put` replaces existing values at the key; don't allow duplicates.
            myMap = myMap.with1(key, value);
            return this;
        }

        @Override
        public Object putsM(Evaluator eval, String key, Object value)
            throws FusionException
        {
            myMap = myMap.withMulti(key, value);
            return this;
        }

        @Override
        public Object removeKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            myMap = myMap.withoutKeys(keys);
            return this;
        }

        @Override
        public Object retainKeysM(Evaluator eval, String[] keys)
            throws FusionException
        {
            myMap = MultiHashTrie.fromSelectedKeys(myMap, keys);
            return this;
        }

        @Override
        public Object mergeM(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            myMap = myMap.mergeMulti(other.getMap(eval));
            return this;
        }

        @Override
        public Object merge1M(Evaluator eval, BaseStruct other)
            throws FusionException
        {
            myMap = myMap.merge1(other.getMap(eval));
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

        throw who.argError(eval, expectation, argNum, args);
    }


    /**
     * Checks that an argument is a Fusion list, including {@code null.struct}.
     *
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

        private StructIterator(MultiHashTrie<String,Object> map)
        {
            myEntryIterator = map.iterator();
        }

        @Override
        boolean hasNext(Evaluator eval)
            throws FusionException
        {
            return myEntryIterator.hasNext();
        }

        @Override
        Object next(Evaluator eval)
            throws FusionException
        {
            Map.Entry<String, Object> entry = myEntryIterator.next();

            Object fieldName = makeSymbol(eval, entry.getKey());
            Object value     = entry.getValue();

            // TODO route multi-values through the evaluator
            return new Object[] { fieldName, value };
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
            return new StructIterator(s.getMap(eval));
        }
    }



    private abstract static class BaseStructProc
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

        abstract Object empty(Evaluator eval)
            throws FusionException;

        abstract Object singleEntry(Evaluator eval, String name, Object value)
            throws FusionException;

        abstract Object build(Builder builder);

        @Override
        final Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            if (args.length == 0)
            {
                return empty(eval);
            }
            if (args.length == 2)
            {
                String name = checkRequiredTextArg(eval, this, 0, args);
                return singleEntry(eval, name, args[1]);
            }

            checkArityEven(args);

            Builder builder = builder(eval);

            for (int i = 0; i < args.length; i++)
            {
                String name  = checkRequiredTextArg(eval, this, i, args);
                Object value = args[++i];
                builder.add(name, value);
            }

            return build(builder);
        }
    }



    static final class StructProc
        extends BaseStructProc
    {
        @Override
        Object empty(Evaluator eval)
            throws FusionException
        {
            return emptyStruct(eval);
        }

        @Override
        Object singleEntry(Evaluator eval, String name, Object value)
            throws FusionException
        {
            return immutableStruct(eval, name, value);
        }

        @Override
        Object build(Builder builder)
        {
            return builder.buildImmutable();
        }
    }



    static final class MutableStructProc
        extends BaseStructProc
    {
        @Override
        Object empty(Evaluator eval)
            throws FusionException
        {
            return mutableStruct(eval);
        }

        @Override
        Object singleEntry(Evaluator eval, String name, Object value)
            throws FusionException
        {
            return mutableStruct(eval, name, value);
        }

        @Override
        Object build(Builder builder)
        {
            return builder.buildMutable();
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
        abstract Object empty(Evaluator eval)
            throws FusionException;

        abstract Object build(Builder builder);

        @Override
        final Object doApply(Evaluator eval, Object[] args)
            throws FusionException
        {
            checkArityExact(2, args);
            Object names =  checkNullableListArg(eval, this, 0, args);
            Object values = checkNullableListArg(eval, this, 1, args);

            if (unsafeListSize(eval, names) == 0 || unsafeListSize(eval, values) == 0)
            {
                return empty(eval);
            }

            Iterator<?> fieldIterator = unsafeJavaIterate(eval, names);
            Iterator<?> valueIterator = unsafeJavaIterate(eval, values);

            FusionStruct.Builder builder = FusionStruct.builder(eval);

            while (fieldIterator.hasNext() && valueIterator.hasNext())
            {
                Object nameObj = fieldIterator.next();
                String name = textToJavaString(eval, nameObj);
                if (name == null)
                {
                    String expectation =
                        "sequence of non-null strings or symbols";
                    throw argError(eval, expectation, 0, args);
                }

                Object valueObj = valueIterator.next();

                builder.add(name, valueObj);
            }

            return build(builder);
        }
    }


    static final class StructZipProc
        extends AbstractZipProc
    {
        @Override
        Object empty(Evaluator eval)
            throws FusionException
        {
            return emptyStruct(eval);
        }

        @Override
        Object build(Builder builder)
        {
            return builder.buildImmutable();
        }
    }


    static final class MutableStructZipProc
        extends AbstractZipProc
    {
        @Override
        Object empty(Evaluator eval)
            throws FusionException
        {
            return mutableStruct(eval);
        }

        @Override
        Object build(Builder builder)
        {
            return builder.buildMutable();
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
                keys[i-1] = checkRequiredTextArg(eval, this, i, args);
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
