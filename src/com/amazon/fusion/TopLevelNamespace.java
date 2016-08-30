// Copyright (c) 2013-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionVoid.voidValue;
import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.fusion.ModuleNamespace.ModuleDefinedBinding;
import com.amazon.fusion.ModuleNamespace.ProvidedBinding;
import java.util.Iterator;
import java.util.Set;


/**
 * Extended prepare-time {@link Namespace} that knows it's a top-level, and not
 * a {@linkplain ModuleNamespace module}.
 * <p>
 * The tricky part here is getting the precedence correct between imports and
 * top-level definitions.  The rule is that the last occurrence wins.  To
 * implement this, we allow the {@link Namespace.NsBinding} to swing between
 * definitions ({@link TopLevelDefinedBinding}) and imports
 * ({@link TopLevelRequiredBinding}).
 */
final class TopLevelNamespace
    extends Namespace
{
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
        public CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            return compileLocalTopReference(eval, env);
        }

        @Override
        public CompiledForm compileTopReference(Evaluator eval,
                                                Environment env,
                                                SyntaxSymbol id)
            throws FusionException
        {
            return new CompiledTopVariableReference(myAddress);
        }

        @Override
        CompiledForm compileDefine(Evaluator eval,
                                   Environment env,
                                   SyntaxSymbol id,
                                   CompiledForm valueForm)
            throws FusionException
        {
            return env.namespace().compileDefine(eval, this, id, valueForm);
        }

        @Override
        CompiledForm compileDefineSyntax(Evaluator eval,
                                         Environment env,
                                         CompiledForm valueForm)
        {
            // TODO FUSION-192 This should bind after evaluation, as 'define'.
            return super.compileDefineSyntax(eval, env, valueForm);
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
        Binding resolve(BaseSymbol name,
                        Iterator<SyntaxWrap> moreWraps,
                        Set<MarkWrap> returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                nextWrap.resolve(name, moreWraps, returnMarks);
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
        public CompiledForm compileTopReference(Evaluator eval,
                                                Environment env,
                                                SyntaxSymbol id)
            throws FusionException
        {
            String message =
                "#%top not implemented for required binding: " + this;
            throw new IllegalStateException(message);
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
        super(registry, ModuleIdentity.forTopLevel(),
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
    CompiledForm compileDefine(Evaluator eval,
                               FreeBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        return new CompiledFreeDefine(id, valueForm);
    }


    @Override
    CompiledForm compileDefine(Evaluator eval,
                               TopLevelDefinedBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        // We can't trust the identifier in the binding, since it may have
        // resolved to an id with a different set of marks.
        return new CompiledFreeDefine(id, valueForm);
    }


    @Override
    CompiledForm compileDefine(Evaluator eval,
                               ModuleDefinedBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        // The lexical context of the bound identifier resolves to some module.
        // We'll use a top-level binding instead.
        return new CompiledFreeDefine(id, valueForm);
    }


    @Override
    CompiledForm compileFreeTopReference(SyntaxSymbol identifier)
    {
        return new CompiledFreeVariableReference(identifier);
    }


    //========================================================================
    // Compiled Forms

    private static final class CompiledFreeDefine
        implements CompiledForm
    {
        private final SyntaxSymbol myId;
        private final CompiledForm myValueForm;

        CompiledFreeDefine(SyntaxSymbol id, CompiledForm valueForm)
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

            if (value instanceof NamedValue)
            {
                ((NamedValue)value).inferName(myId.stringValue());
            }

            return voidValue(eval);
        }

        Object processValue(Evaluator eval, Store store, Object value)
            throws FusionException
        {
            return value;
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
    private static final class CompiledFreeVariableReference
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

            Object result = ns.lookup(address);

            // There's a potential failure here: the binding may have had an
            // address assigned, but not yet a value. That could happen when
            // another thread is in the midst of defining the binding.
            // In such a scenario, the `result` may be null or even corrupt
            // (if we happen to read from the store while it's being updated).
            // However, that scenario is an *application* thread-safety
            // violation since Fusion doesn't promise that concurrent
            // mutation of a top-level namespace is thread-safe.

            return result;
        }
    }
}
