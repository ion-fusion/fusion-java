// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Generates permutations and subsets.
 */
public class Permute
{
    /**
     * Generate all permutations of the given values.
     *
     * @param perms accumulates the results.
     * @param values the set of values to permute; must be non-empty.
     *               <b>This array will be mutated!</b>
     */
    public static <T>
    void generatePermutations(Collection<T[]> perms, T[] values)
    {
        assert values.length != 0;
        generatePermutations(perms, values, values.length);
    }

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Heap%27s_algorithm">Heap's
     * algorithm</a>.
     */
    private static <T>
    void generatePermutations(Collection<T[]> perms, T[] values, int n)
    {
        if (n == 1)
        {
            perms.add(values.clone());
        }
        else
        {
            generatePermutations(perms, values, n - 1);

            for (int i = 0; i < n - 1; i++)
            {
                T tmp = values[n - 1];
                if (n % 2 == 0)
                {
                    values[n - 1] = values[i];
                    values[i] = tmp;
                }
                else
                {
                    values[n - 1] = values[0];
                    values[0] = tmp;
                }

                generatePermutations(perms, values, n - 1);
            }
        }
    }


    /**
     * Generate all non-empty subsets of the given values.
     *
     * @param values the set of values to permute; must be non-empty.
     */
    public static <T>
    ArrayList<T[]> generateSubsets(T[] values)
    {
        return generateSubsets(new ArrayList<T[]>(), values);
    }

    /**
     * Generate all non-empty subsets of the given values.
     *
     * @param subsets accumulates the results.
     * @param values the set of values to permute; must be non-empty.
     */
    public static <T>
    ArrayList<T[]> generateSubsets(ArrayList<T[]> subsets, T[] values)
    {
        assert values.length != 0;
        return generateSubsets(subsets, values, values.length - 1);
    }

    private static <T>
    ArrayList<T[]> generateSubsets(ArrayList<T[]> subsets, T[] values, int n)
    {
        T nextValue = values[n];

        if (n != 0)
        {
            generateSubsets(subsets, values, n - 1);

            // Capture the current size before we add more things.
            int subsetCount = subsets.size();
            for (int i = 0; i < subsetCount; i++)
            {
                T[] p = subsets.get(i);
                int size = p.length;
                p = Arrays.copyOf(p, size + 1);
                p[size] = nextValue;
                subsets.add(p);
            }
        }

        T[] p = Arrays.copyOf(values, 1);
        p[0] = nextValue;
        subsets.add(p);

        return subsets;
    }


    /**
     * Generate all permutations of all subsets of the given values.
     *
     * @param values the set of values to permute; must be non-empty.
     */
    public static <T>
    ArrayList<T[]> generateSubsetPermutations(T[] values)
    {
        return generateSubsetPermutations(new ArrayList<T[]>(), values);
    }

    /**
     * Generate all permutations of all subsets of the given values.
     *
     * @param perms accumulates the results.
     * @param values the set of values to permute; must be non-empty.
     */
    public static <T, C extends Collection<T[]>>
    C generateSubsetPermutations(C perms, T[] values)
    {
        ArrayList<T[]> subsets = new ArrayList<>();
        generateSubsets(subsets, values);

        for (T[] subset : subsets)
        {
            generatePermutations(perms, subset);
        }

        return perms;
    }
}
