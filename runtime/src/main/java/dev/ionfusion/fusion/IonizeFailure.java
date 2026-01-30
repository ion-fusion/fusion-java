// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Indicates a failure to convert a Fusion value into Ion.
 */
@SuppressWarnings("serial")
final class IonizeFailure
    extends ContractException
{
    private final Object myUnIonizableValue;

    IonizeFailure(String message, Object unIonizableValue)
    {
        super(message);

        myUnIonizableValue = unIonizableValue;
    }
}
