// Copyright (c) 2014 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.amazon.fusion.FusionBool.makeBool;
import static com.amazon.fusion.FusionList.immutableList;
import static com.amazon.fusion.FusionList.nullList;
import static com.amazon.fusion.FusionNumber.checkRequiredIntArg;
import static com.amazon.fusion.FusionNumber.makeInt;
import static com.amazon.fusion.FusionProcedure.isProcedure;
import static com.amazon.fusion.FusionSexp.emptySexp;
import static com.amazon.fusion.FusionSexp.immutableSexp;
import static com.amazon.fusion.FusionSexp.nullSexp;
import static com.amazon.fusion.FusionSexp.pair;
import static com.amazon.fusion.FusionString.checkRequiredStringArg;
import static com.amazon.fusion.FusionString.isString;
import static com.amazon.fusion.FusionString.makeString;
import static com.amazon.fusion.FusionString.unsafeStringToJavaString;

/**
 * Utilities for manipulating Fusion {@code string} values with
 * regular expressions. See fusioncontrib/regexp for full documentation.
 *
 * @author hohle
 */
public final class FusionRegExp {

    private FusionRegExp() { }

    /**
     * Returns `true' if the <tt>value</tt> is a {@link Pattern}.
     *
     * @param eval a Fusion evaluator
     * @param value a Fusion object
     * @return
     *   `true' if <tt>value</tt> is a <tt>Pattern</tt>, `false' otherwise
     */
    static boolean isRegExp(final Evaluator eval,
                            final Object value) {
        return (value instanceof Pattern);
    }

    /**
     * Ensures that an argument is a valid {@link Pattern}. The value at
     * position <tt>argNum</tt> must be non-null and must be an instance of
     * <tt>Pattern</tt>.
     *
     * @param eval An {@link Evaluator}
     * @param who The caller
     * @param argNum The number of args available.
     * @param args The args passed to the proc.
     * @return The {@link Pattern} supplied in <tt>args</tt>.
     * @throws FusionException
     */
    static Pattern checkRequiredPatternArg(final Evaluator eval,
                                           final Procedure who,
                                           final int argNum,
                                           final Object... args)
        throws FusionException {
        final Object arg = args[argNum];

        if (isRegExp(eval, arg)) {
            return (Pattern) arg;
        }

        final String expectation = "non-null regexp";

        throw who.argFailure(expectation, argNum, args);
    }

    //========================================================================
    // Procedures

    static final class IsRegExpProc
        extends Procedure1 {
        @Override
        Object doApply(final Evaluator eval,
                       final Object arg) {
            return makeBool(eval, isRegExp(eval, arg));
        }
    }

    static final class RegExpProc
        extends Procedure1 {
        @Override
        Object doApply(final Evaluator eval,
                       final Object arg)
            throws FusionException {
            try {
                return Pattern.compile(checkRequiredStringArg(eval, this, 0, arg));
            } catch (PatternSyntaxException e) {
                throw argFailure("valid regular expression pattern", 0, arg);
            }
        }
    }

    static final class QuoteProc
        extends Procedure1 {
        @Override
        Object doApply(final Evaluator eval,
                       final Object arg)
            throws FusionException {
            return makeString(eval,
                              Pattern.quote(checkRequiredStringArg(eval, this, 0, arg)));
        }
    }

    static final class ToStringProc
            extends Procedure1 {
        @Override
        Object doApply(final Evaluator eval,
                       final Object arg)
            throws FusionException {
            return makeString(eval,
                              checkRequiredPatternArg(eval, this, 0, arg)
                                  .pattern());
        }
    }

    /**
     * Constructs a matcher for a {@link Pattern}.
     *
     * @param eval An {@link Evaluator}
     * @param who The caller
     * @param args The args passed to the proc.
     * @return A {@link Matcher} based on a {@link Pattern} and {@link String}.
     * @throws FusionException
     */
    static Matcher makeMatcher(final Evaluator eval,
                               final Procedure who,
                               final Object... args)
        throws FusionException {
        who.checkArityAtLeast(2, args);

        final Pattern re = checkRequiredPatternArg(eval, who, 0, args);
        final String str = checkRequiredStringArg(eval, who, 1, args);

        final Matcher matcher = re.matcher(str);

        if (args.length > 2) {
            final int start = checkRequiredIntArg(eval, who, 2, args).intValue();

            final int end = args.length > 3
                ? checkRequiredIntArg(eval, who, 3, args).intValue()
                : str.length();

            final int startingOffset = str.offsetByCodePoints(0, start);
            final int endingOffset = str.offsetByCodePoints(startingOffset, end - start);
            matcher.region(startingOffset,
                           endingOffset);
        }

        return matcher;
    }

    /**
     * Constructs a Fusion compatible match group based on the resuls of a
     * {@link Matcher}.
     *
     * @param eval An {@link Evaluator}
     * @param matcher A {@link Matcher} applied to the string being evaluated.
     * @return An array of match group
     */
    static Object[] makeMatchGroups(final Evaluator eval,
                                    final Matcher matcher) {
        final int groupCount = matcher.groupCount();
        final Object[] groupMatches = new Object[groupCount + 1];

        for (int i = 0; i <= groupCount; i++) {
            groupMatches[i] = makeString(eval,
                                         matcher.group(i));
        }

        return groupMatches;
    }

    /**
     * Constructs an array of pairs containing all match groups found in a
     * {@link Matcher}.
     *
     * @param eval An {@link Evaluator}
     * @param matcher A {@link Matcher} to which was applied to <tt>str</tt>
     * @param str The {@link String} to which <tt>matcher</tt> was applied
     * @return An array of match group positions
     */
    static Object[] makeMatchGroupPositions(final Evaluator eval,
                                            final Matcher matcher,
                                            final String str) {
        final int groupCount = matcher.groupCount();
        final Object[] groupMatches = new Object[groupCount + 1];

        for (int i = 0; i <= groupCount; i++) {
            groupMatches[i] = pair(eval,
                                   makeInt(eval, str.codePointCount(0, matcher.start(i))),
                                   makeInt(eval, str.codePointCount(0, matcher.end(i))));
        }
        return groupMatches;
    }

    static final class MatchProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);

            final Matcher matcher = makeMatcher(eval, this, args);

            if (matcher.find()) {
                final Object[] groupMatches = makeMatchGroups(eval,
                                                              matcher);
                return immutableList(eval, groupMatches);
            }

            return nullList(eval);
        }
    }

    static final class MatchPositionsProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);

            final Matcher matcher = makeMatcher(eval, this, args);

            if (matcher.find()) {
                final String str = checkRequiredStringArg(eval, this, 1, args);
                final Object[] groupMatches = makeMatchGroupPositions(eval,
                                                                      matcher,
                                                                      str);
                return immutableList(eval, groupMatches);
            }

            return nullList(eval);
        }
    }

    static final class MatchGlobalProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);

            final Matcher matcher = makeMatcher(eval, this, args);

            if (matcher.find()) {
                final List<Object> matches = new ArrayList<>();
                do {
                    final Object[] groupMatches = makeMatchGroups(eval,
                                                                  matcher);
                    matches.add(immutableList(eval, groupMatches));
                } while (matcher.find());

                return immutableSexp(eval, matches);
            }

            return nullSexp(eval);
        }
    }

    static final class MatchPositionsGlobalProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);

            final Matcher matcher = makeMatcher(eval, this, args);

            if (matcher.find()) {
                final String str = checkRequiredStringArg(eval, this, 1, args);
                final List<Object> matches = new ArrayList<>();
                do {
                    final Object[] groupMatches = makeMatchGroupPositions(eval,
                                                                          matcher,
                                                                          str);
                    matches.add(immutableList(eval, groupMatches));
                } while (matcher.find());

                return immutableSexp(eval, matches);
            }

            return nullSexp(eval);
        }
    }

    static final class IsMatchProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);
            final Matcher matcher = makeMatcher(eval, this, args);
            return makeBool(eval, matcher.find());
        }
    }

    static final class IsMatchExactProc
        extends Procedure2 {
        @Override
        Object doApply(final Evaluator eval,
                       final Object regex,
                       final Object str)
            throws FusionException {
            final Matcher matcher = makeMatcher(eval, this, regex, str);
            return makeBool(eval, matcher.matches());
        }
    }

    static final class SplitProc
        extends Procedure {
        String regionalString(final Evaluator eval,
                              final String orig,
                              final Object[] args)
            throws FusionException {
            if (args.length == 2) {
                return orig;
            }

            final int start = checkRequiredIntArg(eval, this, 2, args).intValue();

            final int end = args.length > 3
                ? checkRequiredIntArg(eval, this, 3, args).intValue()
                : orig.length();

            return orig.substring(start, end);
        }

        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(2, 4, args);

            final Pattern re = checkRequiredPatternArg(eval, this, 0, args);
            final String str = regionalString(eval,
                                              checkRequiredStringArg(eval, this, 1, args),
                                              args);

            final String[] parts = re.split(str);
            final Object[] strings = new Object[parts.length];

            for (int i = 0; i < parts.length; i++) {
                strings[i] = makeString(eval, parts[i]);
            }

            return immutableSexp(eval, strings);
        }
    }

    static final class ReplaceProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityExact(3, args);

            final Pattern re = checkRequiredPatternArg(eval, this, 0, args);
            final String str = checkRequiredStringArg(eval, this, 1, args);

            if (!isString(eval, args[2]) && !isProcedure(eval, args[2])) {
                final String expectation = "non-null string or procedure";
                throw argFailure(expectation, 2, args);
            }

            final Matcher matcher = makeMatcher(eval, this, re, args[1]);

            if (!matcher.find()) {
                return makeString(eval, str);
            }

            return (isString(eval, args[2]))
                ? applyStringReplacement(eval, matcher, unsafeStringToJavaString(eval, args[2]))
                : applyProcReplacement(eval, matcher, str, (Procedure) args[2]);
        }

        BaseValue applyStringReplacement(final Evaluator eval,
                                         final Matcher matcher,
                                         final String string) {
            return makeString(eval, matcher.replaceFirst(string));
        }

        BaseValue applyProcReplacement(final Evaluator eval,
                                       final Matcher matcher,
                                       final String input,
                                       final Procedure procedure)
            throws FusionException {
            final int
                start = matcher.start(),
                end = matcher.end();

            final String
                prefix = input.substring(0, start),
                suffix = input.substring(end);

            final Object[] groupMatches = makeMatchGroups(eval,
                                                          matcher);

            final Object result = eval.callNonTail(procedure,
                                                   (Object[]) groupMatches);

            final String replacement =
                prefix +
                unsafeStringToJavaString(eval, result) +
                suffix;

            return makeString(eval, replacement);
        }
    }

    static final class ReplaceGlobalProc
        extends Procedure {
        @Override
        Object doApply(final Evaluator eval,
                       final Object[] args)
            throws FusionException {
            checkArityRange(3, 5, args);

            final Pattern re = checkRequiredPatternArg(eval, this, 0, args);
            final String str = checkRequiredStringArg(eval, this, 1, args);

            if (!isString(eval, args[2]) && !isProcedure(eval, args[2])) {
                final String expectation = "non-null string or procedure";
                throw argFailure(expectation, 2, args);
            }

            final Matcher matcher = (args.length == 3)
                ? makeMatcher(eval, this, re, args[1])
                : (args.length == 4)
                    ? makeMatcher(eval, this, re, args[1], args[3])
                    : makeMatcher(eval, this, re, args[1], args[3], args[4]);

            if (!matcher.find()) {
                return makeString(eval, str);
            }

            return (isString(eval, args[2]))
                ? applyStringReplacement(eval, matcher, unsafeStringToJavaString(eval, args[2]))
                : applyProcReplacement(eval, matcher, str, (Procedure) args[2]);
        }

        Object applyStringReplacement(final Evaluator eval,
                                      final Matcher matcher,
                                      final String string) {
            return makeString(eval, matcher.replaceAll(string));
        }

        Object applyProcReplacement(final Evaluator eval,
                                    final Matcher matcher,
                                    final String input,
                                    final Procedure procedure)
            throws FusionException {

            final StringBuffer buffer = new StringBuffer();

            do {
                final int
                    start = matcher.start(),
                    end = matcher.end();

                final String
                    prefix = input.substring(0, start),
                    suffix = input.substring(end);

                final Object[] groupMatches = makeMatchGroups(eval,
                                                              matcher);

                final Object result = eval.callNonTail(procedure,
                                                       (Object[]) groupMatches);

                matcher.appendReplacement(buffer,
                                          unsafeStringToJavaString(eval, result));
            } while (matcher.find());

            matcher.appendTail(buffer);

            return makeString(eval, buffer.toString());
        }
    }

    static final class ReplaceQuoteProc
        extends Procedure1 {
        int countOf(final String str,
                    final char ch) {
            int n = 0, pos = 0;
            while ((pos = str.indexOf(ch, pos)) != -1) {
                n++;
                pos++;
            }
            return n;
        }

        @Override
        Object doApply(final Evaluator eval,
                       final Object arg)
            throws FusionException {
            final String str = checkRequiredStringArg(eval, this, 0, arg);

            // all '\\' and '$' characters will be replaced with escaped
            // versions ("\\\\" and "\\$" respectively).
            final int length = str.length() + countOf(str, '\\') + countOf(str, '$');
            final StringBuffer buffer = new StringBuffer(length);

            for (int pos = 0; pos < str.length(); pos++) {
                int start = pos;

                while (pos < str.length()
                       && str.charAt(pos) != '\\'
                       && str.charAt(pos) != '$') {
                    pos++;
                }

                buffer.append(str, start, pos);

                if (pos != str.length()) {
                    buffer.append('\\');
                    buffer.append(str, pos, pos + 1);
                }
            }

            return makeString(eval, buffer.toString());
        }
    }
}
