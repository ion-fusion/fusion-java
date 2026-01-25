// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.FusionValue.isAnyNull;


/**
 * Helper class for checking syntactic forms.
 */
class SyntaxChecker
{
    final Evaluator myEvaluator;
    final String myFormName;
    final SyntaxSequence myForm;

    SyntaxChecker(Evaluator eval, String formName, SyntaxSequence form)
    {
        assert form != null;

        myEvaluator = eval;
        myFormName = formName;
        myForm = form;
    }


    /**
     * The form name for messages is derived from the sexp's leading identifer.
     */
    SyntaxChecker(Evaluator eval, SyntaxSexp form)
        throws FusionException
    {
        assert form != null;

        SyntaxSymbol id = form.firstIdentifier(eval);

        myEvaluator = eval;
        myFormName = (id == null ? null : id.getName().stringValue());
        myForm = form;
    }


    /**
     * Gets the syntax form being checked.
     *
     * @return not null.
     */
    SyntaxSequence form()
    {
        return myForm;
    }


    //========================================================================

    SyntaxException failure(String message)
    {
        return new SyntaxException(myFormName, message, myForm);
    }

    SyntaxException failure(String message, SyntaxValue subform)
    {
        SyntaxException e = new SyntaxException(myFormName, message, subform);
        e.addContext(myForm.getLocation());
        return e;
    }

    /**
     * Arity includes the initial syntactic form identifier!
     */
    void arityExact(int count)
        throws FusionException
    {
        if (myForm.size() != count)
        {
            throw failure("expected " + (count-1) + " subforms");
        }
    }

    /**
     * Arity includes the initial syntactic form identifier!
     * @return the actual arity (including the initial syntactic form
     * identifier).
     */
    final int arityAtLeast(int count)
        throws FusionException
    {
        int size = myForm.size();
        if (size < count)
        {
            throw failure("expect at least " + (count-1) + " subforms");
        }
        return size;
    }


    final SyntaxValue requiredForm(String expectation, int argNum)
        throws FusionException
    {
        try
        {
            return myForm.get(myEvaluator, argNum);
        }
        catch (IndexOutOfBoundsException e) {}

        throw failure("expected " + expectation);
    }


    final SyntaxSequence requiredSequence(String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        return checkSyntax(SyntaxSequence.class, expectation,
                           false /* nullable */, form);
    }


    final SyntaxSexp requiredSexp(String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        return checkSyntax(SyntaxSexp.class, expectation,
                           false /* nullable */, form);
    }


    final SyntaxString requiredString(String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        return checkSyntax(SyntaxString.class, expectation,
                           false /* nullable */, form);
    }


    // TODO problematic WRT keywords
    @Deprecated
    final SyntaxSymbol requiredSymbol(String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        return checkSyntax(SyntaxSymbol.class, expectation,
                           false /* nullable */, form);
    }


    final String requiredText(Evaluator eval, String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        Object datum = form.unwrap(eval);
        String text = FusionText.textToJavaString(eval, datum);
        if (text == null)
        {
            throw failure("expected " + expectation, form);
        }
        return text;
    }


    /**
     * Requires an argument to be a non-null, non-empty symbol (and not a keyword).
     */
    final SyntaxSymbol requiredIdentifier(String expectation, int argNum)
        throws FusionException
    {
        SyntaxValue form = requiredForm(expectation, argNum);
        // Special case for better error messaging
        if (form instanceof SyntaxKeyword)
        {
            String message =
                "expected " + expectation + ", not a keyword";
            throw failure(message, form);
        }

        SyntaxSymbol id = checkSyntax(SyntaxSymbol.class, expectation,
                                      false /* nullable */, form);
        if (! id.getName().isNonEmpty())
        {
            String message = "expected non-empty identifier";
            throw failure(message, form);
        }

        return id;
    }

    /**
     * Requires an argument to be a non-null, non-empty symbol (and not a keyword).
     */
    final SyntaxSymbol requiredIdentifier(int argNum)
        throws FusionException
    {
        return requiredIdentifier("identifier", argNum);
    }


    final <T extends SyntaxValue> T checkSyntax(Class<T> klass,
                                                String expectation,
                                                boolean nullable,
                                                SyntaxValue form)
        throws FusionException
    {
        try
        {
            if (nullable || ! isAnyNull(myEvaluator, form.unwrap(myEvaluator)))
            {
                return klass.cast(form);
            }
        }
        catch (ClassCastException e) {}

        throw failure("expected " + expectation, form);
    }

    //========================================================================

    /**
     * Checks that this form has an element at the given index that's an
     * actual sexp.
     *
     * @param description
     * @param index
     * @return a checker wrapping the requested element.
     * @throws FusionException
     */
    SyntaxChecker subformSexp(String description, int index)
        throws FusionException
    {
        SyntaxSexp subsexp = requiredSexp(description, index);
        return new SubformChecker(this, description, subsexp);
    }

    SyntaxChecker subformSeq(String description, int index)
        throws FusionException
    {
        SyntaxSequence subsexp = requiredSequence(description, index);
        return new SubformChecker(this, description, subsexp);
    }

    static class SubformChecker extends SyntaxChecker
    {
        private final SyntaxChecker myBaseForm;

        SubformChecker(SyntaxChecker base, String description,
                       SyntaxSequence form)
        {
            super(base.myEvaluator, description, form);
            myBaseForm = base;
        }

        @Override
        SyntaxException failure(String message)
        {
            SyntaxException e =
                new SyntaxException(myBaseForm.myFormName, message, myForm);
            e.addContext(myBaseForm.myForm.getLocation());
            return e;
        }

        @Override
        SyntaxException failure(String message, SyntaxValue subform)
        {
            SyntaxException e =
                new SyntaxException(myBaseForm.myFormName, message, subform);
            e.addContext(myBaseForm.myForm.getLocation());
            return e;
        }

        @Override
        void arityExact(int count)
            throws FusionException
        {
            if (myForm.size() != count)
            {
                String message =
                    "expected " + count + " subforms in " + myFormName;
                throw failure(message);
            }
        }

        @Override
        SyntaxChecker subformSexp(String description, int index)
            throws FusionException
        {
            SyntaxSexp subsexp = requiredSexp(description, index);
            return new SubformChecker(myBaseForm, description, subsexp);
        }

        @Override
        SyntaxChecker subformSeq(String description, int index)
            throws FusionException
        {
            SyntaxSequence subsexp = requiredSequence(description, index);
            return new SubformChecker(myBaseForm, description, subsexp);
        }
    }
}
