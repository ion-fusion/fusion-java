// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import dev.ionfusion.fusion.FusionSymbol.BaseSymbol;
import dev.ionfusion.fusion.LocalEnvironment.LocalBinding;
import dev.ionfusion.fusion.ModuleNamespace.ModuleDefinedBinding;
import dev.ionfusion.fusion.ModuleNamespace.ProvidedBinding;
import dev.ionfusion.fusion.Namespace.NsDefinedBinding;
import dev.ionfusion.fusion.Namespace.RequiredBinding;
import dev.ionfusion.fusion.TopLevelNamespace.TopLevelDefinedBinding;

/**
 * Bindings are used during expansion and compilation to identify a specific
 * binding site.
 * They are compiled away and are not used at eval-time.
 * <p>
 * The types of bindings are as follows:
 * <ul>
 *   <li><em>Original</em> bindings represent the base cases for bindings.
 *     <ul>
 *       <li><em>Physical</em> bindings denote variables and map to storage
 *         locations.
 *         <ul>
 *           <li>{@link NsDefinedBinding}s denote namespace-level variables.
 *           </li>
 *           <li>{@link LocalBinding}s denote variables at other levels, like
 *             the arguments to procedures and the local variables declared by
 *             {@code let}, {@code letrec}, <i>etc.</i>
 *           </li>
 *         </ul>
 *       </li>
 *       <li><em>Free</em> bindings do not map to anything, they are essentially
 *         unbound variables.
 *       </li>
 *     </ul>
 *   </li>
 *   <li><em>Indirect</em> bindings refer to an original binding indirectly,
 *     perhaps renaming the variable along the way.
 *     <ul>
 *       <li>{@link ProvidedBinding}s refer to module-level defined
 *         bindings, or to required binding.
 *       </li>
 *       <li>{@link RequiredBinding}s refer to provided bindings.
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * Every binding has a <em>target</em> binding, which is the original binding
 * to which it refers, perhaps through multiple indirections.
 * A binding is its own target if and only if the binding is original.
 */
abstract class Binding
{
    abstract BaseSymbol getName();

    /**
     * @return null if {@link FreeBinding}.
     */
    abstract BindingSite getBindingSite();

    /**
     * Determines whether this is a {@link FreeBinding} with the given name.
     * The default implementation returns false.
     */
    boolean isFree(BaseSymbol name)
    {
        return false;
    }

    /**
     * Gets the original binding to which this binding refers.
     * Returns itself, if and only if this binding is original.
     *
     * @return not null.
     */
    Binding target()
    {
        return this;
    }

    /**
     * Determines whether two bindings refer to the same {@link #target()},
     * despite any renames on import or export.
     *
     * @param other must not be null.
     */
    boolean sameTarget(Binding other)
    {
        return target() == other.target();
    }

    /**
     * Derives a binding that exports this one.
     *
     * @param name the exported name; may be different from {@link #getName()}.
     * @param idLocation the location, in a {@code provide} form, of the
     *   exported identifier. May be null if the name isn't explicitly given
     *   in the form, as in {@code (provide (all_defined_out))}.
     */
    ProvidedBinding provideAs(BaseSymbol name, SourceLocation idLocation)
    {
        throw new IllegalStateException("This kind of binding can't be exported");
    }

    /**
     * Don't call directly! Second half of double-dispatch from
     * {@link Namespace#lookup(Binding)}.
     *
     * @return null if there's no value associated with the binding.
     */
    abstract Object lookup(Namespace ns);

    /**
     * Checks whether this binding can be mutated.
     *
     * @return null if the binding can be mutated, else a syntax error message.
     */
    String mutationSyntaxErrorMessage()
    {
        return null;
    }


    abstract Object visit(Visitor v) throws FusionException;

    static abstract class Visitor
    {
        Object visit(Binding b) throws FusionException
        {
            throw new IllegalStateException("No visitor for " + getClass());
        }

        Object visit(FreeBinding b) throws FusionException
        {
            return visit((Binding) b);
        }

        Object visit(LocalBinding b) throws FusionException
        {
            return visit((Binding) b);
        }

        Object visit(NsDefinedBinding b) throws FusionException
        {
            return visit((Binding) b);
        }

        Object visit(TopLevelDefinedBinding b) throws FusionException
        {
            return visit((NsDefinedBinding) b);
        }

        Object visit(ModuleDefinedBinding b) throws FusionException
        {
            return visit((NsDefinedBinding) b);
        }

        Object visit(ProvidedBinding b) throws FusionException
        {
            return visit((Binding) b);
        }

        Object visit(RequiredBinding b) throws FusionException
        {
            return visit((Binding) b);
        }
    }
}
