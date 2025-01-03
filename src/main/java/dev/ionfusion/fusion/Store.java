// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * The "memory" holding execution-time data.
 */
interface Store
{
    /**
     * Finds the value bound at a given address in this store.
     *
     * @param address the index within this Store.
     *
     * @return not null.
     */
    Object lookup(int address);


    /**
     * Finds the value bound at a given address in some enclosing store.
     * When possible, code should use {@link #lookup(int)} to access this
     * store immediately.
     *
     * @param rib the number of lexical binding layers to go "up"
     * @param address the index within the rib
     *
     * @return not null.
     */
    Object lookup(int rib, int address);


    /**
     * Assigns a value to a variable in this store.
     * This is the implementation of {@code set!}.
     */
    void set(int address, Object value);


    /**
     * Assigns a value to a variable in some enclosing store.
     * This is the implementation of {@code set!}.
     * When possible, code should use {@link #set(int,Object)} to access this
     * store immediately.
     */
    void set(int rib, int address, Object value);


    NamespaceStore namespace();
}
