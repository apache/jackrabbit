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
package org.apache.jackrabbit.spi2dav;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.spi.AbstractSPITest;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryServiceStub;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.ordering.OrderingConstants;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.security.SecurityConstants;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;

/**
 * <code>DavPropertyTest</code>...
 */
public class DavPropertyTest extends AbstractSPITest implements ItemResourceConstants {

    private final String testPath = "/test";
    private RepositoryServiceImpl rs;
    private SessionInfo si;
    private NamePathResolver resolver;

    private String repoURI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        repoURI = helper.getProperty(ServiceStubImpl.PROP_REPOSITORY_URI);

        rs = (RepositoryServiceImpl) helper.getRepositoryService();
        si = helper.getAdminSessionInfo();

        NamespaceResolver nsResolver = new AbstractNamespaceResolver() {
            public String getURI(String prefix) {
                return ("jcr".equals(prefix)) ? "http://www.jcp.org/jcr/1.0" : prefix;
            }
            public String getPrefix(String uri) {
                return ("http://www.jcp.org/jcr/1.0".equals(uri)) ? "jcr" : uri;
            }
        };
        resolver = new DefaultNamePathResolver(nsResolver);

        try {
            rs.getNodeInfo(si, getNodeId(testPath));
        } catch (RepositoryException e) {
            Batch b = rs.createBatch(si, getNodeId("/"));
            b.addNode(getNodeId("/"), resolver.getQName("test"), NameConstants.NT_UNSTRUCTURED, null);
            QValueFactory qvf = rs.getQValueFactory();
            b.addProperty(getNodeId("/test"), resolver.getQName("prop"), qvf.create("value", PropertyType.STRING));
            b.addProperty(getNodeId("/test"), resolver.getQName("propMV"), new QValue[] {qvf.create(1), qvf.create(2)});
            rs.submit(b);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Batch b = rs.createBatch(si, getNodeId("/"));
            b.remove(getNodeId(testPath));
            rs.submit(b);
        } finally {
            rs.dispose(si);
            super.tearDown();
        }
    }

    private NodeId getNodeId(String path) throws RepositoryException {
        return rs.getIdFactory().createNodeId((String) null,
                resolver.getQPath(path));
    }

    private PropertyId getPropertyId(NodeId nodeID, Name propName) throws RepositoryException {
        return rs.getIdFactory().createPropertyId(nodeID, propName);
    }

    private static void assertPropertyNames(DavPropertyNameSet expected, DavPropertyNameSet result) {
        assertEquals(expected.getContentSize(), result.getContentSize());

        if (!(expected.getContent().containsAll(result.getContent()))) {
            StringBuilder missing = new StringBuilder();
            for (DavPropertyName name : expected.getContent()) {
                if (!result.contains(name)) {
                    missing.append("- ").append(name.toString()).append('\n');
                }
            }
            fail("Missing properties : \n" + missing);
        }
    }

    private DavPropertyNameSet doPropFindNames(String uri) throws Exception {
        HttpPropfind request = new HttpPropfind(uri, DavConstants.PROPFIND_PROPERTY_NAMES, DavConstants.DEPTH_0);
        HttpClient cl = rs.getClient(si);
        HttpResponse response = cl.execute(request, rs.getContext(si));
        request.checkSuccess(response);

        MultiStatus ms = request.getResponseBodyAsMultiStatus(response);
        assertEquals(1, ms.getResponses().length);
        return ms.getResponses()[0].getPropertyNames(HttpStatus.SC_OK);
    }

    private DavPropertyNameSet doPropFindAll(String uri) throws Exception {
        HttpPropfind request = new HttpPropfind(uri, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        HttpClient cl = rs.getClient(si);
        HttpResponse response = cl.execute(request, rs.getContext(si));
        request.checkSuccess(response);

        MultiStatus ms = request.getResponseBodyAsMultiStatus(response);
        assertEquals(1, ms.getResponses().length);
        return ms.getResponses()[0].getPropertyNames(HttpStatus.SC_OK);
    }

    private DavPropertyNameSet doPropFindByProp(String uri, DavPropertyNameSet props) throws Exception {
        HttpPropfind request = new HttpPropfind(uri, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        HttpClient cl = rs.getClient(si);
        HttpResponse response = cl.execute(request, rs.getContext(si));
        request.checkSuccess(response);

        MultiStatus ms = request.getResponseBodyAsMultiStatus(response);
        assertEquals(1, ms.getResponses().length);
        return ms.getResponses()[0].getPropertyNames(HttpStatus.SC_OK);
    }

    public void testRepositoryRoot() throws Exception {
        DavPropertyNameSet names = doPropFindNames(repoURI);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        /*
         Expected property names

         {DAV:}getlastmodified
         {DAV:}creator-displayname
         {DAV:}comment
         {DAV:}creationdate
         {DAV:}supported-report-set
         {DAV:}displayname
         {DAV:}supportedlock
         {http://www.day.com/jcr/webdav/1.0}workspaceName
         {DAV:}resourcetype
         {DAV:}lockdiscovery
         {DAV:}supported-method-set
         {DAV:}iscollection
         */
        assertPropertyNames(expected, names);

        DavPropertyNameSet all = doPropFindAll(repoURI);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {DAV:}creationdate
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {DAV:}iscollection
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        DavPropertyNameSet result = doPropFindByProp(repoURI, props);
        assertPropertyNames(props, result);
    }

    public void testWorkspace() throws Exception {
        StringBuilder uri = new StringBuilder(repoURI);
        uri.append("/").append(helper.getProperty(RepositoryServiceStub.PROP_PREFIX + "." +RepositoryServiceStub.PROP_WORKSPACE));

        DavPropertyNameSet set = doPropFindNames(uri.toString());
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(WORKSPACE_SET);

        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}workspace
        {DAV:}comment
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}supported-method-set
        {DAV:}iscollection
        {DAV:}creator-displayname
        {DAV:}creationdate
        {DAV:}supported-report-set
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {http://www.day.com/jcr/webdav/1.0}namespaces
        {http://www.day.com/jcr/webdav/1.0}nodetypes-cnd
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri.toString());
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(JCR_NODETYPES_CND);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {DAV:}creationdate
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}namespaces
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);        
        props.add(JCR_NODETYPES_CND);
        DavPropertyNameSet result = doPropFindByProp(uri.toString(), props);
        assertPropertyNames(props, result);

    }

    public void testProperty() throws Exception {
        String uri = rs.getItemUri(getPropertyId(getNodeId("/test"), resolver.getQName("prop")), si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(PROPERTY_SET);
        expected.add(JCR_PARENT);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}length
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {DAV:}supported-method-set
        {DAV:}iscollection
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}supported-report-set
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}path
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {http://www.day.com/jcr/webdav/1.0}type
        {http://www.day.com/jcr/webdav/1.0}value
         */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_LENGTH);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}type
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {http://www.day.com/jcr/webdav/1.0}value
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_LENGTH);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testMVProperty() throws Exception {
        String uri = rs.getItemUri(getPropertyId(getNodeId("/test"), resolver.getQName("propMV")), si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(PROPERTY_MV_SET);
        expected.add(JCR_PARENT);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}values
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {DAV:}supported-method-set
        {DAV:}iscollection
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}supported-report-set
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}path
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {http://www.day.com/jcr/webdav/1.0}type
        {http://www.day.com/jcr/webdav/1.0}lengths
         */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_LENGTHS);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}values
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}parent
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}supportedlock
        {DAV:}displayname
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}type
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_LENGTHS);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testRootNode() throws Exception {
        String uri = rs.getItemUri(getNodeId("/"), si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.add(OrderingConstants.ORDERING_TYPE);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}ordering-type
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(OrderingConstants.ORDERING_TYPE);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(OrderingConstants.ORDERING_TYPE);        
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testNode() throws Exception {
        String uri = rs.getItemUri(getNodeId("/test"), si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.add(OrderingConstants.ORDERING_TYPE);
        expected.add(JCR_PARENT);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}ordering-type
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(OrderingConstants.ORDERING_TYPE);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(OrderingConstants.ORDERING_TYPE);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testReferenceableNode() throws Exception {
        NodeId nid = getNodeId("/test");
        Batch b = rs.createBatch(si, nid);
        b.setMixins(nid, new Name[] {NameConstants.MIX_REFERENCEABLE});
        rs.submit(b);

        String uri = rs.getItemUri(nid, si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.add(OrderingConstants.ORDERING_TYPE);
        expected.add(JCR_PARENT);
        expected.add(JCR_UUID);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}ordering-type
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}uuid
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(OrderingConstants.ORDERING_TYPE);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_UUID);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(OrderingConstants.ORDERING_TYPE);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_UUID);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testNodeWithPrimaryItem() throws Exception {
        // create file node
        NodeId nid = getNodeId("/test");
        Name fileName = resolver.getQName("test.txt");
        Batch b = rs.createBatch(si, nid);
        b.addNode(nid, fileName, NameConstants.NT_FILE, null);

        String filePath = testPath + "/" + fileName.getLocalName();
        NodeId fileID = getNodeId(filePath);
        b.addNode(fileID, NameConstants.JCR_CONTENT, NameConstants.NT_RESOURCE, null);

        NodeId content = getNodeId(filePath + "/" + NameConstants.JCR_CONTENT);

        QValue lastModified = rs.getQValueFactory().create(Calendar.getInstance());
        QValue mimeType = rs.getQValueFactory().create("text/plain", PropertyType.STRING);
        QValue enc = rs.getQValueFactory().create("utf-8", PropertyType.STRING);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_LASTMODIFIED), lastModified);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_MIMETYPE), mimeType);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_ENCODING), enc);

        InputStream data = new ByteArrayInputStream("\u0633\u0634".getBytes("UTF-8"));
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_DATA), rs.getQValueFactory().create(data));

        rs.submit(b);

        // test properties of the file node
        String uri = rs.getItemUri(fileID, si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.add(JCR_PARENT);
        expected.add(JCR_PRIMARYITEM);
        /*
         Expected property names

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}resourcetype
        {DAV:}lockdiscovery
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}primaryitem
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_PRIMARYITEM);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_PRIMARYITEM);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testCheckedOutVersionableNode() throws Exception {
        NodeId nid = getNodeId("/test");
        Batch b = rs.createBatch(si, nid);
        b.setMixins(nid, new Name[] {NameConstants.MIX_VERSIONABLE});
        rs.submit(b);

        String uri = rs.getItemUri(nid, si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.addAll(VERSIONABLE_SET);
        expected.add(OrderingConstants.ORDERING_TYPE);
        expected.add(JCR_PARENT);
        expected.add(JCR_UUID);
        expected.add(VersionControlledResource.CHECKED_OUT);        
        expected.add(VersionControlledResource.PREDECESSOR_SET);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}ordering-type
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}uuid
        {DAV:}predecessor-set
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}version-history
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {DAV:}auto-version
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {DAV:}checked-out
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(OrderingConstants.ORDERING_TYPE);        
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_UUID);
        expected.remove(VersionControlledResource.CHECKED_OUT);
        expected.remove(VersionControlledResource.VERSION_HISTORY);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {DAV:}predecessor-set
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}auto-version
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(OrderingConstants.ORDERING_TYPE);                
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_UUID);
        props.add(VersionControlledResource.CHECKED_OUT);
        props.add(VersionControlledResource.PREDECESSOR_SET);
        props.add(VersionControlledResource.VERSION_HISTORY);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testCheckedInVersionableNode() throws Exception {
        NodeId nid = getNodeId("/test");
        Batch b = rs.createBatch(si, nid);
        b.setMixins(nid, new Name[] {NameConstants.MIX_VERSIONABLE});
        rs.submit(b);
        rs.checkin(si, nid);

        String uri = rs.getItemUri(nid, si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.addAll(VERSIONABLE_SET);
        expected.add(OrderingConstants.ORDERING_TYPE);
        expected.add(JCR_PARENT);
        expected.add(JCR_UUID);
        expected.add(VersionControlledResource.CHECKED_IN);

        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}ordering-type
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}uuid
        {DAV:}checked-in
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}version-history
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {DAV:}auto-version
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(OrderingConstants.ORDERING_TYPE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_UUID);
        expected.remove(VersionControlledResource.CHECKED_IN);
        expected.remove(VersionControlledResource.VERSION_HISTORY);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {DAV:}auto-version
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(OrderingConstants.ORDERING_TYPE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_UUID);
        props.add(VersionControlledResource.CHECKED_IN);
        props.add(VersionControlledResource.VERSION_HISTORY);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testVersion() throws Exception {
        NodeId nid = getNodeId("/test");
        Batch b = rs.createBatch(si, nid);
        b.setMixins(nid, new Name[] {NameConstants.MIX_VERSIONABLE});
        rs.submit(b);
        NodeId vID = rs.checkin(si, nid);

        String uri = rs.getItemUri(vID, si);

        DavPropertyNameSet set = doPropFindNames(uri);
        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.addAll(VERSION_SET);
        expected.add(JCR_PARENT);
        expected.add(JCR_UUID);
        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}version-name
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}uuid
        {DAV:}checkout-set
        {DAV:}predecessor-set
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}version-history
        {DAV:}successor-set
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}label-name-set
        {DAV:}supportedlock
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_UUID);
        expected.remove(VersionResource.VERSION_NAME);
        expected.remove(VersionResource.LABEL_NAME_SET);
        expected.remove(VersionResource.PREDECESSOR_SET);
        expected.remove(VersionResource.SUCCESSOR_SET);
        expected.remove(VersionResource.VERSION_HISTORY);
        expected.remove(VersionResource.CHECKOUT_SET);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {DAV:}iscollection
        {DAV:}getcontenttype
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_UUID);
        props.add(VersionResource.VERSION_NAME);
        props.add(VersionResource.LABEL_NAME_SET);
        props.add(VersionResource.PREDECESSOR_SET);
        props.add(VersionResource.SUCCESSOR_SET);
        props.add(VersionResource.VERSION_HISTORY);
        props.add(VersionResource.CHECKOUT_SET);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }

    public void testVersionHistory() throws Exception {
        NodeId nid = getNodeId("/test");
        Batch b = rs.createBatch(si, nid);
        b.setMixins(nid, new Name[] {NameConstants.MIX_VERSIONABLE});
        rs.submit(b);
        NodeId vID = rs.checkin(si, nid);

        String uri = Text.getRelativeParent(rs.getItemUri(vID, si), 1);
        DavPropertyNameSet set = doPropFindNames(uri);

        DavPropertyNameSet expected = new DavPropertyNameSet(BASE_SET);
        expected.addAll(EXISTING_ITEM_BASE_SET);
        expected.addAll(NODE_SET);
        expected.addAll(VERSIONHISTORY_SET);
        expected.add(JCR_PARENT);
        expected.add(JCR_UUID);

        /*
         Expected property names

        {DAV:}getlastmodified
        {DAV:}root-version
        {DAV:}version-set
        {http://www.day.com/jcr/webdav/1.0}definition
        {DAV:}comment
        {http://www.day.com/jcr/webdav/1.0}references
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {http://www.day.com/jcr/webdav/1.0}subscriptiondiscovery
        {http://www.day.com/jcr/webdav/1.0}uuid
        {http://www.day.com/jcr/webdav/1.0}name
        {DAV:}current-user-privilege-set
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
        {DAV:}workspace
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}index
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}versionableuuid
        {DAV:}supported-method-set
        {DAV:}iscollection
        {http://www.day.com/jcr/webdav/1.0}weakreferences
        {DAV:}creator-displayname
        {DAV:}getcontenttype
        {DAV:}creationdate
        {DAV:}supported-report-set
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        */
        assertPropertyNames(expected, set);

        DavPropertyNameSet all = doPropFindAll(uri);
        expected.remove(DeltaVConstants.COMMENT);
        expected.remove(DeltaVConstants.CREATOR_DISPLAYNAME);
        expected.remove(DeltaVConstants.SUPPORTED_METHOD_SET);
        expected.remove(DeltaVConstants.SUPPORTED_REPORT_SET);
        expected.remove(DeltaVConstants.WORKSPACE);
        expected.remove(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        expected.remove(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        expected.remove(JCR_DEFINITION);
        expected.remove(JCR_INDEX);
        expected.remove(JCR_REFERENCES);
        expected.remove(JCR_WEAK_REFERENCES);
        expected.remove(JCR_UUID);
        expected.remove(VersionHistoryResource.ROOT_VERSION);
        expected.remove(VersionHistoryResource.VERSION_SET);
        /*
        Expected all-props

        {DAV:}getlastmodified
        {http://www.day.com/jcr/webdav/1.0}depth
        {http://www.day.com/jcr/webdav/1.0}workspaceName
        {DAV:}displayname
        {http://www.day.com/jcr/webdav/1.0}parent
        {DAV:}supportedlock
        {http://www.day.com/jcr/webdav/1.0}versionableuuid
        {DAV:}iscollection
        {DAV:}getcontenttype
        {DAV:}creationdate
        {http://www.day.com/jcr/webdav/1.0}name
        {http://www.day.com/jcr/webdav/1.0}mixinnodetypes
        {http://www.day.com/jcr/webdav/1.0}path
        {http://www.day.com/jcr/webdav/1.0}primarynodetype
        {DAV:}lockdiscovery
        {DAV:}resourcetype
         */
        assertPropertyNames(expected , all);

        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(DeltaVConstants.COMMENT);
        props.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        props.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        props.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        props.add(DeltaVConstants.WORKSPACE);
        props.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
        props.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        props.add(JCR_DEFINITION);
        props.add(JCR_INDEX);
        props.add(JCR_REFERENCES);
        props.add(JCR_WEAK_REFERENCES);
        props.add(JCR_UUID);
        props.add(VersionHistoryResource.ROOT_VERSION);
        props.add(VersionHistoryResource.VERSION_SET);
        DavPropertyNameSet result = doPropFindByProp(uri, props);
        assertPropertyNames(props, result);
    }



    //==========================================================================
    // copied from jcr-server sources
    //==========================================================================    
    private static final DavPropertyName JCR_WORKSPACE_NAME = DavPropertyName.create(JCR_WORKSPACE_NAME_LN, NAMESPACE);
    private static final DavPropertyName JCR_NAME = DavPropertyName.create(JCR_NAME_LN, NAMESPACE);
    private static final DavPropertyName JCR_PATH = DavPropertyName.create(JCR_PATH_LN, NAMESPACE);
    private static final DavPropertyName JCR_DEPTH = DavPropertyName.create(JCR_DEPTH_LN, NAMESPACE);
    private static final DavPropertyName JCR_PARENT = DavPropertyName.create(JCR_PARENT_LN, NAMESPACE);
    private static final DavPropertyName JCR_DEFINITION = DavPropertyName.create(JCR_DEFINITION_LN, NAMESPACE);
    private static final DavPropertyName JCR_PRIMARYNODETYPE = DavPropertyName.create(JCR_PRIMARYNODETYPE_LN, NAMESPACE);
    private static final DavPropertyName JCR_MIXINNODETYPES = DavPropertyName.create(JCR_MIXINNODETYPES_LN, NAMESPACE);
    private static final DavPropertyName JCR_INDEX = DavPropertyName.create(JCR_INDEX_LN, NAMESPACE);
    private static final DavPropertyName JCR_REFERENCES = DavPropertyName.create(JCR_REFERENCES_LN, NAMESPACE);
    private static final DavPropertyName JCR_WEAK_REFERENCES = DavPropertyName.create(JCR_WEAK_REFERENCES_LN, NAMESPACE);
    private static final DavPropertyName JCR_UUID = DavPropertyName.create(JCR_UUID_LN, NAMESPACE);
    private static final DavPropertyName JCR_PRIMARYITEM = DavPropertyName.create(JCR_PRIMARYITEM_LN, NAMESPACE);
    private static final DavPropertyName JCR_TYPE = DavPropertyName.create(JCR_TYPE_LN, NAMESPACE);
    private static final DavPropertyName JCR_VALUE = DavPropertyName.create(JCR_VALUE_LN, NAMESPACE);
    private static final DavPropertyName JCR_VALUES = DavPropertyName.create(JCR_VALUES_LN, NAMESPACE);
    private static final DavPropertyName JCR_LENGTH = DavPropertyName.create(JCR_LENGTH_LN, NAMESPACE);
    private static final DavPropertyName JCR_LENGTHS = DavPropertyName.create(JCR_LENGTHS_LN, NAMESPACE);
    private static final DavPropertyName JCR_NAMESPACES = DavPropertyName.create(JCR_NAMESPACES_LN, NAMESPACE);
    private static final DavPropertyName JCR_NODETYPES_CND = DavPropertyName.create(JCR_NODETYPES_CND_LN, NAMESPACE);
    private static final DavPropertyName JCR_VERSIONABLEUUID = DavPropertyName.create(JCR_VERSIONABLEUUID_LN, NAMESPACE);

    /**
     * Default property names present with all resources.
     */
    private static final DavPropertyNameSet BASE_SET = new DavPropertyNameSet();
    static {
        BASE_SET.add(DavPropertyName.DISPLAYNAME);
        BASE_SET.add(DavPropertyName.RESOURCETYPE);
        BASE_SET.add(DavPropertyName.ISCOLLECTION);
        BASE_SET.add(DavPropertyName.GETLASTMODIFIED);
        BASE_SET.add(DavPropertyName.CREATIONDATE);
        BASE_SET.add(DavPropertyName.SUPPORTEDLOCK);
        BASE_SET.add(DavPropertyName.LOCKDISCOVERY);
        BASE_SET.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        BASE_SET.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        BASE_SET.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        BASE_SET.add(DeltaVConstants.COMMENT);
        BASE_SET.add(JCR_WORKSPACE_NAME);
    }

    /**
     * Property names defined for JCR workspace resources.
     */
    private static final DavPropertyNameSet WORKSPACE_SET = new DavPropertyNameSet();
    static {
        WORKSPACE_SET.add(DeltaVConstants.WORKSPACE);
        WORKSPACE_SET.add(JCR_NAMESPACES);
        WORKSPACE_SET.add(JCR_NODETYPES_CND);
    }

    /**
     * Additional property names defined for existing and non-existing item resources.
     */
    private static final DavPropertyNameSet ITEM_BASE_SET = new DavPropertyNameSet();
    static {
        ITEM_BASE_SET.add(DavPropertyName.GETCONTENTTYPE);
        ITEM_BASE_SET.add(DeltaVConstants.WORKSPACE);
        ITEM_BASE_SET.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
        ITEM_BASE_SET.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
    }

    /**
     * Additional property names defined for existing item resources.
     */
    private static final DavPropertyNameSet EXISTING_ITEM_BASE_SET = new DavPropertyNameSet(ITEM_BASE_SET);
    static {
        EXISTING_ITEM_BASE_SET.add(JCR_NAME);
        EXISTING_ITEM_BASE_SET.add(JCR_PATH);
        EXISTING_ITEM_BASE_SET.add(JCR_DEPTH);
        EXISTING_ITEM_BASE_SET.add(JCR_DEFINITION);
    }

    /**
     * Additional property names defined by single value JCR properties.
     */
    private static final DavPropertyNameSet PROPERTY_SET = new DavPropertyNameSet();
    static {
        PROPERTY_SET.add(JCR_TYPE);
        PROPERTY_SET.add(JCR_VALUE);
        PROPERTY_SET.add(JCR_LENGTH);
    }

    /**
     * Additional property names defined by single value JCR properties.
     */
    private static final DavPropertyNameSet PROPERTY_MV_SET = new DavPropertyNameSet();
    static {
        PROPERTY_MV_SET.add(JCR_TYPE);
        PROPERTY_MV_SET.add(JCR_VALUES);
        PROPERTY_MV_SET.add(JCR_LENGTHS);
    }

    /**
     * Additional property names defined by regular JCR nodes.
     */
    private static final DavPropertyNameSet NODE_SET = new DavPropertyNameSet();
    static {
        NODE_SET.add(JCR_PRIMARYNODETYPE);
        NODE_SET.add(JCR_MIXINNODETYPES);
        NODE_SET.add(JCR_INDEX);
        NODE_SET.add(JCR_REFERENCES);
        NODE_SET.add(JCR_WEAK_REFERENCES);
    }

    /**
     * Additional property names defined by versionable JCR nodes.
     */
    private static final DavPropertyNameSet VERSIONABLE_SET = new DavPropertyNameSet();
    static {
        VERSIONABLE_SET.add(VersionControlledResource.VERSION_HISTORY);
        VERSIONABLE_SET.add(VersionControlledResource.AUTO_VERSION);
    }

    /**
     * Additional property names defined by JCR version nodes.
     */
    private static final DavPropertyNameSet VERSION_SET = new DavPropertyNameSet();
    static {
        VERSION_SET.add(VersionResource.VERSION_NAME);
        VERSION_SET.add(VersionResource.LABEL_NAME_SET);
        VERSION_SET.add(VersionResource.PREDECESSOR_SET);
        VERSION_SET.add(VersionResource.SUCCESSOR_SET);
        VERSION_SET.add(VersionResource.VERSION_HISTORY);
        VERSION_SET.add(VersionResource.CHECKOUT_SET);
    }

    /**
     * Additional property names defined by JCR version history nodes.
     */
    private static final DavPropertyNameSet VERSIONHISTORY_SET = new DavPropertyNameSet();
    static {
        VERSIONHISTORY_SET.add(VersionHistoryResource.ROOT_VERSION);
        VERSIONHISTORY_SET.add(VersionHistoryResource.VERSION_SET);
        VERSIONHISTORY_SET.add(JCR_VERSIONABLEUUID);
    }
}