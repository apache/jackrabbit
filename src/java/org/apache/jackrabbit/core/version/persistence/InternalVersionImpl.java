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
package org.apache.jackrabbit.core.version.persistence;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.InternalFrozenNode;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

/**
 *
 */
class InternalVersionImpl extends InternalVersionItemImpl
        implements InternalVersion, Constants {

    /**
     * the list/cache of predecessors (values == InternalVersion)
     */
    private ArrayList predecessors = new ArrayList();

    /**
     * the list of successors (values == InternalVersion)
     */
    private ArrayList successors = new ArrayList();

    /**
     * the underlaying persistance node of this version
     */
    private PersistentNode node;

    /**
     * the date when this version was created
     */
    private Calendar created;

    /**
     * the set of version labes of this history (values == String)
     */
    private HashSet labelCache = null;

    /**
     * the id of this version
     */
    private String versionId;

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
    InternalVersionImpl(InternalVersionHistoryImpl vh, PersistentNode node) {
        super(vh.getVersionManager());
        this.versionHistory = vh;
        this.node = node;

        // get id
        versionId = (String) node.getPropertyValue(NativePVM.PROPNAME_VERSION_ID).internalValue();

        // init internal values
        InternalValue[] values = node.getPropertyValues(JCR_CREATED);
        if (values != null) {
            created = (Calendar) values[0].internalValue();
        }
        values = node.getPropertyValues(NativePVM.PROPNAME_VERSION_NAME);
        if (values != null) {
            name = (QName) values[0].internalValue();
        } else {
            name = null; // ????
        }
        isRoot = name.equals(JCR_ROOTVERSION);
    }

    public String getId() {
        return versionId;
    }

    protected String getPersistentId() {
        return node.getUUID();
    }

    public InternalVersionItem getParent() {
        return versionHistory;
    }

    /**
     * Returns the name of this version
     *
     * @return
     */
    public QName getName() {
        return name;
    }

    protected PersistentNode getNode() {
        return node;
    }

    /**
     * Returns the frozen node
     *
     * @return
     */
    public InternalFrozenNode getFrozenNode() {
        // get frozen node
        try {
            NodeState.ChildNodeEntry entry = node.getState().getChildNodeEntry(JCR_FROZENNODE, 1);
            if (entry == null) {
                throw new InternalError("version has no frozen node: " + getId());
            }
            return (InternalFrozenNode) getVersionManager().getItemByInternal(entry.getUUID());
        } catch (RepositoryException e) {
            throw new IllegalStateException("unable to retrieve frozen node: " + e);
        }
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
     * resolves the predecessors property and indirectly adds it self to their
     * successor list.
     */
    void resolvePredecessors() {
        InternalValue[] values = node.getPropertyValues(JCR_PREDECESSORS);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalVersionImpl v = (InternalVersionImpl) versionHistory.getVersion(values[i].internalValue().toString());
                predecessors.add(v);
                v.addSuccessor(this);
            }
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
     * stores the internal predecessor cache to the persistance node
     *
     * @throws RepositoryException
     */
    private void storePredecessors() throws RepositoryException {
        InternalValue[] values = new InternalValue[predecessors.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.create(new UUID(((InternalVersion) predecessors.get(i)).getId()));
        }
        node.setPropertyValues(JCR_PREDECESSORS, PropertyType.STRING, values);
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
        successors.clear();
        predecessors.clear();
        labelCache = null;
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
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     */
    public boolean isMoreRecent(InternalVersion v) {
        for (int i = 0; i < predecessors.size(); i++) {
            InternalVersion pred = (InternalVersion) predecessors.get(i);
            if (pred.equals(this) || pred.isMoreRecent(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns the internal version history of this version
     *
     * @return
     */
    public InternalVersionHistory getVersionHistory() {
        return versionHistory;
    }

    /**
     * adds a label to the label cache. does not affect storage
     *
     * @param label
     * @return
     */
    protected boolean internalAddLabel(QName label) {
        if (labelCache == null) {
            labelCache = new HashSet();
        }
        return labelCache.add(label);
    }

    /**
     * removes a label from the label cache. does not affect storage
     *
     * @param label
     * @return
     */
    protected boolean internalRemoveLabel(QName label) {
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
     * @return
     */
    protected boolean internalHasLabel(QName label) {
        return labelCache == null ? false : labelCache.contains(label);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLabel(QName label) {
        return internalHasLabel(label);
    }

    /**
     * returns the array of the cached labels
     *
     * @return
     */
    protected QName[] internalGetLabels() {
        return labelCache == null ? new QName[0] : (QName[]) labelCache.toArray(new QName[labelCache.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getLabels() {
        return internalGetLabels();
    }

    /**
     * checks if this is the root version.
     *
     * @return <code>true</code> if this version is the root version;
     *         <code>false</code> otherwise.
     */
    public boolean isRootVersion() {
        return isRoot;
    }

}
