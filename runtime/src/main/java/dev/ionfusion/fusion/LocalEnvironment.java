// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.BindingSite.makeLocalBindingSite;
import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.SyntaxSymbol.ensureUniqueIdentifiers;
import static dev.ionfusion.fusion._private.FusionUtils.EMPTY_STRING_ARRAY;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import java.util.Set;

final class LocalEnvironment
    implements Environment
{
    /**
     * Denotes a binding defined by a local environment, for example a
     * {@code lambda} or {@code letrec} expression.
     * <p>
     * This compile-time representation of a local binding holds the data
     * needed to compute the two-dimensional De Bruijn indexes used by
     * compiled references to the binding. Here, the <em>depth</em> counts
     * enclosing local environments inward from top-level, and the
     * <em>address</em> indexes into the list of bindings introduced by that
     * environment.
     * </p>
     *
     * @see CompiledImmediateVariableReference
     * @see CompiledLocalVariableReference
     */
    static final class LocalBinding
        extends Binding
    {
        static final LocalBinding[] EMPTY_ARRAY = new LocalBinding[0];

        private final SyntaxSymbol       myIdentifier;
                final int                myDepth;
                final int                myAddress;
        private final BindingSite        mySite;

        private LocalBinding(SyntaxSymbol identifier, int depth, int address)
        {
            assert depth > 0;
            myIdentifier = identifier;
            myDepth      = depth;
            myAddress    = address;
            mySite       = makeLocalBindingSite(identifier.getLocation());
        }

        @Override
        BaseSymbol getName()
        {
            return myIdentifier.getName();
        }

        @Override
        BindingSite getBindingSite()
        {
            return mySite;
        }

        @Override
        boolean sameTarget(Binding other)
        {
            // Don't need to call other.target() since local bindings are
            // original and unique.
            return this == other;
        }


        @Override
        Object lookup(Namespace ns)
        {
            return null;  // We're local, not a namespace binding.
        }


        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(this);
        }

        @Override
        public String toString()
        {
            return "{{{LocalBinding " + myIdentifier + "}}}";
        }


        /** Extract the names from an array of bindings. */
        static String[] toNames(LocalBinding[] bindings)
        {
            if (bindings == null || bindings.length == 0)
            {
                return EMPTY_STRING_ARRAY;
            }
            else
            {
                String[] names = new String[bindings.length];
                for (int i = 0; i < bindings.length; i++)
                {
                    names[i] = bindings[i].myIdentifier.stringValue();
                }
                return names;
            }
        }
    }


    /** Not null */
    private final Environment    myEnclosure;
    private final Namespace      myNamespace;
    private final int            myDepth;
    private final LocalBinding[] myBindings;


    /**
     * Expand-time environment construction.
     *
     * @throws FusionException if there's a duplicate identifier.
     */
    LocalEnvironment(Environment enclosure,
                     SyntaxSymbol[] identifiers,
                     SyntaxValue formForErrors)
        throws FusionException
    {
        myEnclosure = enclosure;
        myNamespace = enclosure.namespace();
        myDepth = 1 + enclosure.getDepth();

        // 2019-04 Stats from the standard library and test suite:
        //   85% of local envs have 1 entry, 12% have 2.
        //   99% have 3 or fewer entries and none have more than 5.
        int count = identifiers.length;
        if (count > 1)
        {
            ensureUniqueIdentifiers(identifiers, formForErrors);
        }

        myBindings = new LocalBinding[count];
        for (int i = 0; i < count; i++)
        {
            SyntaxSymbol identifier = identifiers[i];
            myBindings[i] = new LocalBinding(identifier, myDepth, i);
        }
    }


    /**
     * Compile-time environment construction; makes a dummy environment to
     * keep track of the current depth.
     */
    LocalEnvironment(Environment enclosure)
    {
        myEnclosure = enclosure;
        myNamespace = enclosure.namespace();
        myDepth = 1 + enclosure.getDepth();

        myBindings = null;
    }


    @Override
    public Namespace namespace()
    {
        return myNamespace;
    }

    @Override
    public int getDepth()
    {
        return myDepth;
    }


    @Override
    public Binding substitute(Binding binding, Set<MarkWrap> marks)
    {
        BoundIdentifier boundId = new BoundIdentifier(binding, marks);

        for (LocalBinding b : myBindings)
        {
            if (b.myIdentifier.resolveBoundIdentifier().equals(boundId))
            {
                return b;
            }
        }
        return binding;
    }

    @Override
    public Binding substituteFree(BaseSymbol name, Set<MarkWrap> marks)
    {
        BoundIdentifier boundId =
            new BoundIdentifier(new FreeBinding(name), marks);

        for (LocalBinding b : myBindings)
        {
            if (b.myIdentifier.resolveBoundIdentifier().equals(boundId))
            {
                return b;
            }
        }
        return null;
    }


    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder("{{{LocalEnv (");
        if (myBindings != null)
        {
            boolean first = true;
            for (LocalBinding b : myBindings)
            {
                if (!first) buf.append(' ');
                buf.append(b.myIdentifier);
                first = false;
            }
        }
        buf.append(")}}}");
        return buf.toString();
    }


    //========================================================================


    /**
     * A reference to a variable in the immediately-enclosing environment.
     * This is an optimized form of {@link CompiledLocalVariableReference}
     * where {@code rib == 0}.
     */
    static final class CompiledImmediateVariableReference
        implements CompiledForm
    {
        private final int myAddress;

        CompiledImmediateVariableReference(int address)
        {
            myAddress = address;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object result = store.lookup(myAddress);
            assert result != null
                : "No value for immediate var @" + myAddress;
            return result;
        }
    }


    static final class CompiledLocalVariableReference
        implements CompiledForm
    {
        private final int myRib;
        private final int myAddress;

        CompiledLocalVariableReference(int rib, int address)
        {
            assert rib > 0;
            myRib     = rib;
            myAddress = address;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object result = store.lookup(myRib, myAddress);
            assert result != null
                : "No value for rib " + myRib + " address " + myAddress;
            return result;
        }
    }


    static final class CompiledImmediateVariableSet
        implements CompiledForm
    {
        private final int          myAddress;
        private final CompiledForm myValueForm;

        CompiledImmediateVariableSet(int address, CompiledForm valueForm)
        {
            myAddress   = address;
            myValueForm = valueForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm);
            store.set(myAddress, value);
            return voidValue(eval);
        }
    }


    static final class CompiledLocalVariableSet
        implements CompiledForm
    {
        private final int          myRib;
        private final int          myAddress;
        private final CompiledForm myValueForm;

        CompiledLocalVariableSet(int rib, int address, CompiledForm valueForm)
        {
            assert rib > 0;
            myRib       = rib;
            myAddress   = address;
            myValueForm = valueForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm);
            store.set(myRib, myAddress, value);
            return voidValue(eval);
        }
    }
}
