// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.ion.IonReader;
import com.amazon.ion.IonValue;
import com.amazon.ion.ValueFactory;
import java.io.File;

/**
 * Primary entry point for embedding Fusion within a Java program.
 * <p>
 * The runtime maintains a top-level environment within which expressions can
 * be evaluated. The environment is maintained between calls.
 */
public interface FusionRuntime
{

    /**
     * Evaluates top-level forms within this {@link FusionRuntime}'s namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     *
     * @param source must not be null.
     * @param name identifies the source for error reporting. May be null.
     * @return not null, but perhaps {@link FusionValue#UNDEF}.
     *
     * @throws ExitException if the Fusion {@code exit} procedure is invoked.
     */
    public Object eval(String source, SourceName name)
        throws ExitException, FusionException;

    /**
     * Evaluates top-level forms within this {@link FusionRuntime}'s namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     * <p>
     * {@link #eval(String,SourceName)} should be preferred to this method,
     * since it can provide better error reporting.
     *
     * @param source must not be null.
     * @return not null, but perhaps {@link FusionValue#UNDEF}.
     *
     * @throws ExitException if the Fusion {@code exit} procedure is invoked.
     *
     * @see #eval(String,SourceName)
     */
    public Object eval(String source)
        throws ExitException, FusionException;


    /**
     * Evaluates top-level forms within this {@link FusionRuntime}'s namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     *
     * @param source must not be null.
     * @param name identifies the source for error reporting. May be null.
     * @return not null, but perhaps {@link FusionValue#UNDEF}.
     *
     * @throws ExitException if the Fusion {@code exit} procedure is invoked.
     */
    Object eval(IonReader source, SourceName name)
        throws ExitException, FusionException;

    /**
     * Evaluates top-level forms within this {@link FusionRuntime}'s namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     * <p>
     * {@link #eval(IonReader,SourceName)} should be preferred to this method,
     * since it can provide better error reporting.
     *
     * @param source must not be null.
     * @return not null, but perhaps {@link FusionValue#UNDEF}.
     *
     * @throws ExitException if the Fusion {@code exit} procedure is invoked.
     *
     * @see #eval(IonReader,SourceName)
     */
    public Object eval(IonReader source)
        throws ExitException, FusionException;


    /**
     * Evaluates top-level forms within this {@link FusionRuntime}'s namespace.
     * Top-level {@code define} forms will alter the environment and will be
     * visible to later calls.
     *
     * @param source must not be null.
     * @return not null, but perhaps {@link FusionValue#UNDEF}.
     *
     * @throws ExitException if the Fusion {@code exit} procedure is invoked.
     */
    public Object load(File source)
        throws ExitException, FusionException;


    /**
     * Binds an identifier with a value in this {@link FusionRuntime}'s
     * namespace.
     *
     * @param value must not be null.
     */
    public void bind(String name, Object value);


    // TODO require(File)

    // TODO main(String...)


    //========================================================================


    /**
     * Creates a fresh {@code IonValue} DOM from a Fusion value, using the
     * default ionization strategy.
     *
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container.
     *
     * @throws FusionException if the value is not handled by the default
     * ionization strategy, or if something else goes wrong during ionization.
     */
    public IonValue ionize(Object fusionValue, ValueFactory factory)
        throws FusionException;


    /**
     * Creates a fresh {@code IonValue} DOM from a Fusion value, using the
     * default ionization strategy.
     *
     * @param factory must not be null.
     *
     * @return a fresh instance, without a container, or null if the value is
     * not handled by the default ionization strategy.
     *
     * @throws FusionException if something goes wrong during ionization.
     */
    public IonValue ionizeMaybe(Object fusionValue, ValueFactory factory)
        throws FusionException;
}
