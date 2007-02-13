/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;
import java.util.Iterator;

/**
 * <code>RemoveVersion</code>...
 */
public class RemoveVersion extends Remove {

    private static Logger log = LoggerFactory.getLogger(RemoveVersion.class);

    private NodeEntry versionableEntry = null;

    protected RemoveVersion(ItemState removeState, NodeState parent, VersionManager mgr) {
        super(removeState, parent);
        try {
            versionableEntry = mgr.getVersionableNodeState((NodeState) removeState);
        } catch (RepositoryException e) {
            log.warn("Internal error", e);
        }
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been updated and all
     * its decendants. Second, the parent state gets invalidated.
     *
     * @see Operation#persisted(CacheBehaviour)
     * @param cacheBehaviour
     */
    public void persisted(CacheBehaviour cacheBehaviour) {
        if (cacheBehaviour == CacheBehaviour.INVALIDATE) {
            // invaliate the versionable node as well (version related properties)
            if (versionableEntry != null) {
                Iterator propEntries = versionableEntry.getPropertyEntries();
                while (propEntries.hasNext()) {
                    PropertyEntry pe = (PropertyEntry) propEntries.next();
                    pe.invalidate(false);
                }
                versionableEntry.invalidate(false);
            }

            // invalidate the versionhistory entry and all its children
            // in order to the the v-graph recalculated
            removeState.getHierarchyEntry().getParent().invalidate(true);
        }
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(NodeState versionState, NodeState vhState, VersionManager mgr) {
        RemoveVersion rm = new RemoveVersion(versionState, vhState, mgr);
        return rm;
    }
}
