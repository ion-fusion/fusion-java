// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionVoid.voidValue;
import static dev.ionfusion.fusion.NamedValue.inferObjectName;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
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
            value = processValue(eval, store, value);

            TopLevelNamespace ns = (TopLevelNamespace) store.namespace();
            SyntaxSymbol boundId = ns.predefine(myId, myId);
            TopLevelDefinedBinding binding = (TopLevelDefinedBinding) boundId.getBinding();

            ns.set(binding.myAddress, value);

            inferObjectName(value, myId.getName());

            return voidValue(eval);
        }

        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            eval.checkSingleResult(value, "top-level definition");
            return value;
        }
    }


    /**
     * Interprets non-single-binding {@code define_values} at top-level.
     * Single-binding forms are interpreted by {@link CompiledTopDefine}.
     */
    static final class CompiledTopDefineValues
        implements CompiledForm
    {
        private final SyntaxSymbol[] myIds;
        private final CompiledForm myValuesForm;

        CompiledTopDefineValues(SyntaxSymbol[] ids, CompiledForm valuesForm)
        {
            myIds        = ids;
            myValuesForm = valuesForm;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object values = eval.eval(store, myValuesForm);

            TopLevelNamespace ns = (TopLevelNamespace) store.namespace();

            int expectedCount = myIds.length;
            if (expectedCount == 1)
            {
                eval.checkSingleResult(values, "top-level definition");
                defineAndBind(ns, 0, values);
            }
            else if (values instanceof Object[])
            {
                Object[] vals = (Object[]) values;
                int actualCount = vals.length;
                if (expectedCount != actualCount)
                {
                    String expectation =
                        expectedCount + " results but received " +
                            actualCount;
                    throw new ResultFailure("top-level definition",
                                            expectation, -1, vals);
                }

                for (int i = 0; i < expectedCount; i++)
                {
                    Object       value   = vals[i];
                    defineAndBind(ns, i, value);
                }
            }
            else
            {
                String expectation =
                    expectedCount + " results but received 1";
                throw new ResultFailure("top-level definition",
                                        expectation, -1, values);
            }

            return voidValue(eval);
        }

        private void defineAndBind(TopLevelNamespace ns, int i, Object value)
            throws FusionException
        {
            SyntaxSymbol boundId = myIds[i];
            boundId = ns.predefine(boundId, boundId);

            TopLevelDefinedBinding binding =
                (TopLevelDefinedBinding) boundId.getBinding();

            ns.set(binding.myAddress, value);

            inferObjectName(value, boundId.getName());
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
     * In other words, a forward reference.
     * <p>
     * The first time the variable is dereferenced, we have to resolve the
     * binding. We cache the resulting address so we only have to perform the
     * expensive work once.
     * <p>
     * This uses a rare safe instance of the double-checked locking idiom:
     * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
     */
    static final class CompiledFreeVariableReference
        implements CompiledForm
    {
        private SyntaxSymbol myId;
        private volatile int myAddress = -1;

        CompiledFreeVariableReference(SyntaxSymbol id)
        {
            myId = id;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Namespace ns = (Namespace) store.namespace();

            int address = myAddress;
            if (address < 0)
            {
                synchronized (this)
                {
                    if (myAddress < 0)
                    {
                        NsDefinedBinding binding = ns.resolveDefinition(myId);
                        if (binding == null)
                        {
                            throw new UnboundIdentifierException(myId);
                        }

                        myAddress = binding.myAddress;
                        myId = null;
                    }
                }

                address = myAddress;
            }

            // There's a potential failure here: the binding may have had an
            // address assigned, but not yet a value. That could happen when
            // another thread is in the midst of defining the binding.
            // In such a scenario, the `result` may be null or even corrupt
            // (if we happen to read from the store while it's being updated).
            // However, that scenario is an *application* thread-safety
            // violation since Fusion doesn't promise that concurrent
            // mutation of a top-level namespace is thread-safe.

            return ns.lookup(address);
        }
    }
}
