// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Gates access to system resources.
 *
 * This implementation is extremely quick-and-dirty.
 *
 * See https://docs.racket-lang.org/reference/securityguards.html
 */
final class SecurityGuard
{
    static final SecurityGuard OPEN   = new SecurityGuard(true);
    static final SecurityGuard CLOSED = new SecurityGuard(false);


    private final boolean myFileSystemEnabled;

    private SecurityGuard(boolean allowFileSystemAccess)
    {
        myFileSystemEnabled = allowFileSystemAccess;
    }


    boolean isFileSystemEnabled()
    {
        return myFileSystemEnabled;
    }
}
