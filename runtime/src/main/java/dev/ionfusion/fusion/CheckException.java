// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionString.checkNonEmptyStringArg;

/**
 * Only needed so that {@code is_check_error} can detect it.
 */
@SuppressWarnings("serial")
final class CheckException
    extends FusionErrorException
{
    CheckException(String message)
    {
        super(message);
    }


    /**
     * In Fusion this is {@code make_check_error}.
     */
    static final class MakeCheckErrorProc
        extends Procedure1
    {
        @Override
        Object doApply(Evaluator eval, Object message)
            throws FusionException
        {
            String msg = checkNonEmptyStringArg(eval, this, 0, message);
            return new CheckException(msg);
        }
    }
}
