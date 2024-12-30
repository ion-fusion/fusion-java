// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Implementation of Fusion's {@code error_exn}.
 */
@SuppressWarnings("serial")
public class FusionErrorException
    extends FusionException
{
    /**
     * @param message
     */
    FusionErrorException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    FusionErrorException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param cause
     */
    FusionErrorException(Throwable cause)
    {
        super(cause);
    }
}
