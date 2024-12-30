// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ion_fusion.fusion;

import static dev.ion_fusion.fusion.FusionIo.safeWrite;
import java.io.IOException;

/**
 * Indicates a failure to convert a Fusion value into Ion.
 */
@SuppressWarnings("serial")
final class IonizeFailure
    extends ContractException
{
    private final Object myUnIonizableValue;

    IonizeFailure(Object unIonizableValue)
    {
        super(null);

        myUnIonizableValue = unIonizableValue;
    }


    @Override
    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        out.append("Cannot ionize non-Ionizable data: ");
        safeWrite(eval, out, myUnIonizableValue);
    }
}
