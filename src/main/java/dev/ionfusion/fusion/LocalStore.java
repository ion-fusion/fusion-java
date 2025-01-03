// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Eval-time storage for a lexical scope.
 */
final class LocalStore
    implements Store
{
    private final Store          myEnclosure;
    private final NamespaceStore myNamespace;
    private final Object[]       myValues;

    /**
     * @param values CALLEE ASSUMES OWNERSHIP OF THIS ARRAY.
     * Caller can (and does) mutate the array elements after constructing the
     * store!
     */
    LocalStore(Store enclosure, Object[] values)
    {
        myEnclosure = enclosure;
        myNamespace = enclosure.namespace();
        myValues    = values;
    }

    @Override
    public NamespaceStore namespace()
    {
        return myNamespace;
    }

    @Override
    public Object lookup(int address)
    {
        return myValues[address];
    }

    @Override
    public Object lookup(int rib, int address)
    {
        if (rib == 0) return myValues[address];
        return myEnclosure.lookup(rib - 1, address);
    }

    @Override
    public void set(int address, Object value)
    {
        myValues[address] = value;
    }

    @Override
    public void set(int rib, int address, Object value)
    {
        if (rib == 0)
        {
            myValues[address] = value;
        }
        else
        {
            myEnclosure.set(rib - 1, address, value);
        }
    }
}
