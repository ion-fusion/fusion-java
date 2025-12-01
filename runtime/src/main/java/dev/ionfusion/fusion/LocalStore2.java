// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Eval-time storage for a lexical scope with exactly two entries.
 */
final class LocalStore2
    implements Store
{
    private final Store          myEnclosure;
    private final NamespaceStore myNamespace;
    private       Object         myValue0;
    private       Object         myValue1;


    LocalStore2(Store enclosure, Object value0, Object value1)
    {
        myEnclosure = enclosure;
        myNamespace = enclosure.namespace();
        myValue0    = value0;
        myValue1    = value1;
    }

    @Override
    public NamespaceStore namespace()
    {
        return myNamespace;
    }

    @Override
    public Object lookup(int address)
    {
        assert address < 2;
        return (address == 0 ? myValue0 : myValue1);
    }

    @Override
    public Object lookup(int rib, int address)
    {
        if (rib == 0)
        {
            assert address < 2;
            return (address == 0 ? myValue0 : myValue1);
        }
        return myEnclosure.lookup(rib - 1, address);
    }

    @Override
    public void set(int address, Object value)
    {
        assert address < 2;
        if (address == 0)
        {
            myValue0 = value;
        }
        else
        {
            myValue1 = value;
        }
    }

    @Override
    public void set(int rib, int address, Object value)
    {
        if (rib == 0)
        {
            assert address < 2;
            if (address == 0)
            {
                myValue0 = value;
            }
            else
            {
                myValue1 = value;
            }
        }
        else
        {
            myEnclosure.set(rib - 1, address, value);
        }
    }
}
