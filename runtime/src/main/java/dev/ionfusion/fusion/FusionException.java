// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import dev.ionfusion.runtime.base.SourceLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents conditions raised within Fusion code, as opposed to failures
 * within the interpreter implementation.
 * <p>
 * Unlike Java's {@code throw} form, Fusion's {@code raise} procedure allows
 * one to throw arbitrary values, not just "exception" types.  Within the
 * FusionJava implementation, all such values are wrapped in
 * {@link FusionException}s.
 * <p>
 * To show Fusion stack traces when these Java exceptions are printed,
 * {@link #getMessage()} consists of two parts: the <em>base message</em>} (a
 * description of the exception) and the <em>context</em> (the Fusion stack
 * trace).
 * Rather than simply printing the Java exception, applications and tools may
 * produce better messages by getting the components individually via
 * {@link #getBaseMessage()} and {@link #getContext()}.
 */
@SuppressWarnings("serial")
public class FusionException
    extends Exception
{
    /**
     * The Fusion stack trace, aggregated by {@code catch} clauses in the
     * interpreter as the Java stack unwinds.
     */
    private List<SourceLocation> myContext;


    public FusionException(String message)
    {
        super(message);
    }

    public FusionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FusionException(Throwable cause)
    {
        super(cause.getMessage(), cause);
    }


    /**
     * Prepends a location to the continuation trace of this exception.
     *
     * @param location can be null to indicate an unknown location.
     */
    public void addContext(SourceLocation location)
    {
        if (myContext == null)
        {
            myContext = new ArrayList<>(32);
            myContext.add(location);
        }
        else
        {
            // Collapse equal adjacent locations
            SourceLocation prev = myContext.get(myContext.size() - 1);
            if (! Objects.equals(prev, location))
            {
                myContext.add(location);
            }
        }
    }


    /**
     * Returns the Fusion stack trace of this exception.
     * The first element in the list is the deepest stack frame, normally the
     * site of the exception.
     * <p>
     * The list may contain null elements indicating notable gaps in the trace.
     * In the default stack display, these appear as {@code ...} lines without
     * locations.
     *
     * @return an immutable list; not null.
     */
    public List<SourceLocation> getContext()
    {
        return (myContext == null ? emptyList() : unmodifiableList(myContext));
    }


    private void displayContinuation(Appendable out)
        throws IOException
    {
        if (myContext != null)
        {
            for (SourceLocation loc : myContext)
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
     * Gets the value that was passed to Fusion's {@code raise} procedure.
     * The result could be any Fusion value, so it must be handled carefully.
     * True Fusion exception values -- that is, the values raised by library
     * features like {@code assert} and {@code raise_argument_error} -- are
     * implemented as subclasses of this type, and this method will return
     * {@code this} object.
     *
     * @return the Fusion value raised by Fusion code.
     */
    public Object getRaisedValue()
    {
        return this;
    }

    /**
     * Returns the base message of this exception.
     * <p>
     * This should be preferred over {@link #getMessage()} since the latter
     * includes the Fusion continuation trace.
     *
     * @return the base message.
     */
    public String getBaseMessage()
    {
        return super.getMessage();
    }

    /**
     * Returns the base message of this exception, followed by the Fusion stack
     * trace.
     *
     * @return not null.
     */
    @Override
    public final String getMessage()
    {
        StringBuilder out = new StringBuilder();

        String base = getBaseMessage();
        if (base == null)
        {
            base = getClass().getSimpleName();
        }
        out.append(base);

        try
        {
            displayContinuation(out);
        }
        catch (IOException e)
        {
            // Swallow these, we can't do anything with it at the moment.
        }

        return out.toString();
    }
}
