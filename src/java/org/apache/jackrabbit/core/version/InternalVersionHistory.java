/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.util.Text;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This Class implements a version history.
 */
public class InternalVersionHistory {

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
     * the version manager
     */
    private final PersistentVersionManager vMgr;

    /**
     * Creates a new VersionHistory object for the given node state.
     */
    InternalVersionHistory(PersistentVersionManager vMgr, PersistentNode node) throws RepositoryException {
        this.vMgr = vMgr;
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
        historyId = (String) node.getPropertyValue(PersistentVersionManager.PROPNAME_HISTORY_ID).internalValue();

        // get entries
        PersistentNode[] children = node.getChildNodes();
        for (int i = 0; i < children.length; i++) {
            PersistentNode child = children[i];
            if (child.getName().equals(PersistentVersionManager.NODENAME_VERSION_LABELS)) {
                labelNode = child;
                continue;
            }
            InternalVersion v = new InternalVersion(this, child);
            versionCache.put(v.getId(), v);
            if (v.isRootVersion()) {
                rootVersion = v;
            }
        }

        // resolve successors and predecessors
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            v.resolvePredecessors();
        }

        // init label cache
        PersistentNode labels[] = labelNode.getChildNodes();
        for (int i = 0; i < labels.length; i++) {
            PersistentNode lNode = labels[i];
            String name = (String) lNode.getPropertyValue(PersistentVersionManager.PROPNAME_NAME).internalValue();
            String ref = (String) lNode.getPropertyValue(PersistentVersionManager.PROPNAME_VERSION).internalValue();
            InternalVersion v = getVersion(ref);
            labelCache.put(name, v);
            v.internalAddLabel(name);
        }
    }

    /**
     * returns the version manager
     *
     * @return
     */
    public PersistentVersionManager getVersionManager() {
        return vMgr;
    }

    /**
     * Returns the id of this version history
     *
     * @return
     */
    public String getId() {
        return historyId;
    }

    /**
     * @see VersionHistory#getRootVersion()
     */
    public InternalVersion getRootVersion() throws RepositoryException {
        return rootVersion;
    }

    /**
     * @see VersionHistory#getVersion(java.lang.String)
     */
    public InternalVersion getVersion(QName versionName) throws RepositoryException {
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
     * @see VersionHistory#hasVersion(String)
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
     * @see VersionHistory#getVersionByLabel(java.lang.String)
     */
    public InternalVersion getVersionByLabel(String label) throws RepositoryException {
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
     * @throws RepositoryException
     */
    public void removeVersion(QName versionName) throws RepositoryException {
        InternalVersion v = (InternalVersion) getVersion(versionName);
        if (v.equals(rootVersion)) {
            String msg = "Removal of " + versionName + " not allowed.";
            log.error(msg);
            throw new VersionException(msg);
        }

        // remove from persistance state
        node.removeNode(versionName);

        // unregister from labels
        String[] labels = v.internalGetLabels();
        for (int i = 0; i < labels.length; i++) {
            v.internalRemoveLabel(labels[i]);
            QName name = new QName("", Text.md5(labels[i]));
            labelNode.removeNode(name);
        }

        // detach from the version graph
        v.internalDetach();

        // and remove from history
        versionCache.remove(v.getId());
        store();

    }

    /**
     * Adds a label to a version
     *
     * @param version
     * @param label
     * @throws RepositoryException
     */
    public void addVersionLabel(InternalVersion version, String label, boolean move)
            throws VersionException, RepositoryException {
        InternalVersion prev = (InternalVersion) labelCache.get(label);
        if (version.equals(prev)) {
            // ignore
            return;
        } else if (prev != null && !move) {
            // already defined elsewhere, throw
            throw new VersionException("Version label " + label + " already defined for version " + prev.getName());
        } else if (prev != null) {
            // if already defined, but move, remove old label first
            removeVersionLabel(label);
        }
        labelCache.put(label, version);
        version.internalAddLabel(label);
        QName name = new QName("", Text.md5(label));
        PersistentNode lNode = labelNode.addNode(name, NodeTypeRegistry.NT_UNSTRUCTURED);
        lNode.setPropertyValue(PersistentVersionManager.PROPNAME_NAME, InternalValue.create(label));
        lNode.setPropertyValue(PersistentVersionManager.PROPNAME_VERSION, InternalValue.create(version.getId()));
        labelNode.store();

        // inform manager
        vMgr.onVersionModified(version);
    }

    /**
     * Removes the label from the respective version
     *
     * @param label
     * @throws RepositoryException if the label does not exist
     */
    public void removeVersionLabel(String label) throws RepositoryException {
        InternalVersion v = (InternalVersion) labelCache.remove(label);
        if (v == null) {
            throw new RepositoryException("Version label " + label + " is not in version history.");
        }
        v.internalRemoveLabel(label);
        QName name = new QName("", Text.md5(label));
        labelNode.removeNode(name);
        labelNode.store();

        // inform manager
        vMgr.onVersionModified(v);
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
    protected InternalVersion checkin(QName name, NodeImpl src)
            throws RepositoryException {

        // copy predecessors from src node
        Value[] preds = src.getProperty(VersionManager.PROPNAME_PREDECESSORS).getValues();
        InternalValue[] predecessors = new InternalValue[preds.length];
        for (int i = 0; i < preds.length; i++) {
            String predId = preds[i].getString();
            // check if version exist
            if (!versionCache.containsKey(predId)) {
                throw new RepositoryException("invalid predecessor in source node");
            }
            predecessors[i] = InternalValue.create(predId);
        }

        PersistentNode vNode = node.addNode(name, NodeTypeRegistry.NT_UNSTRUCTURED);
        String versionId = UUID.randomUUID().toString();
        vNode.setPropertyValue(PersistentVersionManager.PROPNAME_VERSION_ID, InternalValue.create(versionId));

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(VersionManager.PROPNAME_CREATED, InternalValue.create(Calendar.getInstance()));
        vNode.setPropertyValues(VersionManager.PROPNAME_PREDECESSORS, PropertyType.STRING, predecessors);

        // checkin source node
        InternalFrozenNode.checkin(vNode, PersistentVersionManager.NODENAME_FROZEN, src, false, false);

        // and store
        store();

        // update version graph
        InternalVersion version = new InternalVersion(this, vNode);
        version.resolvePredecessors();

        // update cache
        versionCache.put(version.getId(), version);

        return version;
    }


    /**
     * Stores the changes made to this version history
     *
     * @throws RepositoryException
     */
    protected void store() throws RepositoryException {
        node.store();
    }

    /**
     * discards the changes made to this version history
     *
     * @throws RepositoryException
     */
    protected void reload() throws RepositoryException {
        node.reload();
        init();
    }

    /**
     * Returns an iterator over all versions (not ordered yet)
     *
     * @return
     */
    protected Iterator getVersions() {
        return versionCache.values().iterator();
    }

    /**
     * Returns the number of versions
     *
     * @return
     */
    protected int getNumVersions() {
        return versionCache.size();
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
    protected static InternalVersionHistory create(PersistentVersionManager vMgr, PersistentNode parent, String historyId, QName name, NodeImpl src)
            throws RepositoryException {

        // create history node
        PersistentNode pNode = parent.addNode(name, NodeTypeRegistry.NT_UNSTRUCTURED);
        pNode.setPropertyValue(PersistentVersionManager.PROPNAME_HISTORY_ID, InternalValue.create(historyId));

        // create label node
        pNode.addNode(PersistentVersionManager.NODENAME_VERSION_LABELS, NodeTypeRegistry.NT_UNSTRUCTURED);

        // create root version
        String versionId = UUID.randomUUID().toString();
        PersistentNode vNode = pNode.addNode(VersionManager.NODENAME_ROOTVERSION, NodeTypeRegistry.NT_UNSTRUCTURED);
        vNode.setPropertyValue(PersistentVersionManager.PROPNAME_VERSION_ID, InternalValue.create(versionId));

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(VersionManager.PROPNAME_CREATED, InternalValue.create(Calendar.getInstance()));
        vNode.setPropertyValues(VersionManager.PROPNAME_PREDECESSORS, PropertyType.REFERENCE, new InternalValue[0]);

        // add also an empty frozen node to the root version
        InternalFrozenNode.checkin(vNode, PersistentVersionManager.NODENAME_FROZEN, src, true, false);

        parent.store();
        return new InternalVersionHistory(vMgr, pNode);
    }
}


