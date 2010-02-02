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
package org.apache.jackrabbit.jcr2spi.version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.commons.iterator.FrozenNodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.VersionIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.ItemLifeCycleListener;
import org.apache.jackrabbit.jcr2spi.LazyItemIterator;
import org.apache.jackrabbit.jcr2spi.NodeImpl;
import org.apache.jackrabbit.jcr2spi.SessionImpl;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>VersionHistoryImpl</code>...
 */
public class VersionHistoryImpl extends NodeImpl implements VersionHistory {

    private static Logger log = LoggerFactory.getLogger(VersionHistoryImpl.class);

    private final NodeEntry vhEntry;
    private final NodeEntry labelNodeEntry;

    public VersionHistoryImpl(SessionImpl session, NodeState state, ItemLifeCycleListener[] listeners)
            throws VersionException, RepositoryException {
        super(session, state, listeners);
        this.vhEntry = (NodeEntry) state.getHierarchyEntry();

        // retrieve hierarchy entry of the jcr:versionLabels node
        labelNodeEntry = vhEntry.getNodeEntry(NameConstants.JCR_VERSIONLABELS, Path.INDEX_DEFAULT, true);
        if (labelNodeEntry == null) {
            String msg = "Unexpected error: nt:versionHistory requires a mandatory, autocreated child node jcr:versionLabels.";
            log.error(msg);
            throw new VersionException(msg);
        }
    }

    //-----------------------------------------------------< VersionHistory >---
    /**
     * @see VersionHistory#getVersionableUUID()
     */
    public String getVersionableUUID() throws RepositoryException {
        return getVersionableIdentifier();
    }

    /**
     * @see VersionHistory#getRootVersion()
     */
    public Version getRootVersion() throws RepositoryException {
        checkStatus();
        NodeEntry vEntry = vhEntry.getNodeEntry(NameConstants.JCR_ROOTVERSION, Path.INDEX_DEFAULT, true);
        if (vEntry == null) {
            String msg = "Unexpected error: VersionHistory state does not contain a root version child node entry.";
            log.error(msg);
            throw new RepositoryException(msg);
        }
        return (Version) getItemManager().getItem(vEntry);
    }

    /**
     * @see VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        checkStatus();
        refreshEntry(vhEntry);
        Iterator<NodeEntry> childIter = vhEntry.getNodeEntries();
        List<NodeEntry> versionEntries = new ArrayList<NodeEntry>();
        // all child-nodes except from jcr:versionLabels point to Versions.
        while (childIter.hasNext()) {
            NodeEntry entry = childIter.next();
            if (!NameConstants.JCR_VERSIONLABELS.equals(entry.getName())) {
                versionEntries.add(entry);
            }
        }
        return new LazyItemIterator(getItemManager(), new RangeIteratorAdapter(versionEntries));
    }

    /**
     * @see VersionHistory#getAllLinearVersions()
     */
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        checkStatus();

        // TODO: improve and use lazy loading of versions as needed.
        // TODO: change session.getNodeByUUID to Session.getNodeByIdentifier as soon as implemented

        List<Version> versions = new ArrayList<Version>();
        Version rootV = getRootVersion();
        Node vn = session.getNodeByUUID(getVersionableUUID());
        Version v = vn.getBaseVersion();
        while (v != null && !rootV.isSame(v)) {
            versions.add(0, v);
            v = v.getLinearPredecessor();
        }
        versions.add(0, rootV);

        return new VersionIteratorAdapter(versions);
    }

    /**
     * @see VersionHistory#getAllFrozenNodes()
     */
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        return new FrozenNodeIteratorAdapter(getAllVersions());
    }

    /**
     * @see VersionHistory#getAllLinearFrozenNodes()
     */
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        return new FrozenNodeIteratorAdapter(getAllLinearVersions());
    }

    /**
     * @see VersionHistory#getVersion(String)
     */
    public Version getVersion(String versionName) throws VersionException, RepositoryException {
        checkStatus();
        NodeState vState = getVersionState(versionName);
        return (Version) getItemManager().getItem(vState.getHierarchyEntry());
    }

    /**
     * @see VersionHistory#getVersionByLabel(String)
     */
    public Version getVersionByLabel(String label) throws RepositoryException {
        checkStatus();
        return getVersionByLabel(getQLabel(label));
    }

    /**
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(String versionName, String label, boolean moveLabel) throws VersionException, RepositoryException {
        checkStatus();
        Name qLabel = getQLabel(label);
        NodeState vState = getVersionState(versionName);
        // delegate to version manager that operates on workspace directly
        session.getVersionStateManager().addVersionLabel((NodeState) getItemState(), vState, qLabel, moveLabel);
    }

    /**
     * @see VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(String label) throws VersionException, RepositoryException {
        checkStatus();
        Name qLabel = getQLabel(label);
        Version version = getVersionByLabel(qLabel);
        NodeState vState = getVersionState(version.getName());
        // delegate to version manager that operates on workspace directly
        session.getVersionStateManager().removeVersionLabel((NodeState) getItemState(), vState, qLabel);
    }

    /**
     * @see VersionHistory#hasVersionLabel(String)
     */
    public boolean hasVersionLabel(String label) throws RepositoryException {
        checkStatus();
        Name l = getQLabel(label);
        Name[] qLabels = getQLabels();
        for (int i = 0; i < qLabels.length; i++) {
            if (qLabels[i].equals(l)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see VersionHistory#hasVersionLabel(Version, String)
     */
    public boolean hasVersionLabel(Version version, String label) throws VersionException, RepositoryException {
        // check-status performed within checkValidVersion
        checkValidVersion(version);
        String vUUID = version.getUUID();
        Name l = getQLabel(label);

        Name[] qLabels = getQLabels();
        for (int i = 0; i < qLabels.length; i++) {
            if (qLabels[i].equals(l)) {
                String uuid = getVersionByLabel(qLabels[i]).getUUID();
                return vUUID.equals(uuid);
            }
        }
        return false;
    }

    /**
     * @see VersionHistory#getVersionLabels()
     */
    public String[] getVersionLabels() throws RepositoryException {
        checkStatus();
        Name[] qLabels = getQLabels();
        String[] labels = new String[qLabels.length];

        for (int i = 0; i < qLabels.length; i++) {
            labels[i] = session.getNameResolver().getJCRName(qLabels[i]);
        }
        return labels;
    }

    /**
     * @see VersionHistory#getVersionLabels(Version)
     */
    public String[] getVersionLabels(Version version) throws VersionException, RepositoryException {
        // check-status performed within checkValidVersion
        checkValidVersion(version);
        String vUUID = version.getUUID();

        List<String> vlabels = new ArrayList<String>();
        Name[] qLabels = getQLabels();
        for (int i = 0; i < qLabels.length; i++) {
            String uuid = getVersionByLabel(qLabels[i]).getUUID();
            if (vUUID.equals(uuid)) {
                vlabels.add(session.getNameResolver().getJCRName(qLabels[i]));
            }
        }
        return vlabels.toArray(new String[vlabels.size()]);
    }

    /**
     * @see VersionHistory#removeVersion(String)
     */
    public void removeVersion(String versionName) throws ReferentialIntegrityException,
        AccessDeniedException, UnsupportedRepositoryOperationException,
        VersionException, RepositoryException {
        checkStatus();
        NodeState vState = getVersionState(versionName);
        session.getVersionStateManager().removeVersion((NodeState) getItemState(), vState);
    }

    /**
     * @see VersionHistory#getVersionableIdentifier()
     */
    public String getVersionableIdentifier() throws RepositoryException {
        checkStatus();
        return getProperty(NameConstants.JCR_VERSIONABLEUUID).getString();
    }

    //---------------------------------------------------------------< Item >---
    /**
     *
     * @param otherItem
     * @return
     * @see Item#isSame(Item)
     */
    @Override
    public boolean isSame(Item otherItem) throws RepositoryException {
        checkStatus();
        if (otherItem instanceof VersionHistoryImpl) {
            // since all version histories are referenceable, protected and live
            // in the same workspace, a simple comparison of the UUIDs is sufficient.
            VersionHistoryImpl other = ((VersionHistoryImpl) otherItem);
            return vhEntry.getUniqueID().equals(other.vhEntry.getUniqueID());
        }
        return false;
    }

    //-----------------------------------------------------------< ItemImpl >---
    /**
     *
     * @throws UnsupportedRepositoryOperationException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    @Override
    protected void checkIsWritable() throws UnsupportedRepositoryOperationException, ConstraintViolationException, RepositoryException {
        super.checkIsWritable();
        throw new ConstraintViolationException("VersionHistory is protected");
    }

    /**
     * Always returns false
     *
     * @throws RepositoryException
     * @see NodeImpl#isWritable()
     */
    @Override
    protected boolean isWritable() throws RepositoryException {
        super.isWritable();
        return false;
    }
    //------------------------------------------------------------< private >---
    /**
     *
     * @return
     */
    private Name[] getQLabels() throws RepositoryException {
        refreshEntry(labelNodeEntry);
        List<Name> labelNames = new ArrayList<Name>();
        for (Iterator<PropertyEntry> it = labelNodeEntry.getPropertyEntries(); it.hasNext(); ) {
            PropertyEntry pe = it.next();
            if (! NameConstants.JCR_PRIMARYTYPE.equals(pe.getName()) &&
                ! NameConstants.JCR_MIXINTYPES.equals(pe.getName())) {
                labelNames.add(pe.getName());
            }
        }
        return labelNames.toArray(new Name[labelNames.size()]);
    }

    /**
     *
     * @param versionName
     * @return
     * @throws VersionException
     * @throws RepositoryException
     */
    private NodeState getVersionState(String versionName) throws VersionException, RepositoryException {
        try {
            Name vName = session.getNameResolver().getQName(versionName);
            refreshEntry(vhEntry);
            NodeEntry vEntry = vhEntry.getNodeEntry(vName, Path.INDEX_DEFAULT, true);
            if (vEntry == null) {
                throw new VersionException("Version '" + versionName + "' does not exist in this version history.");
            } else {
                return vEntry.getNodeState();
            }
        } catch (org.apache.jackrabbit.spi.commons.conversion.NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     *
     * @param qLabel
     * @return
     * @throws VersionException
     * @throws RepositoryException
     */
    private Version getVersionByLabel(Name qLabel) throws VersionException, RepositoryException {
         refreshEntry(labelNodeEntry);
        // retrieve reference property value -> and retrieve referenced node
        PropertyEntry pEntry = labelNodeEntry.getPropertyEntry(qLabel, true);
        if (pEntry == null) {
            throw new VersionException("Version with label '" + qLabel + "' does not exist.");
        }
        Node version = ((Property) getItemManager().getItem(pEntry)).getNode();
        return (Version) version;
    }

    /**
     *
     * @param label
     * @return
     * @throws RepositoryException
     */
    private Name getQLabel(String label) throws RepositoryException {
        try {
            return session.getNameResolver().getQName(label);
        } catch (NameException e) {
            String error = "Invalid version label: " + e.getMessage();
            log.error(error);
            throw new RepositoryException(error, e);
        }
    }

    /**
     * Checks if the specified version belongs to this <code>VersionHistory</code>.
     * This method throws <code>VersionException</code> if {@link Version#getContainingHistory()}
     * is not the same item than this <code>VersionHistory</code>.
     *
     * @param version
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.RepositoryException
     */
    private void checkValidVersion(Version version) throws VersionException, RepositoryException {
        if (!version.getContainingHistory().isSame(this)) {
            throw new VersionException("Specified version '" + version.getName() + "' is not part of this history.");
        }
    }

    /**
     *
     * @param entry
     * @throws RepositoryException
     */
    private static void refreshEntry(NodeEntry entry) throws RepositoryException {
        // TODO: check again.. is this correct? or should NodeEntry be altered
        entry.getNodeState();
    }
}