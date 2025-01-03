// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * NOT FOR APPLICATION USE
 */
public class _Private_Trampoline
{
    private _Private_Trampoline() {}


    public static void setDocumenting(FusionRuntimeBuilder rb,
                                      boolean documenting)
    {
        rb.setDocumenting(documenting);
    }

    public static FusionException newFusionException(String message,
                                                     Throwable cause)
    {
        return new FusionException(message, cause);
    }
}
