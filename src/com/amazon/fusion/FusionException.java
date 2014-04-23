// Copyright (c) 2012-2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import static com.amazon.fusion.FusionUtils.safeEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents conditions raised within Fusion code, as opposed to failures
 * within the interpreter implementation.
 */
@SuppressWarnings("serial")
public class FusionException
    extends Exception
{
    private List<SourceLocation> myContinuation;

    // Constructors aren't public because I don't want applications to create
    // exceptions directly or subclass them.

    FusionException(String message)
    {
        super(message);
    }

    FusionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    FusionException(Throwable cause)
    {
        super(cause.getMessage(), cause);
    }


    /**
     * Prepends a now location to the continuation of this exception.
     *
     * @param location can be null to indicate an unknown location.
     */
    void addContext(SourceLocation location)
    {
        if (myContinuation == null)
        {
            myContinuation = new ArrayList<>(32);
            myContinuation.add(location);
        }
        else
        {
            SourceLocation prev =
                myContinuation.get(myContinuation.size() - 1);
            if (! safeEquals(prev, location))
            {
                // Collapse equal adjacent locations
                myContinuation.add(location);
            }
        }
    }


    /**
     * Prepends a now location to the continuation of this exception.
     *
     * @param stx can be null to indicate an unknown location.
     */
    void addContext(SyntaxValue stx)
    {
        if (stx != null)
        {
            addContext(stx.getLocation());
        }
    }


    // Before making this public, think about whether it needs Evaluator
    // and should throw FusionException
    void displayContinuation(Appendable out)
        throws IOException
    {
        if (myContinuation != null)
        {
            for (SourceLocation loc : myContinuation)
            {
                if (loc == null)
                {
                    out.append("\n  ...");
                }
                else
                {
                    out.append("\n  ...at ");
                    loc.display(out);
                }
            }
        }
    }


    /**
     * Returns the message string given to the exception constructor.
     * This should be used instead of {@link #getMessage()} since the latter is
     * overridden here to delegate to {@link #displayMessage}.
     */
    final String getBaseMessage()
    {
        return super.getMessage();
    }

    void displayMessage(Evaluator eval, Appendable out)
        throws IOException, FusionException
    {
        String superMessage = getBaseMessage();
        if (superMessage != null)
        {
            out.append(superMessage);
        }
    }

    /**
     * @return the result of calling {@link #displayMessage}.
     */
    @Override
    public final String getMessage()
    {
        StringBuilder out = new StringBuilder();

        try
        {
            displayMessage(null, out);
            displayContinuation(out);
        }
        catch (IOException e) {}
        catch (FusionException e) {}

        return out.toString();
    }
}
