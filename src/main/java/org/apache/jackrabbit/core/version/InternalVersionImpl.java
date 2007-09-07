/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.QName;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * Implements a <code>InternalVersion</code>
 */
class InternalVersionImpl extends InternalVersionItemImpl
        implements InternalVersion {

    /**
     * the date when this version was created
     */
    private Calendar created;

    /**
     * the set of version labes of this history (values == String)
     */
    private HashSet labelCache = null;

    /**
     * specifies if this is the root version
     */
    private final boolean isRoot;

    /**
     * the version name
     */
    private final QName name;

    /**
     * the version history
     */
    private final InternalVersionHistory versionHistory;

    /**
     * Creates a new internal version with the given version history and
     * persistance node. please note, that versions must be created by the
     * version history.
     *
     * @param node
     */
    public InternalVersionImpl(InternalVersionHistoryImpl vh, NodeStateEx node, QName name) {
        super(vh.getVersionManager(), node);
        this.versionHistory = vh;
        this.name = name;

        // init internal values
        InternalValue[] values = node.getPropertyValues(QName.JCR_CREATED);
        if (values != null) {
            created = values[0].getDate();
        }
        isRoot = name.equals(QName.JCR_ROOTVERSION);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return versionHistory;
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public InternalFrozenNode getFrozenNode() {
        // get frozen node
        try {
            NodeState.ChildNodeEntry entry = node.getState().getChildNodeEntry(QName.JCR_FROZENNODE, 1);
            if (entry == null) {
                throw new InternalError("version has no frozen node: " + getId());
            }
            return (InternalFrozenNode) vMgr.getItem(entry.getId());
        } catch (RepositoryException e) {
            throw new IllegalStateException("unable to retrieve frozen node: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion[] getSuccessors() {
        vMgr.acquireReadLock();
        try {
            InternalValue[] values = node.getPropertyValues(QName.JCR_SUCCESSORS);
            if (values != null) {
                InternalVersion[] versions = new InternalVersion[values.length];
                for (int i = 0; i < values.length; i++) {
                    NodeId vId = new NodeId(values[i].getUUID());
                    versions[i] = versionHistory.getVersion(vId);
                }
                return versions;
            } else {
                return new InternalVersion[0];
            }
        } finally {
            vMgr.releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion[] getPredecessors() {
        InternalValue[] values = node.getPropertyValues(QName.JCR_PREDECESSORS);
        if (values != null) {
            InternalVersion[] versions = new InternalVersion[values.length];
            for (int i = 0; i < values.length; i++) {
                NodeId vId = new NodeId(values[i].getUUID());
                versions[i] = versionHistory.getVersion(vId);
            }
            return versions;
        } else {
            return new InternalVersion[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMoreRecent(InternalVersion v) {
        InternalVersion[] preds = getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            InternalVersion pred = preds[i];
            if (pred.equals(v) || pred.isMoreRecent(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory() {
        return versionHistory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLabel(QName label) {
        return internalHasLabel(label);
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getLabels() {
        return internalGetLabels();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRootVersion() {
        return isRoot;
    }

    /**
     * Clear the list of predecessors/successors and the label cache.
     */
    void clear() {
        labelCache = null;
    }

    /**
     * stores the given successors or predecessors to the persistance node
     *
     * @throws RepositoryException
     */
    private void storeXCessors(List cessors, QName propname, boolean store)
            throws RepositoryException {
        InternalValue[] values = new InternalValue[cessors.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.create(
                    ((InternalVersion) cessors.get(i)).getId().getUUID());
        }
        node.setPropertyValues(propname, PropertyType.STRING, values);
        if (store) {
            node.store();
        }
    }

    /**
     * Detaches itself from the version graph.
     *
     * @throws RepositoryException
     */
    void internalDetach() throws RepositoryException {
        // detach this from all successors
        InternalVersion[] succ = getSuccessors();
        for (int i = 0; i < succ.length; i++) {
            ((InternalVersionImpl) succ[i]).internalDetachPredecessor(this, true);
        }

        // detach cached successors from preds
        InternalVersion[] preds = getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            ((InternalVersionImpl) preds[i]).internalDetachSuccessor(this, true);
        }

        // clear properties
        clear();
    }

    /**
     * Attaches this version as successor to all predecessors. assuming that the
     * predecessors are already set.
     *
     * @throws RepositoryException
     */
    void internalAttach() throws RepositoryException {
        InternalVersion[] preds = getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            ((InternalVersionImpl) preds[i]).internalAddSuccessor(this, true);
        }
    }

    /**
     * Adds a version to the set of successors.
     *
     * @param succ
     * @param store
     * @throws RepositoryException
     */
    private void internalAddSuccessor(InternalVersionImpl succ, boolean store)
            throws RepositoryException {
        List l = new ArrayList(Arrays.asList(getSuccessors()));
        if (!l.contains(succ)) {
            l.add(succ);
            storeXCessors(l, QName.JCR_SUCCESSORS, store);
        }
    }

    /**
     * Removes the predecessor V of this predecessors list and adds all of Vs
     * predecessors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachPredecessor(InternalVersionImpl v, boolean store)
            throws RepositoryException {
        // remove 'v' from predecessor list
        List l = new ArrayList(Arrays.asList(getPredecessors()));
        l.remove(v);

        // attach v's predecessors
        l.addAll(Arrays.asList(v.getPredecessors()));
        storeXCessors(l, QName.JCR_PREDECESSORS, store);
    }

    /**
     * Removes the successor V of this successors list and adds all of Vs
     * successors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachSuccessor(InternalVersionImpl v, boolean store)
            throws RepositoryException {
        // remove 'v' from successors list
        List l = new ArrayList(Arrays.asList(getSuccessors()));
        l.remove(v);

        // attach v's successors
        l.addAll(Arrays.asList(v.getSuccessors()));
        storeXCessors(l, QName.JCR_SUCCESSORS, store);
    }

    /**
     * adds a label to the label cache. does not affect storage
     *
     * @param label
     * @return <code>true</code> if the label was added
     */
    boolean internalAddLabel(QName label) {
        if (labelCache == null) {
            labelCache = new HashSet();
        }
        return labelCache.add(label);
    }

    /**
     * removes a label from the label cache. does not affect storage
     *
     * @param label
     * @return <code>true</code> if the label was removed
     */
    boolean internalRemoveLabel(QName label) {
        if (labelCache == null) {
            return false;
        } else {
            return labelCache.remove(label);
        }
    }

    /**
     * checks, if a label is in the label cache
     *
     * @param label
     * @return <code>true</code> if the label exists
     */
    boolean internalHasLabel(QName label) {
        if (labelCache == null) {
            return false;
        } else {
            return labelCache.contains(label);
        }
    }

    /**
     * returns the array of the cached labels
     *
     * @return the internal labels
     */
    QName[] internalGetLabels() {
        if (labelCache == null) {
            return new QName[0];
        } else {
            return (QName[]) labelCache.toArray(new QName[labelCache.size()]);
        }
    }

    /**
     * Invalidate this item.
     */
    void invalidate() {
        node.getState().discard();
    }

    /**
     * Resolves jcr:successor properties that are missing.
     *
     * @throws RepositoryException
     */
    void legacyResolveSuccessors() throws RepositoryException {
        InternalValue[] values = node.getPropertyValues(QName.JCR_PREDECESSORS);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                NodeId vId = new NodeId(values[i].getUUID());
                InternalVersionImpl v = (InternalVersionImpl) versionHistory.getVersion(vId);
                v.internalAddSuccessor(this, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InternalVersionImpl) {
            InternalVersionImpl v = (InternalVersionImpl) obj;
            return v.getId().equals(getId());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return getId().hashCode();
    }
}
