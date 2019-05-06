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
package org.apache.jackrabbit.spi.commons;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.SessionInfo;

/**
 * <code>AbstractReadableRepositoryService</code> provides an abstract base
 * class where all methods that attempt to write throw an
 * {@link UnsupportedRepositoryOperationException}. This class useful for
 * repository service implementation that only provide read access to the
 * underlying content.
 */
public abstract class AbstractReadableRepositoryService extends AbstractRepositoryService {

    protected static final Set<String> WRITE_ACTIONS = new HashSet<String>(
            Arrays.asList("add_node", "set_property", "remove"));

    /**
     * The list of workspaces that this repository service exposes.
     */
    protected final List<String> wspNames;

    /**
     * The name of the default workspace
     */
    protected final String defaulWsp;

    /**
     * Creates a new <code>AbstractReadableRepositoryService</code>.
     *
     * @param descriptors the repository descriptors. Maps descriptor keys to
     *                    descriptor values.
     * @param namespaces  the namespaces. Maps namespace prefixes to namespace
     *                    URIs.
     * @param cnd         a reader on the compact node type definition.
     * @param wspNames    a list of workspace names.
     * @param defaultWsp  name of the default workspace
     * @throws RepositoryException       if the namespace mappings are invalid.
     * @throws ParseException            if an error occurs while parsing the CND.
     * @throws IllegalArgumentException  if <code>defaultWsp</code> is <code>null</code>
     */
    public AbstractReadableRepositoryService(Map<String, QValue[]> descriptors,
                                             Map<String, String> namespaces,
                                             Reader cnd,
                                             List<String> wspNames,
                                             String defaultWsp)
            throws RepositoryException, ParseException, IllegalArgumentException {

        super(descriptors, namespaces, cnd);

        if (defaultWsp == null) {
            throw new IllegalArgumentException("Default workspace is null");
        }

        this.wspNames = Collections.unmodifiableList(new ArrayList<String>(wspNames));
        this.defaulWsp = defaultWsp;
    }

    //------------------------------------< may be overwritten by subclasses>---
    /**
     * Checks whether the <code>workspaceName</code> is valid.
     * @param workspaceName  name of the workspace to check
     * @throws NoSuchWorkspaceException  if <code>workspaceName</code> is neither in the
     * list of workspace nor null (i.e. default workspace).
     */
    @Override
    protected void checkWorkspace(String workspaceName) throws NoSuchWorkspaceException {
        if (workspaceName != null && !wspNames.contains(workspaceName)) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
    }

    @Override
    protected SessionInfo createSessionInfo(Credentials credentials, String workspaceName)
            throws RepositoryException {

        return super.createSessionInfo(credentials, workspaceName == null? defaulWsp : workspaceName);
    }

    // -------------------------------------------------------------< cache >---
    /**
     * @param sessionInfo
     * @return a new instance of <code>ItemInfoCacheImpl</code>
     */
    public ItemInfoCache getItemInfoCache(SessionInfo sessionInfo) {
        return new ItemInfoCacheImpl();
    }

    //------------------------------------------------------------< reading >---
    /**
     * This default implementation returns the first item returned by the call to
     * {@link #getItemInfos(SessionInfo, ItemId)}. The underlying assumption here is that
     * the implementation and the persistence layer are optimized for batch reading. That is,
     * a call to <code>getItemInfos</code> is no more expensive than retrieving the single
     * <code>NodeInfo</code> only. If this assumption does not hold, subclasses should override
     * this method.
     */
    public NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws ItemNotFoundException,
            RepositoryException {

        Iterator<? extends ItemInfo> infos = getItemInfos(sessionInfo, nodeId);
        if (infos.hasNext()) {
            return (NodeInfo) infos.next();
        }
        else {
            throw new ItemNotFoundException();
        }
    }

    //----------------------------------------------------< workspace names >---
    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns the workspaces that were
     * passed to the constructor of this repository service.
     */
    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        return wspNames.toArray(new String[wspNames.size()]);
    }

    //-----------------------------------------------------< access control >---
    /**
     * This default implementation first calls {@link #checkSessionInfo(SessionInfo)}
     * with the <code>sessionInfo</code>, then returns <code>false</code> if
     * the any of the <code>actions</code> are in {@link #WRITE_ACTIONS};
     * otherwise returns <code>true</code>.
     */
    public boolean isGranted(SessionInfo sessionInfo,
                             ItemId itemId,
                             String[] actions) throws RepositoryException {
        checkSessionInfo(sessionInfo);
        // deny all but read
        for (String action : actions) {
            if (WRITE_ACTIONS.contains(action)) {
                return false;
            }
        }
        return true;
    }
}
