// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private.doc.tool;

import static dev.ionfusion.fusion._private.doc.tool.ExportedBinding.COMPARE_BY_MODULE;
import static java.util.stream.Collectors.toList;

import dev.ionfusion.runtime.base.ModuleIdentity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;


public final class DocIndex
{
    private final Predicate<ModuleIdentity>               myModuleSelector;
    private final Comparator<String>                      myBoundNameComparator;
    private final Map<ModuleIdentity, ModuleEntity>       myModules;
    private final Map<String, SortedSet<ExportedBinding>> myExports;

    DocIndex(Predicate<ModuleIdentity> selector)
    {
        myModuleSelector = selector;
        myBoundNameComparator = new BindingComparator();
        myModules = new HashMap<>();
        myExports = new HashMap<>();
    }

    public Comparator<String> getBoundNameComparator()
    {
        return myBoundNameComparator;
    }


    //========================================================================


    void addModule(ModuleEntity module)
    {
        assert !myModules.containsKey(module.getIdentity());
        assert myModuleSelector.test(module.getIdentity());

        myModules.put(module.getIdentity(), module);

        module.exports().forEach(this::addExport);
    }


    private void addExport(ExportedBinding export)
    {
        Set<ExportedBinding> ids =
            myExports.computeIfAbsent(export.getName(),
                                      k -> new TreeSet<>(COMPARE_BY_MODULE));
        ids.add(export);
    }


    public AlphaIndex alphabetize()
    {
        AlphaIndex alpha = new AlphaIndex(myBoundNameComparator);
        myExports.forEach(alpha::addEntry);
        return alpha;
    }

    public PermutedIndex permute()
    {
        PermutedIndex permuted = new PermutedIndex(myBoundNameComparator);
        myExports.forEach(permuted::addEntries);
        return permuted;
    }

    public List<ExportedBinding> otherExportsOf(ExportedBinding binding)
    {
        assert binding.getName() != null;
        assert myExports.containsKey(binding.getName());

        return myExports.get(binding.getName())
                        .stream()
                        .filter(x -> x != binding)
                        .collect(toList());
    }
}
