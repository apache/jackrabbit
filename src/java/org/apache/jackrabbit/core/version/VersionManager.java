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

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.Iterator;

/**
 * This interface defines the version manager. It gives access to the underlaying
 * persistence layer of the versioning.
 */
public interface VersionManager {
    /**
     * root path for version storage
     */
    public static final QName NODENAME_HISTORY_ROOT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionStorage");
    /**
     * the name of the frozen node
     */
    public static final QName NODENAME_FROZEN = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenNode");
    /**
     * the name of the version labels node
     */
    public static final QName NODENAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:frozenUuid' property
     */
    public static final QName PROPNAME_FROZEN_UUID = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenUuid");
    /**
     * name of the 'jcr:frozenPrimaryType' property
     */
    public static final QName PROPNAME_FROZEN_PRIMARY_TYPE = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenPrimaryType");
    /**
     * name of the 'jcr:frozenMixinTypes' property
     */
    public static final QName PROPNAME_FROZEN_MIXIN_TYPES = new QName(NamespaceRegistryImpl.NS_JCR_URI, "frozenMixinTypes");
    /**
     * name of the 'jcr:predecessors' property
     */
    public static final QName PROPNAME_PREDECESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "predecessors");
    /**
     * name of the 'jcr:versionLabels' property
     */
    public static final QName PROPNAME_VERSION_LABELS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionLabels");
    /**
     * name of the 'jcr:successors' property
     */
    public static final QName PROPNAME_SUCCESSORS = new QName(NamespaceRegistryImpl.NS_JCR_URI, "successors");
    /**
     * name of the 'jcr:isCheckedOut' property
     */
    public static final QName PROPNAME_IS_CHECKED_OUT = new QName(NamespaceRegistryImpl.NS_JCR_URI, "isCheckedOut");
    /**
     * name of the 'jcr:versionHistory' property
     */
    public static final QName PROPNAME_VERSION_HISTORY = new QName(NamespaceRegistryImpl.NS_JCR_URI, "versionHistory");
    /**
     * name of the 'jcr:baseVersion' property
     */
    public static final QName PROPNAME_BASE_VERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "baseVersion");
    /**
     * name of the 'jcr:child' property
     */
    public static final QName PROPNAME_CHILD = new QName(NamespaceRegistryImpl.NS_JCR_URI, "child");
    /**
     * name of the 'jcr:created' property
     */
    public static final QName PROPNAME_CREATED = new QName(NamespaceRegistryImpl.NS_JCR_URI, "created");
    /**
     * the name of the 'jcr:rootVersion' node
     */
    public static final QName NODENAME_ROOTVERSION = new QName(NamespaceRegistryImpl.NS_JCR_URI, "rootVersion");

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public VirtualItemStateProvider getVirtualItemStateProvider(SessionImpl session,
                                                                ItemStateManager base);

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionalbe' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node) throws RepositoryException;

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version checkin(NodeImpl node) throws RepositoryException;

    //-----------------------------------------------------< internal stuff >---

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersionHistory(String id);

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String id) throws RepositoryException;

    /**
     * Returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException;

    /**
     * Returns an iterator over all ids of {@link InternalVersionHistory}s.
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException;

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersion(String id);

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String id) throws RepositoryException;

    /**
     * checks, if the node with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasItem(String id);

    /**
     * Returns the version item with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItem(String id) throws RepositoryException;

}
