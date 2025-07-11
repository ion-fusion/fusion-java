// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonCatalog;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;

/**
 * Primary entry point for embedding Fusion within a Java program.
 * The runtime contains resources common to many evaluations, in particular the
 * default module registry tracking loaded modules.
 * Instances are immutable and thread-safe.
 * <p>
 * <b>WARNING:</b> This interface must not be implemented or extended by
 * code outside of this library.
 * </p>
 * <p>
 * The runtime should be considered a very heavyweight component, and it should
 * not be used in a transient or localized fashion. Most applications should
 * construct exactly one runtime.
 * </p>
 * <p>
 * The runtime maintains a {@link TopLevel} namespace within which expressions
 * can be evaluated. The namespace bindings and state are maintained between
 * calls.  Applications that need isolated evaluation should create additional
 * top levels by calling one of the {@link #makeTopLevel} methods. More secure
 * evaluation contexts are provided via {@link #makeSandboxBuilder()}.
 * </p>
 * <p>
 * To create a {@link FusionRuntime}, use a {@link FusionRuntimeBuilder}.
 * </p>
 */
public interface FusionRuntime
{
    /**
     * Gets the identifier for this runtime's default language, which supplies
     * the initial bindings for its {@link TopLevel} namespaces.
     * Unless configured otherwise by the {@link FusionRuntimeBuilder}, this
     * value is {@code "/fusion"}.
     *
     * @return an absolute module path.
     *
     * @see FusionRuntimeBuilder#getDefaultLanguage()
     */
    String getDefaultLanguage();


    /**
     * Gets the Ion symbol table catalog used when reading and writing Ion data.
     *
     * @return not null.
     */
    IonCatalog getDefaultIonCatalog();


    /**
     * Returns a singular {@link TopLevel} for this runtime; each call returns
     * the same instance.  All uses of the instance will share bindings and
     * state, so applications that require isolated state must create
     * additional {@link TopLevel}s via {@link #makeTopLevel()}.
     * <p>
     * The default {@link TopLevel} is bootstrapped with bindings from this
     * runtime's default language.
     *
     * @return not null.
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #getDefaultLanguage()
     */
    TopLevel getDefaultTopLevel()
        throws FusionException;


    /**
     * Returns a fresh {@link TopLevel} instance, bootstrapped with this
     * runtime's default language.
     *
     * @return not null.
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #getDefaultLanguage()
     */
    TopLevel makeTopLevel()
        throws FusionException;


    /**
     * Returns a fresh {@link TopLevel} instance, populated with bindings from
     * a given module.  The default language is <em>not</em> imported.
     *
     * @param initialModulePath must be an absolute module path, starting with
     * {@code '/'}.
     *
     * @return not null.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    TopLevel makeTopLevel(String initialModulePath)
        throws FusionException;


    /**
     * Returns a fresh builder for creating evaluation sandboxes.
     *
     * @return a new builder instance.
     */
    SandboxBuilder makeSandboxBuilder();


    /**
     * Returns a builder for constructing modules via Java.
     *
     * @param absoluteModulePath must be an absolute module path, starting
     * with {@code '/'}.
     *
     * @return a new module builder.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    ModuleBuilder makeModuleBuilder(String absoluteModulePath)
        throws FusionException;


    //========================================================================


    /**
     * Creates a fresh {@code IonValue} DOM from a Fusion value, using the
     * default ionization strategy.
     *
     * @param fusionValue the value to transform; must not be null.
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container.
     *
     * @throws FusionException if the value is not handled by the default
     * ionization strategy, or if something else goes wrong during ionization.
     */
    IonValue ionize(Object fusionValue, ValueFactory factory)
        throws FusionException;


    /**
     * Creates a fresh {@code IonValue} DOM from a Fusion value, using the
     * default ionization strategy, returning null if ionization fails.
     *
     * @param fusionValue the value to transform.
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container, or null if the value
     * (or any part of the value) is not handled by the default ionization
     * strategy.
     *
     * @throws FusionException if something goes wrong during ionization.
     */
    IonValue ionizeMaybe(Object fusionValue, ValueFactory factory)
        throws FusionException;
}
