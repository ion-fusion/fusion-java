// Copyright (c) 2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionClob.isClob;
import static com.amazon.fusion.FusionLob.isLob;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class ClobTest
    extends LobTestCase
{
    @Override
    Object makeLob(byte[] value)
        throws FusionException
    {
        TopLevel top = topLevel();
        return FusionClob.forBytesNoCopy(top, value);
    }


    @Override
    void checkLobType(Object lob)
        throws FusionException
    {
        TopLevel top = topLevel();
        assertTrue(isLob(top, lob));
        assertTrue(isClob(top, lob));
    }
}
