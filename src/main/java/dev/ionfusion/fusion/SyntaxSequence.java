// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;



abstract class SyntaxSequence
    extends SyntaxContainer
{
    SyntaxSequence(SourceLocation loc, Object[] properties, SyntaxWraps wraps)
    {
        super(loc, properties, wraps);
    }

    SyntaxSequence(SourceLocation loc)
    {
        super(loc);
    }


    abstract int size()
        throws FusionException;


    abstract SyntaxValue get(Evaluator eval, int index)
        throws FusionException;


    /**
     * Gets all the children of this sequence as a new array.
     * Useful for making changes and then building a replacement sequence.
     *
     * @return a new array, or null when called on null sequences.
     */
    abstract SyntaxValue[] extract(Evaluator eval)
        throws FusionException;

    /**
     * Creates a new syntax sequence, using our location and properties but
     * the given children.
     */
    abstract SyntaxSequence copyReplacingChildren(Evaluator eval,
                                                  SyntaxValue... children)
        throws FusionException;


    /** Creates a new sequence with this + that. */
    abstract SyntaxSequence makeAppended(Evaluator eval, SyntaxSequence that)
        throws FusionException;


    /**
     * @return null if this sequence isn't proper and from goes beyond the end.
     */
    abstract SyntaxSequence makeSubseq(Evaluator eval, int from)
        throws FusionException;
}
