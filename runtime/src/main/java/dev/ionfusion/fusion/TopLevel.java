// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import com.amazon.ion.IonReader;
import java.io.File;

/**
 * A top-level namespace for evaluating Fusion scripts or expressions.
 * Each instance has a separate namespace, bootstrapped with bindings from a
 * specific Fusion module or language.
 * <p>
 * <b>WARNING:</b> This interface must not be implemented or extended by
 * code outside of this library.
 * <p>
 * {@link TopLevel}s are <em>not</em> inherently thread-safe! It is not safe
 * to use the same instance from multiple threads, unless all such use is
 * functional (that is, no mutations are made) with respect to any common data.
 * Put another way, this library does not perform any synchronization on
 * namespaces or user-visible data structures, so your application must take
 * steps to handle such synchronization when and if it is needed.
 * <p>
 * Using thread-local {@link TopLevel}s is reasonable, if you can properly
 * isolate state changes between requests on the same thread.
 * <p>
 * To create a {@link TopLevel}, use a {@link FusionRuntime}.
 *
 * <h2>Exceptions</h2>
 *
 * Most of the methods on this class are capable of throwing two subclasses of
 * {@link FusionException} that are noteworthy in that applications may
 * require special handling for them:
 *
 * <ul>
 *   <li>
 *     {@link ExitException} is thrown if Fusion's {@code exit} procedure
 *     is invoked.
 *   </li>
 *   <li>
 *     {@link FusionInterruptedException} is thrown if the evaluation is
 *     cancelled by calling {@link Thread#interrupt()} on the current thread.
 *     When this is thrown the thread's interrupt status will have been set.
 *   </li>
 * </ul>
 *
 */
public interface TopLevel
{
    /**
     * Evaluates top-level forms within this namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     * <p>
     * If the reader is already positioned on a value (that is, if
     * {@code source.getType() != null}), then that value is the first form
     * evaluated. Otherwise, the reader is advanced to the next value
     * automatically.
     *
     * @param source Fusion source code, in Ion data format. Must not be null.
     *               The caller is responsible for closing this reader.
     * @param name identifies the source for error reporting. May be null.
     *
     * @return the resulting Fusion value; typically the value of the last
     * expression in the source. May be null (if no value results) or an
     * {@code Object[]} (if there are multiple values).
     *
     * @throws FusionException if an error occurs during evaluation
     */
    Object eval(IonReader source, SourceName name)
        throws FusionException;


    /**
     * Evaluates top-level forms within this namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     * <p>
     * {@link #eval(IonReader,SourceName)} should be preferred to this method,
     * since it can provide better error reporting.
     *
     * @param source Fusion source code, in Ion data format. Must not be null.
     *               The caller is responsible for closing this reader.
     *
     * @return the resulting Fusion value; typically the value of the last
     * expression in the source. May be null (if no value results) or an
     * {@code Object[]} (if there are multiple values).
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #eval(IonReader,SourceName)
     */
    Object eval(IonReader source)
        throws FusionException;


    /**
     * Evaluates top-level forms within this namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     *
     * @param source Fusion source code, in Ion data format. Must not be null.
     * @param name identifies the source for error reporting. May be null.
     *
     * @return the resulting Fusion value; typically the value of the last
     * expression in the source. May be null (if no value results) or an
     * {@code Object[]} (if there are multiple values).
     *
     * @throws FusionException if an error occurs during evaluation
     */
    Object eval(String source, SourceName name)
        throws FusionException;


    /**
     * Evaluates top-level forms within this namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     * <p>
     * {@link #eval(String,SourceName)} should be preferred to this method,
     * since it can provide better error reporting.
     *
     * @param source Fusion source code, in Ion data format. Must not be null.
     *
     * @return the resulting Fusion value; typically the value of the last
     * expression in the source. May be null (if no value results) or an
     * {@code Object[]} (if there are multiple values).
     *
     * @throws FusionException if an error occurs during evaluation
     *
     * @see #eval(String,SourceName)
     */
    Object eval(String source)
        throws FusionException;



    /**
     * Evaluates top-level forms within this namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     *
     * @param source a file containing Fusion source code, in Ion data format.
     * Must not be null.
     *
     * @return the resulting Fusion value; typically the value of the last
     * expression in the source. May be null (if no value results) or an
     * {@code Object[]} (if there are multiple values).
     *
     * @throws FusionException if an error occurs during evaluation
     */
    Object load(File source)
        throws FusionException;


    //========================================================================
    // Common operations


    /**
     * Imports all exported bindings from a module into this namespace.
     * <p>
     * If any imported names have top-level definitions, those definitions will
     * be shadowed by the imported binding.
     *
     * @param modulePath locates the required module. It may be either an
     * absolute or relative path.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    void requireModule(String modulePath)
        throws FusionException;


    /**
     * Binds an identifier with a value in this namespace,
     * <a href="{@docRoot}/overview-summary.html#inject">injecting</a> Java
     * values.
     * <p>
     * If the name has already been defined in this namespace then the existing
     * variable is mutated to have the given value.
     * If the name has been previously imported into this namespace, then a
     * top-level definition is created or modified and will shadow the imported
     * binding in future expressions.
     *
     * @param name the top-level binding to introduce or mutate.
     * @param value the value to bind, must be
     * <a href="{@docRoot}/overview-summary.html#inject">injectable</a>.
     * It is unspecified whether the value will be copied in whole or in part.
     *
     * @throws FusionException if an error occurs during evaluation
     * @throws IllegalArgumentException if the {@code value} cannot be injected.
     */
    void define(String name, Object value)
        throws FusionException;


    /**
     * Finds a top-level binding for a given name and returns any defined
     * value.
     *
     * @param name the binding to lookup and dereference.
     *
     * @return the bound value, or null if there's no top-level definition or
     * imported binding for the name.
     *
     * @throws FusionException if an error occurs during evaluation
     */
    Object lookup(String name)
        throws FusionException;


    /**
     * Calls a Fusion procedure by name,
     * <a href="{@docRoot}/overview-summary.html#inject">injecting</a> Java
     * values.
     *
     * @param procedureName must name a visible procedure, either defined in
     * this namespace or imported from a module.
     *
     * @param arguments the procedure arguments. It is unspecified whether the
     * arguments will be copied in whole or in part. Values may be mutated by
     * the procedure.
     *
     * @return the resulting Fusion value.
     * May be null (if no value results) or an {@code Object[]} (if there are
     * multiple values).
     * Note that "no value" is not the same as "returns void".
     *
     * @throws FusionException if an error occurs during evaluation
     * @throws IllegalArgumentException if {@code procedureName} is not bound to
     * a procedure, or if one of the {@code arguments} cannot be injected.
     */
    Object call(String procedureName, Object... arguments)
        throws FusionException;


    /**
     * Calls a Fusion procedure by value,
     * <a href="{@docRoot}/overview-summary.html#inject">injecting</a> Java
     * values.
     *
     * @param procedure must be a Fusion procedure.
     * @param arguments the procedure arguments. It is unspecified whether the
     * arguments will be copied in whole or in part. Values may be mutated by
     * the procedure.
     *
     * @return the resulting Fusion value.
     * May be null (if no value results) or an {@code Object[]} (if there are
     * multiple values).
     * Note that "no value" is not the same as "returns void".
     *
     * @throws FusionException if an error occurs during evaluation
     * @throws IllegalArgumentException if {@code procedure} is not a procedure,
     * or if one of the {@code arguments} cannot be injected.
     */
    Object call(Object procedure, Object... arguments)
        throws FusionException;


    //========================================================================
    // Dynamic module definitions


    /**
     * Loads a module declaration from source code. This compiles the module,
     * but does not instantiate it (evaluate the module's body); that happens
     * when the module is first {@code require}d.
     * <p>
     * If the reader is positioned on a value, it will be read; otherwise the
     * {@linkplain IonReader#next() next} value will be read.
     * <p>
     * If the reader doesn't provide exactly one top-level value, an exception
     * is thrown.
     *
     * @param absoluteModulePath identifies the module to be loaded.
     * Must be an absolute module path, starting with {@code '/'}.
     * @param source Fusion source code, in Ion data format. Must not be null.
     * @param name identifies the source for error reporting. May be null.
     *
     * @throws FusionException if the reader doesn't provide exactly one
     * top-level value, if a module with the given identity has already been
     * loaded, or if there's any other problem reading or compiling the module.
     */
    void loadModule(String absoluteModulePath,
                    IonReader source,
                    SourceName name)
        throws FusionException;
}
