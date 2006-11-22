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

import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 * <code>RemoveVersion</code>...
 */
public class RemoveVersion extends Remove {

    protected RemoveVersion(ItemState removeState) {
        super(removeState);
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
     * @see Operation#persisted()
     */
    public void persisted() {
        NodeState parent = removeState.getParent();
        removeState.invalidate(true);
        parent.invalidate(false);
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(NodeState versionState) {
        RemoveVersion rm = new RemoveVersion(versionState);
        return rm;
    }
}
