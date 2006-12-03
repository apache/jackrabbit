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
package org.apache.jackrabbit.test.rmi;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteItemDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRow;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.easymock.MockControl;

/**
 * Tests for the adapter classes of JCR-RMI. These tests use reflection
 * to invoke all methods of an adapter object and to verify that the
 * corresponding methods of the underlying object get called by the adapter.
 */
public class RemoteAdapterTest extends TestCase {

    /** Factory for creating remote test adapters. */
    private RemoteAdapterFactory remoteFactory;

    /** Factory for creating local test adapters. */
    private LocalAdapterFactory localFactory;

    /** Map of method names to method descriptions. */
    private Map methods;

    /** Mock controller. */
    private MockControl control;

    /** Mock object. */
    private Object mock;

    /**
     * Prepares the automated test suite to adapters of the given interface.
     *
     * @param iface adapter interface
     * @throws Exception on errors
     */
    private void prepareTests(Class iface) throws Exception {
        remoteFactory = new ServerAdapterFactory();
        localFactory = new ClientAdapterFactory();

        methods = new HashMap();
        Method[] m = iface.getDeclaredMethods();
        for (int i = 0; i < m.length; i++) {
            methods.put(m[i].getName(), m[i]);
        }

        control = MockControl.createControl(iface);
        mock = control.getMock();
    }

    /**
     * Removes the identified method from the automatic tests.
     *
     * @param name method name
     */
    private void ignoreMethod(String name) {
        methods.remove(name);
    }

    /**
     * Returns a parameter array for the given method.
     *
     * @param method method description
     * @return parameter array
     */
    private Object[] getParameters(Method method) {
        Class[] types = method.getParameterTypes();
        Object[] parameters = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (!types[i].isPrimitive()) {
                parameters[i] = null;
            } else if ("int".equals(types[i].getName())) {
                parameters[i] = new Integer(0);
            } else if ("boolean".equals(types[i].getName())) {
                parameters[i] = new Boolean(false);
            } else {
                System.out.println(types[i].getName());
                parameters[i] = null;
            }
        }
        return parameters;
    }

    /**
     * Sets the expected return value for the given method.
     *
     * @param method method description
     * @param control mock controller
     */
    private void setReturnValue(Method method, MockControl control) {
        Class type = method.getReturnType();
        if (!type.isPrimitive()) {
            control.setReturnValue(null);
        } else if ("void".equals(type.getName())) {
            control.setVoidCallable();
        } else if ("int".equals(type.getName())) {
            control.setReturnValue((int) 0);
        } else if ("long".equals(type.getName())) {
            control.setReturnValue((long) 0);
        } else if ("boolean".equals(type.getName())) {
            control.setReturnValue(false);
        } else {
            System.out.println(type.getName());
            control.setReturnValue(null);
        }
    }

    /**
     * Runs the automatic test suite on the given adapter instance.
     *
     * @param adapter adapter instance
     * @throws Exception on errors
     */
    private void runTests(Object adapter) throws Exception {
        Iterator iterator = methods.values().iterator();
        while (iterator.hasNext()) {
            Method method = (Method) iterator.next();
            Object[] parameters = getParameters(method);

            method.invoke(mock, parameters);
            setReturnValue(method, control);
            control.replay();

            method.invoke(adapter, parameters);
            control.verify();

            control.reset();
        }
    }

    /**
     * Tests Repository adapters.
     *
     * @throws Exception on errors
     */
    public void testRepository() throws Exception {
        prepareTests(Repository.class);

        Repository repository = (Repository) mock;
        RemoteRepository remote = remoteFactory.getRemoteRepository(repository);
        Repository local = localFactory.getRepository(remote);

        runTests(local);
    }

    /**
     * Tests Session adapters.
     *
     * @throws Exception on errors
     */
    public void testSession() throws Exception {
        prepareTests(Session.class);
        ignoreMethod("getRepository");           // implemented locally
        ignoreMethod("importXML");               // wrapped stream
        ignoreMethod("getImportContentHandler"); // implemented locally
        ignoreMethod("exportSystemView");        // multiple methods
        ignoreMethod("exportDocumentView");      // multiple method
        ignoreMethod("getValueFactory");         // implemented locally
        ignoreMethod("logout");                  // local live flag
        ignoreMethod("isLive");                  // local live flag

        Session session = (Session) mock;
        RemoteSession remote = remoteFactory.getRemoteSession(session);
        Session local = localFactory.getSession(null, remote);

        runTests(local);
    }

    /**
     * Tests Item adapters.
     *
     * @throws Exception on errors
     */
    public void testItem() throws Exception {
        prepareTests(Item.class);
        ignoreMethod("accept");     // implemented in subclasses
        ignoreMethod("getSession"); // implemented locally
        ignoreMethod("isNode");     // implemented in subclasses
        ignoreMethod("isSame");     // implemented locally

        Item item = (Item) mock;
        RemoteItem remote = remoteFactory.getRemoteItem(item);
        Item local = localFactory.getItem(null, remote);

        runTests(local);
    }

    /**
     * Tests Node adapters.
     *
     * @throws Exception on errors
     */
    public void testNode() throws Exception {
        prepareTests(Node.class);
        ignoreMethod("cancelMerge");              // TODO
        ignoreMethod("doneMerge");                // TODO
        ignoreMethod("checkin");                  // TODO
        ignoreMethod("restore");                  // multiple methods
        ignoreMethod("getVersionHistory");        // TODO
        ignoreMethod("getBaseVersion");           // TODO
        ignoreMethod("setProperty");              // multiple methods
        ignoreMethod("getNodes");                 // null iterator
        ignoreMethod("getProperties");            // null iterator
        ignoreMethod("getReferences");            // null iterator
        ignoreMethod("merge");                    // null iterator

        Node node = (Node) mock;
        RemoteNode remote = remoteFactory.getRemoteNode(node);
        Node local = localFactory.getNode(null, remote);

        runTests(local);
    }

    /**
     * Tests Property adapters.
     *
     * @throws Exception on errors
     */
    public void testProperty() throws Exception {
        prepareTests(Property.class);
        ignoreMethod("getBoolean");  // implemented locally
        ignoreMethod("getLong");     // implemented locally
        ignoreMethod("getDouble");   // implemented locally
        ignoreMethod("getDate");     // implemented locally
        ignoreMethod("getString");   // implemented locally
        ignoreMethod("getStream");   // implemented locally
        ignoreMethod("getNode");     // implemented locally
        ignoreMethod("setValue");    // multiple methods
        ignoreMethod("getValue");    // no null values for SerialValueFactory

        Property property = (Property) mock;
        RemoteProperty remote = remoteFactory.getRemoteProperty(property);
        Property local = localFactory.getProperty(null, remote);

        runTests(local);
    }

    /**
     * Tests Lock adapters.
     *
     * @throws Exception on errors
     */
    public void testLock() throws Exception {
        prepareTests(Lock.class);
        ignoreMethod("getNode"); // implemented locally

        Lock lock = (Lock) mock;
        RemoteLock remote = remoteFactory.getRemoteLock(lock);
        Lock local = localFactory.getLock(null, remote);

        runTests(local);
    }

    /**
     * Tests Workspace adapters.
     *
     * @throws Exception on errors
     */
    public void testWorkspace() throws Exception {
        prepareTests(Workspace.class);
        ignoreMethod("getObservationManager");   // TODO
        ignoreMethod("restore");                 // TODO
        ignoreMethod("getSession");              // implemented locally
        ignoreMethod("copy");                    // multiple methods
        ignoreMethod("importXML");               // wrapped stream
        ignoreMethod("getImportContentHandler"); // implemented locally

        Workspace workspace = (Workspace) mock;
        RemoteWorkspace remote = remoteFactory.getRemoteWorkspace(workspace);
        Workspace local = localFactory.getWorkspace(null, remote);

        runTests(local);
    }

    /**
     * Tests NamespaceRegistry adapters.
     *
     * @throws Exception on errors
     */
    public void testNamespaceRegistry() throws Exception {
        prepareTests(NamespaceRegistry.class);

        NamespaceRegistry registry = (NamespaceRegistry) mock;
        RemoteNamespaceRegistry remote =
            remoteFactory.getRemoteNamespaceRegistry(registry);
        NamespaceRegistry local = localFactory.getNamespaceRegistry(remote);

        runTests(local);
    }

    /**
     * Tests NodeTypeManager adapters.
     *
     * @throws Exception on errors
     */
    public void testNodeTypeManager() throws Exception {
        prepareTests(NodeTypeManager.class);
        ignoreMethod("getAllNodeTypes");     // null iterator
        ignoreMethod("getPrimaryNodeTypes"); // null iterator
        ignoreMethod("getMixinNodeTypes");   // null iterator

        NodeTypeManager manager = (NodeTypeManager) mock;
        RemoteNodeTypeManager remote =
            remoteFactory.getRemoteNodeTypeManager(manager);
        NodeTypeManager local = localFactory.getNodeTypeManager(remote);

        runTests(local);
    }

    /**
     * Tests NodeType adapters.
     *
     * @throws Exception on errors
     */
    public void testNodeType() throws Exception {
        prepareTests(NodeType.class);
        ignoreMethod("canSetProperty"); // wrapped Value object

        NodeType type = (NodeType) mock;
        RemoteNodeType remote = remoteFactory.getRemoteNodeType(type);
        NodeType local = localFactory.getNodeType(remote);

        runTests(local);
    }

    /**
     * Tests ItemDef adapters.
     *
     * @throws Exception on errors
     */
    public void testItemDef() throws Exception {
        prepareTests(ItemDefinition.class);

        ItemDefinition def = (ItemDefinition) mock;
        RemoteItemDefinition remote = remoteFactory.getRemoteItemDefinition(def);
        ItemDefinition local = localFactory.getItemDef(remote);

        runTests(local);
    }

    /**
     * Tests NodeDef adapters.
     *
     * @throws Exception on errors
     */
    public void testNodeDef() throws Exception {
        prepareTests(NodeDefinition.class);

        NodeDefinition def = (NodeDefinition) mock;
        RemoteNodeDefinition remote = remoteFactory.getRemoteNodeDefinition(def);
        NodeDefinition local = localFactory.getNodeDef(remote);

        runTests(local);
    }

    /**
     * Tests PropertyDef adapters.
     *
     * @throws Exception on errors
     */
    public void testPropertyDef() throws Exception {
        prepareTests(PropertyDefinition.class);

        PropertyDefinition def = (PropertyDefinition) mock;
        RemotePropertyDefinition remote = remoteFactory.getRemotePropertyDefinition(def);
        PropertyDefinition local = localFactory.getPropertyDef(remote);

        runTests(local);
    }

    /**
     * Tests QueryManager adapters.
     *
     * @throws Exception on errors
     */
    public void testQueryManager() throws Exception {
        prepareTests(QueryManager.class);
        ignoreMethod("getQuery");   // TODO

        QueryManager manager = (QueryManager) mock;
        RemoteQueryManager remote = remoteFactory.getRemoteQueryManager(manager);
        QueryManager local = localFactory.getQueryManager(null, remote);

        runTests(local);
    }

    /**
     * Tests Query adapters.
     *
     * @throws Exception on errors
     */
    public void testQuery() throws Exception {
        prepareTests(Query.class);

        Query query = (Query) mock;
        RemoteQuery remote = remoteFactory.getRemoteQuery(query);
        Query local = localFactory.getQuery(null, remote);

        runTests(local);
    }

    /**
     * Tests QueryResult adapters.
     *
     * @throws Exception on errors
     */
    public void testQueryResult() throws Exception {
        prepareTests(QueryResult.class);
        ignoreMethod("getNodes"); // null iterator
        ignoreMethod("getRows");  // null iterator

        QueryResult result = (QueryResult) mock;
        RemoteQueryResult remote = remoteFactory.getRemoteQueryResult(result);
        QueryResult local = localFactory.getQueryResult(null, remote);

        runTests(local);
    }

    /**
     * Tests Row adapters.
     *
     * @throws Exception on errors
     */
    public void testRow() throws Exception {
        prepareTests(Row.class);
        ignoreMethod("getValue");    // no null values for SerialValueFactory

        Row row = (Row) mock;
        RemoteRow remote = remoteFactory.getRemoteRow(row);
        Row local = localFactory.getRow(remote);

        runTests(local);
    }

    /**
     * Tests Version adapters.
     *
     * @throws Exception on errors
     */
    public void testVersion() throws Exception {
        prepareTests(Version.class);

        Version version = (Version) mock;
        RemoteVersion remote = remoteFactory.getRemoteVersion(version);
        Version local = localFactory.getVersion(null, remote);

        runTests(local);
    }

    /**
     * Tests VersionHistory adapters.
     *
     * @throws Exception on errors
     */
    public void testVersionHistory() throws Exception {
        prepareTests(VersionHistory.class);
        ignoreMethod("getVersionLabels"); // UUID call
        ignoreMethod("hasVersionLabel");  // UUID call
        ignoreMethod("getAllVersions");   // null iterator

        VersionHistory history = (VersionHistory) mock;
        RemoteVersionHistory remote =
            remoteFactory.getRemoteVersionHistory(history);
        VersionHistory local = localFactory.getVersionHistory(null, remote);

        runTests(local);
    }

}
