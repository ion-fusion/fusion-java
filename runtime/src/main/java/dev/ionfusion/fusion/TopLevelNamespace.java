// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.ResultFailure.makeResultError;
import static dev.ionfusion.fusion.UnboundIdentifierException.makeUnboundError;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.runtime._private.doc.BindingDoc;
import dev.ionfusion.runtime.base.FusionException;
import dev.ionfusion.runtime.base.ModuleIdentity;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;


/**
 * Extended prepare-time {@link Namespace} that knows it's a top-level, and not
 * a {@linkplain ModuleNamespace module}.
 * <p>
 * The tricky part here is getting the precedence correct between imports and
 * top-level definitions.  The rule is that the last occurrence wins.  To
 * implement this, we allow the {@link Namespace.NsBinding} to swing between
 * definitions ({@link TopLevelDefinedBinding}) and imports
 * ({@link TopLevelRequiredBinding}).
 * <p>
 * Like modules, toplevels have a {@link ModuleIdentity}, each a unique
 * "scope" under the {@link #TOP_LEVEL_MODULE_PREFIX}. These paths are not
 * valid module paths! This means that code cannot require the namespace.
 * <p>
 * When a {@code module} declaration is evaluated within a toplevel, its
 * declared name is used to mint a child identifier of the toplevel:
 * {@code (module M ...)} makes a module with identity of the form
 * {@code /fusion/private/toplevel/1234/M}.
 * This ensures that modules declared at toplevel get unique identifiers, and
 * cannot conflict (porticularly within a {@link ModuleRegistry}) with
 * same-named modules in other top-levels.
 * <p>
 * A top-level child module can (currently; see #166) be imported from sibling
 * modules declared in the same toplevel via its simple name. It cannot be
 * reached from outside that specific toplevel, even with a relative path,
 * because the resolved path will be invalid.
 */
final class TopLevelNamespace
    extends Namespace
{
    private static final ModuleIdentity TOP_LEVEL_MODULE_PREFIX =
        ModuleIdentity.forAbsolutePath("/fusion/private/toplevel");


    /**
     * Denotes a binding added into a top-level namespace via {@code define}.
     */
    final class TopLevelDefinedBinding
        extends NsDefinedBinding
    {
        private TopLevelDefinedBinding(SyntaxSymbol identifier, int address)
        {
            super(identifier, address);
        }

        @Override
        public Object lookup(Namespace ns)
        {
            return ns.lookupDefinition(this);
        }


        @Override
        BindingDoc lookupDoc(Namespace current)
        {
            assert current == TopLevelNamespace.this;  // Not always, but just testing
            return TopLevelNamespace.this.document(myAddress);
        }

        @Override
        NsDefinedBinding redefine(SyntaxSymbol identifier,
                                  SyntaxValue formForErrors)
        {
            // Redefinition of an active top-level variable has no effect.
            return this;
        }

        @Override
        RequiredBinding require(SyntaxSymbol localId,
                                ProvidedBinding provided,
                                SyntaxValue formForErrors)
        {
            // Require atop define swing the the import, remembering this
            // defined variable in case it is redefined later.
            return new TopLevelRequiredBinding(localId, this, provided);
        }

        @Override
        public String mutationSyntaxErrorMessage()
        {
            return "mutation of top-level variables is not yet supported";
        }


        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{TopLevelDefinedBinding " + getDebugName() + "}}}";
        }
    }


    /**
     * Exposes the bindings visible at top-level.
     */
    private static final class TopLevelWrap
        extends NamespaceWrap
    {
        // TODO Unit tests passed when this extended EnvironmentWrap, but that
        //   has the wrong variant of resolveTop. What tests are missing?

        TopLevelWrap(TopLevelNamespace ns)
        {
            super(ns);
        }

        @Override
        Binding resolveMaybe(BaseSymbol name,
                             Iterator<SyntaxWrap> moreWraps,
                             Set<MarkWrap> returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                nextWrap.resolveMaybe(name, moreWraps, returnMarks);
            }

            return getEnvironment().substituteFree(name, returnMarks);
        }
    }


    /**
     * Denotes a binding imported into a top-level namespace via
     * {@code require}, or via the namespace's language.
     * <p>
     * Because the top-level mapping for an identifier can swing between
     * definitions and bindings, this class remembers the pre-existing
     * defined-binding (if any) and swings back to it when the name is
     * redefined.  In other words, we re-use the same location for the
     * redefinition.
     */
    private static final class TopLevelRequiredBinding
        extends RequiredBinding
    {
        private final NsDefinedBinding myPriorDefinition;

        private TopLevelRequiredBinding(SyntaxSymbol identifier,
                                        NsDefinedBinding priorDefinition,
                                        ProvidedBinding target)
        {
            super(identifier, target);
            myPriorDefinition = priorDefinition;
        }

        @Override
        NsDefinedBinding redefine(SyntaxSymbol identifier,
                                  SyntaxValue formForErrors)
        {
            return myPriorDefinition;
        }

        @Override
        RequiredBinding require(SyntaxSymbol localId,
                                ProvidedBinding provided,
                                SyntaxValue formForErrors)
        {
            return new TopLevelRequiredBinding(localId, myPriorDefinition,
                                               provided);
        }

        @Override
        NsDefinedBinding definition()
        {
            return myPriorDefinition;
        }

        @Override
        Object visit(Visitor v) throws FusionException
        {
            return v.visit(this);
        }

        @Override
        public String toString()
        {
            return "{{{TopLevelRequiredBinding "
                 + target().myModuleId.absolutePath()
                 + ' ' + getName() + "}}}";
        }
    }


    /**
     * Constructs a top-level namespace. Any bindings will need to be
     * {@code require}d or {@code define}d.
     */
    TopLevelNamespace(ModuleRegistry registry)
    {
        super(registry, ModuleIdentity.forUniqueScope(TOP_LEVEL_MODULE_PREFIX),
              new Function<Namespace, SyntaxWraps>()
              {
                  @Override
                  public SyntaxWraps apply(Namespace _this) {
                      TopLevelNamespace __this = (TopLevelNamespace) _this;
                      return SyntaxWraps.make(new TopLevelWrap(__this));
                  }
              });
    }


    @Override
    Object visit(Visitor v) throws FusionException
    {
        return v.accept(this);
    }


    @Override
    NsDefinedBinding newDefinedBinding(SyntaxSymbol identifier, int address)
    {
        return new TopLevelDefinedBinding(identifier, address);
    }


    @Override
    RequiredBinding newRequiredBinding(SyntaxSymbol    localId,
                                       ProvidedBinding target)
    {
        return new TopLevelRequiredBinding(localId, null, target);
    }

    @Override
    public BaseSymbol getDefinedName(int address)
    {
        throw new UnsupportedOperationException();
    }

    void define(SyntaxSymbol id, Object value)
        throws FusionException
    {
        SyntaxSymbol boundId = predefine(id, id);
        TopLevelDefinedBinding binding = (TopLevelDefinedBinding) boundId.getBinding();
        bind(binding, value);
    }


    //========================================================================
    // Compiled Forms

    static final class CompiledTopDefine
        implements CompiledForm
    {
        private final SyntaxSymbol myId;
        private final CompiledForm myValueForm;

        CompiledTopDefine(SyntaxSymbol id, CompiledForm valueForm)
        {
            myId = id;
            myValueForm = valueForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object value = eval.eval(store, myValueForm);
            eval.checkSingleResult(value, "top-level definition");

            TopLevelNamespace ns = (TopLevelNamespace) store.namespace();
            ns.define(myId, value);

            return voidValue(eval);
        }
    }


    /**
     * Interprets non-single-binding {@code define_values} at top-level.
     * Single-binding forms are interpreted by {@link CompiledTopDefine}.
     * Zero-binding forms are handled here.
     */
    static final class CompiledTopDefineValues
        implements CompiledForm
    {
        private final SyntaxSymbol[] myIds;
        private final CompiledForm myValuesForm;

        CompiledTopDefineValues(SyntaxSymbol[] ids, CompiledForm valuesForm)
        {
            assert ids.length != 1;
            myIds        = ids;
            myValuesForm = valuesForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object values = eval.eval(store, myValuesForm);

            TopLevelNamespace ns = (TopLevelNamespace) store.namespace();

            int expectedCount = myIds.length;  // != 1
            if (values instanceof Object[])
            {
                Object[] vals = (Object[]) values;
                int actualCount = vals.length;
                if (expectedCount != actualCount)
                {
                    String expectation =
                        expectedCount + " results but received " +
                            actualCount;
                    throw makeResultError(eval, "top-level definition", expectation, vals);
                }

                for (int i = 0; i < expectedCount; i++)
                {
                    ns.define(myIds[i], vals[i]);
                }
            }
            else
            {
                String expectation = expectedCount + " results but received 1";
                throw makeResultError(eval, "top-level definition", expectation, values);
            }

            return voidValue(eval);
        }
    }


    /**
     * A reference to a top-level variable in the lexically-enclosing namespace,
     * when the binding is known at compile-time.
     */
    static final class CompiledTopLevelVariableReference
        implements CompiledForm
    {
        final int myAddress;

        CompiledTopLevelVariableReference(int address)
        {
            myAddress = address;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            NamespaceStore ns = store.namespace();
            Object result = ns.lookup(myAddress);
            assert result != null : "No value for namespace address " + myAddress;
            return result;
        }
    }


    /**
     * A reference to a top-level variable in the lexically-enclosing
     * namespace, when the binding isn't known at compile-time.
     * In other words, a forward reference, or perhaps an erroneous unbound variable.
     */
    static final class CompiledFreeVariableReference
        implements CompiledForm
    {
        /**
         * The address of this variable in its namespace store.
         * Since the variable was free at compile-time, we have to determine it lazily.
         * As an invariant, this field is non-negative IFF the binding has been
         * initialized with a value.
         */
        private volatile int myAddress = -1;

        /**
         * The identifier used by this variable reference, to be resolved lazily.
         * We clear it out when the fast path is enabled.
         */
        private volatile SyntaxSymbol myId;

        CompiledFreeVariableReference(SyntaxSymbol id)
        {
            myId = id;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Namespace ns = (Namespace) store.namespace();

            // Minimize reads from a volatile member.
            // TBH I'm not sure if this has any meaningful effect.
            int address = myAddress;
            if (address >= 0)
            {
                // Fast path: the binding is defined and initialized.
                return ns.lookup(address);
            }

            // Slow path: the identifier is not known to be defined and initialized.
            // Use of double-checked locking idiom is safe b/c `myAddress` is volatile.
            // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
            synchronized (this)
            {
                address = myAddress;
                if (address >= 0)    // double-check!
                {
                    // Another thread won the race through this slow path, so go fast.
                    return ns.lookup(address);
                }

                NsDefinedBinding binding = ns.resolveDefinition(myId);
                if (binding != null)
                {
                    address = binding.myAddress;

                    // Address allocation and slot initialization are not atomic!
                    // The slot may still be empty.
                    Object val = ns.lookup(address);
                    if (val != null)
                    {
                        // Initialization is complete, so switch to the fast path.
                        myAddress = address;

                        // An identifier can carry lots of metadata. Let the GC take it.
                        myId = null;

                        return val;
                    }

                    // The slot is allocated but not yet initialized. From this thread's
                    // perspective, the variable is effectively unbound.
                }
            }

            throw makeUnboundError(myId);
            // We will return to the slow path next time around.
        }
    }
}
