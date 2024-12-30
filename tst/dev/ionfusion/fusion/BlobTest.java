// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionBlob.isBlob;
import static dev.ionfusion.fusion.FusionLob.isLob;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class BlobTest
    extends LobTestCase
{
    @Override
    Object makeLob(byte[] value)
        throws FusionException
    {
        TopLevel top = topLevel();
        return FusionBlob.forBytesNoCopy(top, value);
    }


    @Override
    void checkLobType(Object lob)
        throws FusionException
    {
        TopLevel top = topLevel();
        assertTrue(isLob(top, lob));
        assertTrue(isBlob(top, lob));
    }
}
