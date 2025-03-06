// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * A unique identifier for modules available to the Fusion runtime system.
 * This plays the same role as Racket's "resolved module path", except it does
 * not contain location information about the source of the module.
 * <p>
 * Instances are interned because identity comparisons are extremely common
 * during compilation.
 */
class ModuleIdentity
    implements Comparable<ModuleIdentity>
{
    static final String TOP_LEVEL_MODULE_PREFIX =
        "/fusion/private/toplevel/";

    /**
     * Access to this map must be synchronized on it!
     */
    private static final Map<String,ModuleIdentity> ourInternedIdentities =
        new HashMap<>();

    /**
     * Counts the number of synthetic top-level module identities that have
     * been created.
     *
     * @see #forTopLevel()
     */
    private static final AtomicLong ourTopLevelCounter = new AtomicLong();


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
    static boolean isValidModuleName(String name)
    {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    static boolean isValidAbsoluteModulePath(String path)
    {
        return path != null && PATH_PATTERN.matcher(path).matches();
    }

    static boolean isValidModulePath(String path)
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
     * @param path must be an absolute module path.
     * @return not null.
     *
     * @see #isValidAbsoluteModulePath(String)
     */
    static ModuleIdentity forAbsolutePath(String path)
    {
        assert isValidAbsoluteModulePath(path);
        return doIntern(path);
    }


    /**
     * Returns an identity for a child module.
     *
     * @param parent may be null, in which case the result will be a root
     * module path.
     * @param name must be a valid module name (not a general path).
     */
    static ModuleIdentity forChild(ModuleIdentity parent, String name)
    {
        assert isValidModuleName(name);
        String path = (parent != null
                           ? parent.absolutePath() + '/' + name
                           : '/' + name);
        return doIntern(path);
    }


    /**
     * @param baseModule must not be null if the path is relative.
     * @param path must be a valid absolute or relative module path.
     */
    static ModuleIdentity forPath(ModuleIdentity baseModule, String path)
    {
        if (! isValidAbsoluteModulePath(path))
        {
            // Relative path; currently only a simple name, but eventually
            // things like "../uncle"
            assert isValidModuleName(path);
            String basePath = baseModule.relativeBasePath();
            path = basePath + path;
        }
        return doIntern(path);
    }


    /**
     * Creates a <em>non-interned</em> identity for a top-level namespace.
     *
     * @return a fresh, non-interned identity.
     */
    static ModuleIdentity forTopLevel()
    {
        // TODO In a long-running service this could overflow.
        String path =
            TOP_LEVEL_MODULE_PREFIX + ourTopLevelCounter.incrementAndGet();

        return new ModuleIdentity(path)
        {
            @Override
            String relativeBasePath()
            {
                return absolutePath() + "/";
            }
        };
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
     * <p>
     * TODO NIO: Consider returning a Path here, for tighter modeling.
     *
     * @return a non-empty string starting (but not ending) with {@code '/'}.
     */
    public String absolutePath()
    {
        return myPath;
    }


    public String baseName()
    {
        int slashIndex = myPath.lastIndexOf('/');
        if (slashIndex == -1) return myPath;
        return myPath.substring(slashIndex + 1);
    }


    /**
     * Returns a base path to which a relative path can be appended.
     *
     * @return a string starting and ending with {@code '/'}.
     */
    String relativeBasePath()
    {
        String path = myPath;
        int slashIndex = path.lastIndexOf('/');
        assert slashIndex >= 0;
        if (slashIndex == 0)
        {
            path = "/";
        }
        else
        {
            path = path.substring(0, slashIndex + 1);
        }

        return path;
    }


    /**
     * @return null if there's no parent; eg if this is "/foo".
     */
    ModuleIdentity parent()
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
            path = path.substring(0, slashIndex);
            return forAbsolutePath(path);
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
        // Since we are interning instances, it would be nice to use reference
        // equality here. However we don't intern local IDs, so that's not
        // correct.

        if (this == obj) return true;
        if (obj == null) return false;
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
