// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.runtime.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * An identifier for modules within the Fusion runtime system.
 * This plays the same role as Racket's "resolved module path", except it does
 * not contain location information about the source of the module.
 * <p>
 * Instances are interned because identity comparisons are extremely common
 * during compilation.
 * <p>
 * These identities may not uniquely map to module <em>instances</em>, since
 * a module may be instantiated in multiple registries and/or expansion phases.
 */
public class ModuleIdentity
    implements Comparable<ModuleIdentity>
{
    /**
     * Access to this map must be synchronized on it!
     */
    private static final Map<String,ModuleIdentity> ourInternedIdentities =
        new HashMap<>();
    // TODO https://github.com/ion-fusion/fusion-java/issues/164
    //   This should be a weak-reference table.

    /**
     * Counts the number of synthetic-scope module identities that have
     * been created.
     *
     * @see #forUniqueScope(ModuleIdentity)
     */
    private static final AtomicLong ourScopeCounter = new AtomicLong();


    private static final Pattern NAME_PATTERN =
        Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    private static final Pattern PATH_PATTERN =
        Pattern.compile("(/(" + NAME_PATTERN + "))+");


    /**
     * Checks whether a string can be used as a module name.
     * <p>
     * A valid module name starts with an ASCII letter, followed by zero or
     * more letters, digits, and underscores.
     *
     * @param name may be null.
     *
     * @return true iff the string is a valid module name.
     */
    public static boolean isValidModuleName(String name)
    {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }


    /**
     * Checks whether a string can be used as an absolute module path.
     * <p>
     * A valid absolute module path starts with a slash {@code "/"}, followed
     * by one or more module names, separated by slashes.
     *
     * @param path may be null.
     *
     * @return true iff the string is a valid absolute module path.
     */
    public static boolean isValidAbsoluteModulePath(String path)
    {
        return path != null && PATH_PATTERN.matcher(path).matches();
    }


    /**
     * Checks whether a string can be used as a module path.
     * <p>
     * A valid module path consists of an optional slash {@code "/"}, followed
     * by one or more module names, separated by slashes.
     *
     * @param path may be null.
     *
     * @return true iff the string is a valid module path.
     */
    public static boolean isValidModulePath(String path)
    {
        return isValidModuleName(path) || isValidAbsoluteModulePath(path);
    }


    private static ModuleIdentity doIntern(String path)
    {
        synchronized (ourInternedIdentities)
        {
            ModuleIdentity interned = ourInternedIdentities.get(path);
            if (interned != null) return interned;

            ModuleIdentity id = new ModuleIdentity(path);
            ourInternedIdentities.put(path, id);
            return id;
        }
    }


    /**
     * Produces an identity for the given absolute module path.
     *
     * @param path must be an absolute module path.
     * @return not null.
     *
     * @see #isValidAbsoluteModulePath(String)
     */
    public static ModuleIdentity forAbsolutePath(String path)
    {
        assert isValidAbsoluteModulePath(path);
        return doIntern(path);
    }


    /**
     * Produces an identity for a child module.
     *
     * @param parent may be null, in which case the result will be a root
     * module path.
     * @param name must be a valid module name (not a general path).
     * @return not null.
     *
     * @see #isValidModuleName(String)
     */
    public static ModuleIdentity forChild(ModuleIdentity parent, String name)
    {
        assert isValidModuleName(name);
        String path = (parent != null
                           ? parent.absolutePath() + '/' + name
                           : '/' + name);
        return doIntern(path);
    }


    /**
     * Produces an identity for a given module path, relative to a given module.
     *
     * @param baseModule must not be null if the path is relative.
     * @param path must be a valid absolute or relative module path.
     * @return not null.
     *
     * @see #isValidModulePath(String)
     */
    public static ModuleIdentity forPath(ModuleIdentity baseModule, String path)
    {
        if (! isValidAbsoluteModulePath(path))
        {
            // Relative path; currently only a simple name, but eventually
            // things like "../uncle"
            assert isValidModuleName(path);

            // We resolve simple names as children of the base module, which
            // aligns with the desired behavior at top-level. However, at
            // module-level, simple names currently denote *siblings* of the
            // current module. This is arguably a mistake; see #166.

            // This works by conspiring with Namespace#getResolutionBase()
            // which, for modules, returns the parent of the requesting module.

            String basePath = baseModule.absolutePath();
            path = basePath + "/" + path;
        }
        return doIntern(path);
    }


    /**
     * Produces a unique, unresolvable identity that is a child "scope" of the
     * given parent. Child modules added within the scope cannot resolve
     * relative paths to reach outside the scope. Further, code outside a scope
     * cannot resolve paths to modules within it.
     * <p>
     * The {@linkplain #baseName name} of the resulting path is unique within
     * the parent, but is not a valid module name.
     * The syntax of the name is unspecified and subject to change.
     *
     * @param parent the base path for the new scope. In general, the intent is
     * to use the same parent for all scopes of the same nature.
     * Must not be null.
     *
     * @return a unique module identity; the resulting absolute path is
     * <em>not</em> a valid module path.
     */
    public static ModuleIdentity forUniqueScope(ModuleIdentity parent)
    {
        long   scopeId = ourScopeCounter.incrementAndGet();
        String path    = parent.absolutePath() + '/' + scopeId;
        return doIntern(path);
    }


    /** An absolute path, not null or empty */
    private final String myPath;

    private ModuleIdentity(String name)
    {
        myPath = name;
    }


    @Override
    public String toString()
    {
        return myPath;
    }


    /**
     * Returns the absolute path of this identity.  The result can be turned
     * back into an (interned) identity via {@link #forAbsolutePath(String)}.
     *
     * @return a non-empty string starting (but not ending) with {@code '/'}.
     */
    public String absolutePath()
    {
        return myPath;
    }


    /**
     * Returns the name of the identified module; that is, the last name of its
     * path.
     *
     * @return not null.
     */
    public String baseName()
    {
        // TODO Should this be `moduleName()`?
        //  That makes more sense, but doesn't work for unresolvable identities

        int slashIndex = myPath.lastIndexOf('/');
        if (slashIndex == -1) return myPath;
        return myPath.substring(slashIndex + 1);
    }


    /**
     * Iterates the name components of this identity.
     *
     * @return not null.
     */
    public Iterator<String> iterate()
    {
        // Skip the leading slash, otherwise the first component will be "".
        return Arrays.asList(myPath.substring(1).split("/")).iterator();
    }


    /**
     * Returns the identity of this module's parent.
     *
     * @return null if there's no parent; eg if this is "/foo".
     */
    public ModuleIdentity parent()
    {
        String path = myPath;
        int slashIndex = path.lastIndexOf('/');
        assert slashIndex >= 0;
        if (slashIndex == 0)
        {
            return null;
        }
        else
        {
            // We assume that this prefix is a well-formed path.
            path = path.substring(0, slashIndex);
            return doIntern(path);
        }
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime + myPath.hashCode();
    }


    @Override
    public boolean equals(Object obj)
    {
        // We could use reference equality, since all instances are interned.
        // However, we need to store these in a weak map (see #164), in which
        // case we'll need this. See ActualSymbol#equals() for similar code.

        if (this == obj) return true;
        if (obj instanceof ModuleIdentity)
        {
            ModuleIdentity other = (ModuleIdentity) obj;
            return myPath.equals(other.myPath);
        }
        return false;
    }


    @Override
    public int compareTo(ModuleIdentity that)
    {
        return this.myPath.compareTo(that.myPath);
    }
}
