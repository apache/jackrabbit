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
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.PersistentVersionManager;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.VersionException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
class InternalVersionHistoryImpl extends InternalVersionItemImpl
        implements InternalVersionHistory, Constants {
    /**
     * default logger
     */
    private static Logger log = Logger.getLogger(InternalVersionHistory.class);

    /**
     * the cache of the version labels
     * key = version label (String)
     * value = version
     */
    private HashMap labelCache = new HashMap();

    /**
     * the root version of this history
     */
    private InternalVersion rootVersion;

    /**
     * the hashmap of all versions
     * key = versionId (String)
     * value = version
     */
    private HashMap versionCache = new HashMap();

    /**
     * The nodes state of this version history
     */
    private PersistentNode node;

    /**
     * the node that holds the label nodes
     */
    private PersistentNode labelNode;

    /**
     * the id of this history
     */
    private String historyId;

    /**
     * the if of the versionable node
     */
    private String versionableId;

    /**
     * Creates a new VersionHistory object for the given node state.
     */
    InternalVersionHistoryImpl(PersistentVersionManager vMgr, PersistentNode node) throws RepositoryException {
        super(vMgr);
        this.node = node;
        init();
    }

    /**
     * Initialies the history and loads all internal caches
     *
     * @throws RepositoryException
     */
    private void init() throws RepositoryException {
        versionCache.clear();
        labelCache.clear();

        // get id
        historyId = node.getUUID();

        // get versionable id
        versionableId = (String) node.getPropertyValue(NativePVM.PROPNAME_VERSIONABLE_ID).internalValue();

        // get entries
        PersistentNode[] children = node.getChildNodes();
        for (int i = 0; i < children.length; i++) {
            PersistentNode child = children[i];
            if (child.getName().equals(NativePVM.NODENAME_VERSION_LABELS)) {
                labelNode = child;
                continue;
            }
            InternalVersionImpl v = new InternalVersionImpl(this, child, child.getName());
            versionCache.put(v.getId(), v);
            if (v.isRootVersion()) {
                rootVersion = v;
            }
        }

        // resolve successors and predecessors
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersionImpl v = (InternalVersionImpl) iter.next();
            v.resolvePredecessors();
        }

        // init label cache
        PersistentNode labels[] = labelNode.getChildNodes();
        for (int i = 0; i < labels.length; i++) {
            PersistentNode lNode = labels[i];
            QName name = (QName) lNode.getPropertyValue(NativePVM.PROPNAME_NAME).internalValue();
            String ref = (String) lNode.getPropertyValue(NativePVM.PROPNAME_VERSION).internalValue();
            InternalVersionImpl v = (InternalVersionImpl) getVersion(ref);
            labelCache.put(name, v);
            v.internalAddLabel(name);
        }
    }

    /**
     * Returns the id of this version history
     *
     * @return
     */
    public String getId() {
        return historyId;
    }

    public InternalVersionItem getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getRootVersion() {
        return rootVersion;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersion(QName versionName) throws VersionException {
        // maybe add cache by name?
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            if (v.getName().equals(versionName)) {
                return v;
            }
        }
        throw new VersionException("Version " + versionName + " does not exist.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersion(QName versionName) {
        // maybe add cache?
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            if (v.getName().equals(versionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the version for the given uuid exists
     *
     * @param uuid
     * @return
     */
    public boolean hasVersion(String uuid) {
        return versionCache.containsKey(uuid);
    }

    /**
     * Returns the version with the given uuid or <code>null</code> if the
     * respective version does not exist.
     *
     * @param uuid
     * @return
     */
    public InternalVersion getVersion(String uuid) {
        return (InternalVersion) versionCache.get(uuid);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersionByLabel(QName label) {
        return (InternalVersion) labelCache.get(label);
    }

    /**
     * Removes the indicated version from this VersionHistory. If the specified
     * vesion does not exist, if it specifies the root version or if it is
     * referenced by any node e.g. as base version, a VersionException is thrown.
     * <p/>
     * all successors of the removed version become successors of the
     * predecessors of the removed version and vice versa. then, the entire
     * version node and all its subnodes are removed.
     *
     * @param versionName
     * @throws VersionException
     */
    public void removeVersion(QName versionName) throws VersionException {

        InternalVersionImpl v = (InternalVersionImpl) getVersion(versionName);
        if (v.equals(rootVersion)) {
            String msg = "Removal of " + versionName + " not allowed.";
            log.debug(msg);
            throw new VersionException(msg);
        }
        // check if any references (from outside the version storage) exist on this version
        List refs = getVersionManager().getItemReferences(v);
        if (!refs.isEmpty()) {
            throw new VersionException("Unable to remove version. At least once referenced.");
        }

        try {
            boolean succeeded = false;
            UpdatableItemStateManager stateMgr = getVersionManager().getItemStateMgr();
            try {
                stateMgr.edit();

                // remove from persistance state
                node.removeNode(v.getNode().getName());

                // unregister from labels
                QName[] labels = v.internalGetLabels();
                for (int i = 0; i < labels.length; i++) {
                    v.internalRemoveLabel(labels[i]);
                    labelNode.removeNode(labels[i]);
                }
                // detach from the version graph
                v.internalDetach();

                // and remove from history
                versionCache.remove(v.getId());

                // store changes
                node.store();

                stateMgr.update();
                succeeded = true;
                notifyModifed();
            } finally {
                if (!succeeded) {
                    // update operation failed, cancel all modifications
                    stateMgr.cancel();
                }
            }
        } catch (ItemStateException e) {
            throw new VersionException("Unable to store modifications", e);
        } catch (RepositoryException e) {
            throw new VersionException("error while storing modifications", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion addVersionLabel(QName versionName, QName label, boolean move)
            throws VersionException {

        InternalVersion version = getVersion(versionName);
        if (version == null) {
            throw new VersionException("Version " + versionName + " does not exist in this version history.");
        }

        InternalVersionImpl prev = (InternalVersionImpl) labelCache.get(label);
        if (version.equals(prev)) {
            // ignore
            return version;
        } else if (prev != null && !move) {
            // already defined elsewhere, throw
            throw new VersionException("Version label " + label + " already defined for version " + prev.getName());
        } else if (prev != null) {
            // if already defined, but move, remove old label first
            removeVersionLabel(label);
        }
        labelCache.put(label, version);
        ((InternalVersionImpl) version).internalAddLabel(label);
        UpdatableItemStateManager stateMgr = getVersionManager().getItemStateMgr();
        boolean succeeded = false;
        try {
            stateMgr.edit();
            PersistentNode lNode = labelNode.addNode(label, NT_UNSTRUCTURED, null);
            lNode.setPropertyValue(NativePVM.PROPNAME_NAME, InternalValue.create(label));
            lNode.setPropertyValue(NativePVM.PROPNAME_VERSION, InternalValue.create(version.getId()));
            labelNode.store();

            stateMgr.update();
            succeeded = true;
        } catch (ItemStateException e) {
            throw new VersionException("Error while storing modifications", e);
        } catch (RepositoryException e) {
            throw new VersionException("Error while storing modifications", e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
        notifyModifed();
        return prev;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion removeVersionLabel(QName label) throws VersionException {


        InternalVersionImpl v = (InternalVersionImpl) labelCache.remove(label);
        if (v == null) {
            throw new VersionException("Version label " + label + " is not in version history.");
        }
        v.internalRemoveLabel(label);
        UpdatableItemStateManager stateMgr = getVersionManager().getItemStateMgr();
        boolean succeeded = false;
        try {
            stateMgr.edit();
            labelNode.removeNode(label);
            labelNode.store();
            stateMgr.update();
            succeeded = true;
        } catch (ItemStateException e) {
            throw new VersionException("Unable to store modifications", e);
        } catch (RepositoryException e) {
            throw new VersionException("Unable to store modifications", e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
        notifyModifed();
        return v;
    }

    /**
     * Checks in a node. It creates a new version with the given name and freezes
     * the state of the given node.
     *
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    protected InternalVersionImpl checkin(QName name, NodeImpl src)
            throws RepositoryException {

        // copy predecessors from src node
        Value[] preds = src.getProperty(JCR_PREDECESSORS).getValues();
        InternalValue[] predecessors = new InternalValue[preds.length];
        for (int i = 0; i < preds.length; i++) {
            String predId = preds[i].getString();
            // check if version exist
            if (!versionCache.containsKey(predId)) {
                throw new RepositoryException("invalid predecessor in source node");
            }
            predecessors[i] = InternalValue.create(predId);
        }

        String versionId = UUID.randomUUID().toString();
        PersistentNode vNode = node.addNode(name, NativePVM.NT_REP_VERSION, versionId);

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(JCR_CREATED, InternalValue.create(Calendar.getInstance()));
        vNode.setPropertyValues(JCR_PREDECESSORS, PropertyType.STRING, predecessors);

        // checkin source node
        InternalFrozenNodeImpl.checkin(vNode, JCR_FROZENNODE, src, false, false);

        // and store
        node.store();

        // update version graph
        InternalVersionImpl version = new InternalVersionImpl(this, vNode, name);
        version.resolvePredecessors();

        // update cache
        versionCache.put(version.getId(), version);

        return version;
    }

    /**
     * Returns an iterator over all versions (not ordered yet)
     *
     * @return
     */
    public Iterator getVersions() {
        return versionCache.values().iterator();
    }

    /**
     * Returns the number of versions
     *
     * @return
     */
    public int getNumVersions() {
        return versionCache.size();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionableUUID() {
        return versionableId;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getVersionLabels() {
        return (QName[]) labelCache.keySet().toArray(new QName[labelCache.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionLabelsUUID() {
        return labelNode.getUUID();
    }

    /**
     * Returns the persistent node of this version history
     * @return
     */
    protected PersistentNode getNode() {
        return node;
    }

    /**
     * Creates a new <code>InternalVersionHistory</code> below the given parent
     * node and with the given name.
     *
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    protected static InternalVersionHistoryImpl create(PersistentVersionManager vMgr, PersistentNode parent, String historyId, QName name, NodeImpl src)
            throws RepositoryException {

        // create history node
        PersistentNode pNode = parent.addNode(name, NativePVM.NT_REP_VERSION_HISTORY, historyId);

        // set the versionable uuid
        pNode.setPropertyValue(NativePVM.PROPNAME_VERSIONABLE_ID, InternalValue.create(src.internalGetUUID()));

        // create label node
        pNode.addNode(NativePVM.NODENAME_VERSION_LABELS, NT_UNSTRUCTURED, null);

        // create root version
        String versionId = UUID.randomUUID().toString();

        PersistentNode vNode = pNode.addNode(JCR_ROOTVERSION, NativePVM.NT_REP_VERSION, versionId);

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(JCR_CREATED, InternalValue.create(Calendar.getInstance()));
        vNode.setPropertyValues(JCR_PREDECESSORS, PropertyType.REFERENCE, new InternalValue[0]);

        // add also an empty frozen node to the root version
        InternalFrozenNodeImpl.checkin(vNode, JCR_FROZENNODE, src, true, false);

        parent.store();
        return new InternalVersionHistoryImpl(vMgr, pNode);
    }
}
