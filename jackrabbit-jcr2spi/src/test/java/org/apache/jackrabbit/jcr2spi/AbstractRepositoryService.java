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
package org.apache.jackrabbit.jcr2spi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.NamespaceException;
import javax.jcr.RangeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Subscription;
import org.apache.jackrabbit.spi.commons.SessionInfoImpl;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.spi.commons.nodetype.compact.ParseException;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilderImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * This {@link RepositoryService} implementation can serve as a starting point
 * for mock implementation for testing purposes.
 */
public abstract class AbstractRepositoryService implements RepositoryService {
    private final NamespaceMapping namespaces = new NamespaceMapping();
    private final NodeTypeRegistry nodeTypes = new NodeTypeRegistry();

    public AbstractRepositoryService() throws RepositoryException {
        super();
        registerNodeTypes();
    }

    protected void registerNodeTypes() throws RepositoryException {
        String resourceName = "default-nodetypes.cnd";
        InputStream is = AbstractRepositoryService.class.getResourceAsStream(resourceName);
        if (is == null) {
            throw new RepositoryException(("Resource not found: " + resourceName));
        }

        Reader reader = new InputStreamReader(new BufferedInputStream(is));
        try {
            CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(reader,
                    "spi2mock", namespaces, new QNodeTypeDefinitionsBuilderImpl());

            for (Iterator ntDefs = cndReader.getNodeTypeDefs().iterator(); ntDefs.hasNext(); ) {
                nodeTypes.register((QNodeTypeDefinition) ntDefs.next());
            }
        }
        catch (ParseException e) {
            throw new RepositoryException(e);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                throw new RepositoryException("Error closing stream", e);
            }
        }
    }


    // -----------------------------------------------------< RepositoryService >---

    public IdFactory getIdFactory() throws RepositoryException {
        return IdFactoryImpl.getInstance();
    }

    public NameFactory getNameFactory() throws RepositoryException {
        return NameFactoryImpl.getInstance();
    }

    public PathFactory getPathFactory() throws RepositoryException {
        return PathFactoryImpl.getInstance();
    }

    public QValueFactory getQValueFactory() throws RepositoryException {
        return QValueFactoryImpl.getInstance();
    }

    public Map getRepositoryDescriptors() throws RepositoryException {
        Map descriptorKeys = new HashMap();

        descriptorKeys.put(Repository.LEVEL_1_SUPPORTED, Boolean.TRUE.toString());
        descriptorKeys.put(Repository.LEVEL_2_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.OPTION_LOCKING_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.OPTION_OBSERVATION_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.OPTION_QUERY_SQL_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.OPTION_VERSIONING_SUPPORTED, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.QUERY_XPATH_DOC_ORDER, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.QUERY_XPATH_POS_INDEX, Boolean.FALSE.toString());
        descriptorKeys.put(Repository.REP_NAME_DESC, "Mock Repository");
        descriptorKeys.put(Repository.REP_VENDOR_DESC, "Apache Software Foundation");
        descriptorKeys.put(Repository.REP_VENDOR_URL_DESC, "http://www.apache.org/");
        descriptorKeys.put(Repository.REP_VERSION_DESC, "1.0");
        descriptorKeys.put(Repository.SPEC_NAME_DESC, "Content Repository API for Java(TM) Technology Specification");
        descriptorKeys.put(Repository.SPEC_VERSION_DESC, "1.0");

        return descriptorKeys;
    }


    //-----------------------------------< SessionInfo creation and release >---

    public SessionInfo obtain(Credentials credentials, String workspaceName) throws RepositoryException {
        SessionInfoImpl si = new SessionInfoImpl();
        si.setWorkspacename(workspaceName);
        if (credentials instanceof SimpleCredentials) {
            si.setUserID(((SimpleCredentials) credentials).getUserID());
        }

        return si;
    }

    public SessionInfo obtain(SessionInfo sessionInfo, String workspaceName) throws RepositoryException {
        SessionInfoImpl si = new SessionInfoImpl();
        si.setWorkspacename(workspaceName);
        if (sessionInfo instanceof SessionImpl) {
            si.setUserID(((SessionImpl) sessionInfo).getUserID());
        }
        return si;
    }

    public SessionInfo impersonate(SessionInfo sessionInfo, Credentials credentials)
            throws RepositoryException {

        SessionInfoImpl si = new SessionInfoImpl();
        if (credentials instanceof SimpleCredentials) {
            si.setUserID(((SimpleCredentials) credentials).getUserID());
        }
        if (sessionInfo instanceof SessionImpl) {
            si.setUserID(((SessionImpl) sessionInfo).getUserID());
        }

        return si;
    }

    public void dispose(SessionInfo sessionInfo) throws RepositoryException {
        // empty
    }

    public String[] getWorkspaceNames(SessionInfo sessionInfo) throws RepositoryException {
        return new String [] { "default" };
    }


    //-----------------------------------------------------< Access Control >---

    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions)
            throws RepositoryException {

        return true;
    }


    //------------------------------------------------------< Reading items >---

    public QNodeDefinition getNodeDefinition(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {

        QNodeTypeDefinition ntd = nodeTypes.getQNodeTypeDefinition(NameConstants.NT_UNSTRUCTURED);
        QNodeDefinition[] nds = ntd.getChildNodeDefs();
        for (int k = 0; k < nds.length; k++) {
            QNodeDefinition nd = nds[k];
            if (NameConstants.ANY_NAME.equals(nd.getName())) {
                return nd;
            }
        }

        throw new IllegalStateException(("No node definition for " + nodeId));
    }

    public QPropertyDefinition getPropertyDefinition(SessionInfo sessionInfo, PropertyId propertyId)
            throws RepositoryException {

        QNodeTypeDefinition ntd = nodeTypes.getQNodeTypeDefinition(NameConstants.NT_UNSTRUCTURED);
        QPropertyDefinition[] pds = ntd.getPropertyDefs();
        for (int k = 0; k < pds.length; k++) {
            QPropertyDefinition pd = pds[k];
            if (NameConstants.ANY_NAME.equals(pd)) {
                return pd;
            }
        }

        throw new IllegalStateException(("No property definition for " + propertyId));
    }

    public abstract NodeInfo getNodeInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    public abstract Iterator getItemInfos(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException;

    public abstract Iterator getChildInfos(SessionInfo sessionInfo, NodeId parentId) throws RepositoryException;

    public abstract PropertyInfo getPropertyInfo(SessionInfo sessionInfo, PropertyId propertyId);

    //-----------------------------------------------< general modification >---

    public Batch createBatch(SessionInfo sessionInfo, ItemId itemId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }

    public void submit(Batch batch) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }


    //-------------------------------------------------------------< Import >---

    public void importXml(SessionInfo sessionInfo, NodeId parentId, InputStream xmlStream, int uuidBehaviour)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }


    //---------------------------------------------------------< Copy, Move >---

    public void move(SessionInfo sessionInfo, NodeId srcNodeId, NodeId destParentNodeId, Name destName)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }

    public void copy(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId,
            NodeId destParentNodeId, Name destName) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }


    //------------------------------------------------------< Update, Clone >---

    public void update(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }

    public void clone(SessionInfo sessionInfo, String srcWorkspaceName, NodeId srcNodeId,
            NodeId destParentNodeId, Name destName, boolean removeExisting) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("JCR Level 2 not supported");
    }


    //------------------------------------------------------------< Locking >---

    public LockInfo getLockInfo(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking not supported");
    }

    public LockInfo lock(SessionInfo sessionInfo, NodeId nodeId, boolean deep, boolean sessionScoped)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Locking not supported");
    }

    public LockInfo lock(
            SessionInfo sessionInfo, NodeId nodeId,
            boolean deep, boolean sessionScoped,
            long timeoutHint, String ownerHint)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Locking not supported");
    }

    public void refreshLock(SessionInfo sessionInfo, NodeId nodeId)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Locking not supported");
    }

    public void unlock(SessionInfo sessionInfo, NodeId nodeId)
            throws  RepositoryException {

        throw new UnsupportedRepositoryOperationException("Locking not supported");
    }


    //---------------------------------------------------------< Versioning >---

    public NodeId checkin(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void checkout(SessionInfo sessionInfo, NodeId nodeId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void removeVersion(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void restore(SessionInfo sessionInfo, NodeId[] versionIds, boolean removeExisting)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void restore(SessionInfo sessionInfo, NodeId nodeId, NodeId versionId, boolean removeExisting)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public Iterator merge(SessionInfo sessionInfo, NodeId nodeId, String srcWorkspaceName, boolean bestEffort)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void resolveMergeConflict(SessionInfo sessionInfo, NodeId nodeId, NodeId[] mergeFailedIds,
            NodeId[] predecessorIds) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void addVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label, boolean moveLabel) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }

    public void removeVersionLabel(SessionInfo sessionInfo, NodeId versionHistoryId, NodeId versionId,
            Name label) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Versioning not supported");
    }


    //----------------------------------------------------------< Searching >---

    public String[] getSupportedQueryLanguages(SessionInfo sessionInfo) throws RepositoryException {
        return new String[] {};
    }


    public void checkQueryStatement(SessionInfo sessionInfo, String statement, String language,
            Map namespaces) throws RepositoryException {

        // empty
    }

    public QueryInfo executeQuery(SessionInfo sessionInfo, String statement, String language,
            Map namespaces) throws RepositoryException {

        return new QueryInfo() {
            public Name[] getColumnNames() {
                return new Name[] {};
            }

            public RangeIterator getRows() {
                return new RangeIteratorAdapter(EmptyIterator.INSTANCE);
            }
        };
    }


    //--------------------------------------------------------< Observation >---

    public EventFilter createEventFilter(SessionInfo sessionInfo, int eventTypes, Path absPath,
            boolean isDeep, String[] uuid, Name[] nodeTypeName, boolean noLocal) throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Observation not supported");
    }

    public Subscription createSubscription(SessionInfo sessionInfo, EventFilter[] filters)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Observation not supported");
    }

    public void updateEventFilters(Subscription subscription, EventFilter[] filters)
            throws RepositoryException {

        throw new UnsupportedRepositoryOperationException("Observation not supported");
    }

    public EventBundle[] getEvents(Subscription subscription, long timeout) throws RepositoryException,
            InterruptedException {

        throw new UnsupportedRepositoryOperationException("Observation not supported");
    }

    public void dispose(Subscription subscription) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Observation not supported");
    }


    //---------------------------------------------------------< Namespaces >---

    public Map getRegisteredNamespaces(SessionInfo sessionInfo) throws RepositoryException {
        return namespaces.getPrefixToURIMapping();
    }

    public String getNamespaceURI(SessionInfo sessionInfo, String prefix)
            throws RepositoryException {

        return namespaces.getURI(prefix);
    }

    public String getNamespacePrefix(SessionInfo sessionInfo, String uri)
            throws RepositoryException {

        return namespaces.getPrefix(uri);
    }

    public void registerNamespace(SessionInfo sessionInfo, String prefix, String uri)
            throws RepositoryException {

        throw new NamespaceException("Cannot register " + uri);
    }

    public void unregisterNamespace(SessionInfo sessionInfo, String uri) throws RepositoryException {
        throw new NamespaceException("Cannot register " + uri);
    }


    //----------------------------------------------------------< NodeTypes >---

    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo) throws RepositoryException {
        return new ObjectArrayIterator(nodeTypes.getQNodeTypeDefinitions());
    }

    public Iterator getQNodeTypeDefinitions(SessionInfo sessionInfo, Name[] nodeTypeNames)
            throws RepositoryException {

        return new ObjectArrayIterator(nodeTypes.getQNodeTypeDefinitions(nodeTypeNames));
    }

}

/**
 * Registry for {@link QNodeTypeDefinition}s. Registered node type definitions can be retrieved by
 * its name.
 */
class NodeTypeRegistry {
    private final Map definitions = new HashMap();

    /**
     * Register the given {@link QNodeTypeDefinition} by its name. Registering a node type
     * definition with a name already in use effectively removes the old definition from the
     * registry.
     *
     * @param nodeTypeDef
     * @throws IllegalArgumentException if <code>nodeTypeDef</code> is null
     */
    public void register(QNodeTypeDefinition nodeTypeDef)  {
        if (nodeTypeDef == null) {
            throw new IllegalArgumentException("Cannot register null");
        }

        definitions.put(nodeTypeDef.getName(), nodeTypeDef);
    }

    /**
     * Returns an array of {@link QNodeTypeDefinition}s for the given <code>names</code>.
     *
     * @param names Names of the node type definition to look up. If <code>null</code> is passed,
     *                all registered node types are returned.
     * @return
     * @throws RepositoryException If no node type definition is registered for any name in
     *                 <code>names</code>.
     */
    public QNodeTypeDefinition[] getQNodeTypeDefinitions(Name[] names) throws RepositoryException {
        if (names == null) {
            return getQNodeTypeDefinitions();
        }

        List defs = new LinkedList();
        for (int k = 0; k < names.length; k++) {
            Object def = definitions.get(names[k]);
            if (def == null) {
                throw new RepositoryException(("No such node type definition: " + names[k]));
            }

            defs.add(def);
        }

        return (QNodeTypeDefinition[]) defs.toArray(new QNodeTypeDefinition[defs.size()]);
    }

    /**
     * Returns the {@link QNodeTypeDefinition} registered with the given name or null if none is
     * registered.
     *
     * @param name
     * @return
     */
    public QNodeTypeDefinition getQNodeTypeDefinition(Name name) {
        return (QNodeTypeDefinition) definitions.get(name);
    }

    /**
     * @return array of all registered {@link QNodeTypeDefinition}s.
     */
    public QNodeTypeDefinition[] getQNodeTypeDefinitions() {
        return (QNodeTypeDefinition[]) definitions.values().toArray(new QNodeTypeDefinition[0]);
    }

}

