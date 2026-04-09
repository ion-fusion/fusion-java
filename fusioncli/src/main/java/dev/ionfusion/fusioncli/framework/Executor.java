// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusioncli.framework;

public interface Executor
{
    /**
     * Executes a command. In general, any exception will be displayed to the user.
     *
     * @return zero to indicate success, any other number to indicate an error code.
     */
    int execute()
        throws Exception;
}
