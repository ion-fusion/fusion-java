// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;


/**
 * Indicates inappropriate run-time use of a procedure or syntactic form;
 * that is, a failure to satisfy a feature's contract.
 */
@SuppressWarnings("serial")
public class ContractException
    extends FusionErrorException
{
    /**
     * @param message
     */
    ContractException(String message)
    {
        super(message);
    }

    ContractException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param location may be null.
     */
    ContractException(String message, SourceLocation location)
    {
        super(message);
        addContext(location);
    }

    /**
     * @param location may be null.
     */
    ContractException(String message, SourceLocation location,
                      Throwable cause)
    {
        super(message, cause);
        addContext(location);
    }

    /**
     * Returns the formatted message as provided by the application.
     *
     * @return may be null if no message values were provided.
     */
    public String getUserMessage()
    {
        return getBaseMessage();
    }
}
