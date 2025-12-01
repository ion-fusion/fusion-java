// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.testing;

import static dev.ionfusion.testing.Permute.generatePermutations;
import static dev.ionfusion.testing.Permute.generateSubsetPermutations;
import static dev.ionfusion.testing.Permute.generateSubsets;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PermuteTest
{
    private int factorial(int n)
    {
        return (n <= 2) ? n : n * factorial(n - 1);
    }

    /**
     * Compute the number of possible permutations of a given sequence of values.
     */
    private int permutationCount(Object[] values)
    {
        return factorial(values.length);
    }

    /**
     * Compute the number of possible subsets of a given set of values.
     */
    private int subsetCount(Object[] values)
    {
        return (1 << values.length) - 1;
    }

    /**
     * Generate a hashable key for a sequence of values.
     */
    private <T> String keyForSequence(T[] sequence)
    {
        return Arrays.toString(sequence);
    }

    /**
     * Generate a hashable key for a set of values.
     */
    private <T> String keyForSet(T[] set)
    {
        T[] clone = set.clone();
        Arrays.sort(clone);
        return Arrays.toString(clone);
    }


    private <T> void checkPermutations(Collection<T[]> results, T[] values)
    {
        int expectedPermutations = permutationCount(values);
        assertEquals(expectedPermutations, results.size());

        // Check that each result is valid and unique.
        HashSet<String> unique = new HashSet<>(expectedPermutations);
        for (T[] result : results)
        {
            assertThat(result, arrayContainingInAnyOrder(values));

            // equals() doesn't work for arrays, so use the string
            // representation instead.
            unique.add(keyForSequence(result));
        }
        assertEquals(expectedPermutations, unique.size());
    }

    @Test
    public void testPermutations()
    {
        Integer[] values = new Integer[0];
        ArrayList<Integer[]> perms = new ArrayList<>();

        for (int i = 1; i < 7; i++)
        {
            values = Arrays.copyOf(values, i);
            values[i - 1] = i;

            generatePermutations(perms, values);

            checkPermutations(perms, values);
            perms.clear();
        }
    }


    private <T> void checkSubsets(Collection<T[]> results, T[] values)
    {
        int subsetCount = subsetCount(values);
        assertEquals(subsetCount, results.size(), "number of subsets");

        // Check that each result is valid and unique.
        HashSet<String> unique = new HashSet<>(subsetCount);
        for (T[] result : results)
        {
            assertTrue(result.length != 0);

            // Check that everything in `subset` is also in `values`.
            assertThat(Arrays.asList(values), hasItems(result));

            // equals() doesn't work for arrays, so use the (normalized) string
            // representation instead.
            unique.add(keyForSet(result));
        }
        assertEquals(subsetCount, unique.size(), "number of unique subsets");
    }

    @Test
    public void testSubsets()
    {
        Integer[] values = new Integer[0];
        ArrayList<Integer[]> results = new ArrayList<>();

        for (int i = 1; i < 5; i++)
        {
            values = Arrays.copyOf(values, i);
            values[i - 1] = i;

            generateSubsets(results, values);

            checkSubsets(results, values);
            results.clear();
        }
    }


    /**
     * Add an array to the appropriate partition for its subset of values.
     */
    private <T>
    void partitionBySubset(Map<String, ArrayList<T[]>> partitions, T[] result)
    {
        // TODO Java8: Use HashMap.compute() to streamline this.
        String key = keyForSet(result);
        ArrayList<T[]> partition = partitions.get(key);
        if (partition == null)
        {
            partition = new ArrayList<>();
            partitions.put(key, partition);
        }
        partition.add(result);
    }

    private <T>
    void checkSubsetPermutations(Collection<T[]> results, T[] values)
    {
        int subsetCount = subsetCount(values);

        // Partition the results by subset.
        Map<String, ArrayList<T[]>> partitions = new HashMap<>(subsetCount);
        for (T[] result : results)
        {
            assertTrue(result.length != 0);

            // Check that everything in `subset` is also in `values`.
            assertThat(Arrays.asList(values), hasItems(result));

            partitionBySubset(partitions, result);
        }
        assertEquals(subsetCount, partitions.size(), "number of unique subsets");

        // Check each partition to ensure it includes all permutations.
        for (ArrayList<T[]> partition : partitions.values())
        {
            checkPermutations(partition, partition.get(0));
        }
    }

    @Test
    public void testSubsetPermutations()
    {
        Integer[] values = new Integer[0];
        ArrayList<Integer[]> results = new ArrayList<>();

        for (int i = 1; i < 6; i++)
        {
            values = Arrays.copyOf(values, i);
            values[i - 1] = i;

            generateSubsetPermutations(results, values);

            checkSubsetPermutations(results, values);
            results.clear();
        }
    }
}
