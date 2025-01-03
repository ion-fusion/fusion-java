// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

/**
 * Exposes binding metadata about an identifier in Fusion source code.
 * <p>
 * <b>WARNING: This class is experimental and unstable.</b>
 * Use at your own risk.
 * <p>
 * {@code BindingSite} instances form a chain from some point-of-use
 * identifier (that is, a variable reference), through applicable
 * {@code require} and {@code provide} forms, to the point of definition.
 * <p>
 * Calling {@link #target()} will get the site at the end of the chain: the
 * identifier's original binding site, wherever that may be.
 */
public class BindingSite
{
    private final SourceLocation srcLoc;
    private final BindingSite    targetSite;

    private BindingSite(SourceLocation sourceLocation,
                        BindingSite    targetInformation)
    {
        srcLoc = sourceLocation;
        targetSite = targetInformation;
    }


    /**
     * Returns the location of this site.
     *
     * @return null if called on a site with no explicit identifier.
     * This happens when there is no identifier given during import or export.
     */
    public SourceLocation getSourceLocation()
    {
        return srcLoc;
    }


    /**
     * Gets the next site in the chain.
     * <ul>
     *   <li>The next site of a local or definition site is null.
     *   <li>The next site of an import site is an export site.
     *   <li>The next site of an export site is an import, local, or definition
     *       site
     * </ul>
     */
    BindingSite nextSite()
    {
        // TODO Decide whether this should be exposed or if its even needed.
        return null;
    }

    /**
     * Indicates whether this site is where a local binding was declared.
     */
    boolean isLocalSite()
    {
        return false;
    }

    /**
     * Indicates whether this site is where a (namespace-level) binding was
     * {@code define}d.
     */
    boolean isDefinitionSite()
    {
        return false;
    }

    /**
     * Indicates whether this site is where a binding was {@code provide}d by
     * a module.
     */
    boolean isExportSite()
    {
        return false;
    }

    /**
     * Indicates whether this site is where a binding was {@code require}d or
     * imported via a language declaration.
     */
    boolean isImportSite()
    {
        return false;
    }



    /**
     * Gets the site of the originating binding symbol, as declared by
     * {@code define}, {@code lambda}, {@code let}, and so on.
     * This jumps across modules, renames, and files to get to the original
     * definition site.
     *
     * @return not null. Returns itself iff the Binding was original.
     */
    public BindingSite target()
    {
        if (targetSite == null)
        {
            return this;
        }
        return targetSite;
    }


    //=========================================================================


    private static final class LocalSite
        extends BindingSite
    {
        private LocalSite(SourceLocation bindingIdLoc)
        {
            super(bindingIdLoc, null);
        }

        @Override
        public boolean isLocalSite()
        {
            return true;
        }
    }


    private static final class DefinitionSite
        extends BindingSite
    {
        private DefinitionSite(SourceLocation definitionLoc)
        {
            super(definitionLoc, null);
        }

        @Override
        public boolean isDefinitionSite()
        {
            return true;
        }
    }


    private static final class ExportSite
        extends BindingSite
    {
        private final BindingSite myInsideSite;

        private ExportSite(SourceLocation outsideIdLoc,
                           BindingSite    insideSite)
        {
            super(outsideIdLoc, insideSite.target());
            myInsideSite = insideSite;
        }

        @Override
        public boolean isExportSite()
        {
            return true;
        }

        @Override
        public BindingSite nextSite()
        {
            return myInsideSite;
        }
    }


    private static final class ImportSite
        extends BindingSite
    {
        private final BindingSite myExportSite;

        private ImportSite(SourceLocation importLocation,
                           BindingSite    exportSite)
        {
            super(importLocation, exportSite.target());
            myExportSite = exportSite;
        }

        @Override
        public boolean isImportSite()
        {
            return true;
        }

        @Override
        public BindingSite nextSite()
        {
            return myExportSite;
        }
    }


    //=========================================================================
    // Factory Methods


    /**
     * Creates a site for a local binding identifier, like the arguments to
     * procedures and the local variables declared by {@code let},
     * {@code letrec}, <i>etc.</i>
     *
     * @param bindingIdLoc the location of the identifier where the binding
     * was declared.
     */
    static BindingSite makeLocalBindingSite(SourceLocation bindingIdLoc)
    {
        return new LocalSite(bindingIdLoc);
    }

    /**
     * Creates a site for a namespace-level definition.
     *
     * @param definitionLoc the location of the identifier where the binding
     * was declared.
     */
    static BindingSite makeDefineBindingSite(SourceLocation definitionLoc)
    {
        return new DefinitionSite(definitionLoc);
    }

    /**
     * Creates a site for an exported binding.
     *
     * @param outsideIdLoc the location of the identifier declaring the
     *   exported name. May be null, since not all exports explicitly declare
     *   their names.
     * @param insideSite the site of the module-level definition or import
     *   that's being exported.
     */
    static BindingSite makeExportBindingSite(SourceLocation outsideIdLoc,
                                             BindingSite    insideSite)
    {
        return new ExportSite(outsideIdLoc, insideSite);
    }


    static BindingSite makeImportBindingSite(SourceLocation importLocation,
                                             BindingSite    exportSite)
    {
        return new ImportSite(importLocation, exportSite);
    }
}
