/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * <code>NodeReferences</code> represents the references (i.e. properties of
 * type <code>REFERENCE</code>) to a particular node (denoted by its uuid).
 */
public class NodeReferences implements Serializable {

    /**
     * Serial UID
     */
    static final long serialVersionUID = 7007727035982680717L;

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(NodeReferences.class);

    /**
     * id of the target node
     */
    protected NodeId targetId;

    /**
     * list of PropertyId's (i.e. the id's of the properties that refer to
     * the target node denoted by <code>targetId</code>).
     * <p/>
     * note that the list can contain duplicate entries because a specific
     * REFERENCE property can contain multiple references (if it's multi-valued)
     * to potentially the same target node.
     */
    protected ArrayList references = new ArrayList();

    /**
     * New state
     */
    public static final int STATUS_NEW = 0;

    /**
     * Existing state
     */
    public static final int STATUS_EXISTING = 1;

    /**
     * Destroyed state
     */
    public static final int STATUS_DESTROYED = 2;

    /**
     * the internal status of this item state
     */
    protected int status = STATUS_NEW;

    /**
     * Backing state (may be null)
     */
    private NodeReferences overlayed;

    /**
     * Package private constructor
     *
     * @param targetId
     */
    public NodeReferences(NodeId targetId) {
        this.targetId  = targetId;
    }

    /**
     * Package private constructor
     *
     * @param overlayed overlayed state
     */
    public NodeReferences(NodeReferences overlayed) {
        this.overlayed = overlayed;

        pull();
    }

    /**
     * Copy information from another references object into this object
     * @param refs source references object
     */
    void copy(NodeReferences refs) {
        targetId = refs.targetId;
        references.clear();
        references.addAll(refs.getReferences());
    }

    /**
     * Pull information from overlayed object.
     */
    void pull() {
        if (overlayed != null) {
            copy(overlayed);
        }
    }

    /**
     * Push information into overlayed object.
     */
    void push() {
        if (overlayed != null) {
            overlayed.copy(this);
        }
    }

    /**
     * Connect this object to an underlying overlayed object
     */
    void connect(NodeReferences overlayed) {
        if (this.overlayed != null) {
            throw new IllegalStateException(
                    "References object already connected: " + this);
        }
        this.overlayed = overlayed;
    }

    /**
     * Disconnect this object from the underlying overlayed object.
     */
    void disconnect() {
        if (overlayed != null) {
            overlayed = null;
        }
    }

    /**
     * @return
     */
    public NodeId getTargetId() {
        return targetId;
    }

    /**
     * @return
     */
    public boolean hasReferences() {
        return !references.isEmpty();
    }

    /**
     * @return
     */
    public List getReferences() {
        return Collections.unmodifiableList(references);
    }

    /**
     * @param refId
     */
    public void addReference(PropertyId refId) {
        references.add(refId);
    }

    /**
     * @param references
     */
    public void addAllReferences(Set references) {
        references.addAll(references);
    }

    /**
     * @param refId
     * @return
     */
    public boolean removeReference(PropertyId refId) {
        return references.remove(refId);
    }

    /**
     *
     */
    public void clearAllReferences() {
        references.clear();
    }

    /**
     * Returns the status of this item.
     *
     * @return the status of this item.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the new status of this item.
     *
     * @param newStatus the new status
     */
    public void setStatus(int newStatus) {
        switch (newStatus) {
            case STATUS_NEW:
            case STATUS_EXISTING:
            case STATUS_DESTROYED:
                status = newStatus;
                return;
        }
        String msg = "illegal status: " + newStatus;
        log.error(msg);
        throw new IllegalArgumentException(msg);
    }
}
