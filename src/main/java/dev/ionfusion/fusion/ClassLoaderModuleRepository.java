// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.fusion.GlobalState.FUSION_SOURCE_EXTENSION;

import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class ClassLoaderModuleRepository
    extends ModuleRepository
{
    private final ClassLoader myClassLoader;
    private final String      myPathPrefix;

    ClassLoaderModuleRepository(ClassLoader cl, String pathPrefix)
    {
        myClassLoader = cl;
        myPathPrefix  = pathPrefix + "/modules";

        // We want to resolve relative to the classpath root(s).
        assert ! myPathPrefix.startsWith("/");
    }



    @Override
    String identify()
    {
        return "ClassLoader repository";
    }


    @Override
    ModuleLocation locateModule(Evaluator eval, final ModuleIdentity id)
        throws FusionException
    {
        final String resourceName =
            myPathPrefix + id.absolutePath() + FUSION_SOURCE_EXTENSION;

        // The protocol could be a jar: or file: (at least!)
        URL url = myClassLoader.getResource(resourceName);
        if (url == null) return null;

        return ModuleLocation.forUrl(id, url);
    }


    @Override
    void collectModules(Predicate<ModuleIdentity> selector,
                        Consumer<ModuleIdentity>  results)
        throws FusionException
    {
        // Nothing to do. We can't introspect the classloader.
        // TODO If the URL points to a Jar, we may be able to read its entries.
        //   Or, perhaps write a manifest/index into the repo root at build time.
    }
}
