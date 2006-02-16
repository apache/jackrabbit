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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.uuid.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

/**
 * Implements a <code>InternalVersion</code>
 */
class InternalVersionImpl extends InternalVersionItemImpl
        implements InternalVersion {

    /**
     * the list/cache of predecessors (values == InternalVersion)
     */
    private ArrayList predecessors = new ArrayList();

    /**
     * the list of successors (values == InternalVersion)
     */
    private ArrayList successors = new ArrayList();

    /**
     * the underlying persistance node of this version
     */
    private NodeStateEx node;

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
        super(vh.getVersionManager());
        this.versionHistory = vh;
        this.node = node;
        this.name = name;

        // init internal values
        InternalValue[] values = node.getPropertyValues(QName.JCR_CREATED);
        if (values != null) {
            created = (Calendar) values[0].internalValue();
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
        return (InternalVersionImpl[]) successors.toArray(new InternalVersionImpl[successors.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion[] getPredecessors() {
        return (InternalVersionImpl[]) predecessors.toArray(new InternalVersionImpl[predecessors.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMoreRecent(InternalVersion v) {
        for (int i = 0; i < predecessors.size(); i++) {
            InternalVersion pred = (InternalVersion) predecessors.get(i);
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
     * resolves the predecessors property and indirectly adds it self to their
     * successor list.
     */
    void resolvePredecessors() {
        InternalValue[] values = node.getPropertyValues(QName.JCR_PREDECESSORS);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                NodeId vId = new NodeId((UUID) values[i].internalValue());
                InternalVersionImpl v = (InternalVersionImpl) versionHistory.getVersion(vId);
                predecessors.add(v);
                v.addSuccessor(this);
            }
        }
    }

    /**
     * Clear the list of predecessors/successors and the label cache.
     */
    void clear() {
        successors.clear();
        predecessors.clear();
        labelCache = null;
    }

    /**
     * adds a successor version to the internal cache
     *
     * @param successor
     */
    private void addSuccessor(InternalVersion successor) {
        successors.add(successor);
    }

    /**
     * stores the internal predecessor cache to the persistance node
     *
     * @throws RepositoryException
     */
    private void storePredecessors() throws RepositoryException {
        InternalValue[] values = new InternalValue[predecessors.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.create(
                    ((InternalVersion) predecessors.get(i)).getId().getUUID());
        }
        node.setPropertyValues(QName.JCR_PREDECESSORS, PropertyType.STRING, values);
    }

    /**
     * Detaches itself from the version graph.
     *
     * @throws RepositoryException
     */
    void internalDetach() throws RepositoryException {
        // detach this from all successors
        InternalVersionImpl[] succ = (InternalVersionImpl[]) getSuccessors();
        for (int i = 0; i < succ.length; i++) {
            succ[i].internalDetachPredecessor(this);
        }

        // detach cached successors from preds
        InternalVersionImpl[] preds = (InternalVersionImpl[]) getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            preds[i].internalDetachSuccessor(this);
        }

        // clear properties
        clear();
    }

    /**
     * Removes the predecessor V of this predecessors list and adds all of Vs
     * predecessors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachPredecessor(InternalVersionImpl v) throws RepositoryException {
        // remove 'v' from predecessor list
        for (int i = 0; i < predecessors.size(); i++) {
            if (predecessors.get(i).equals(v)) {
                predecessors.remove(i);
                break;
            }
        }
        // attach v's predecessors
        predecessors.addAll(Arrays.asList(v.getPredecessors()));
        storePredecessors();
        node.store();
    }

    /**
     * Removes the successor V of this successors list and adds all of Vs
     * successors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachSuccessor(InternalVersionImpl v) {
        // remove 'v' from successors list
        for (int i = 0; i < successors.size(); i++) {
            if (successors.get(i).equals(v)) {
                successors.remove(i);
                break;
            }
        }
        // attach v's successors
        successors.addAll(Arrays.asList(v.getSuccessors()));
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
}
