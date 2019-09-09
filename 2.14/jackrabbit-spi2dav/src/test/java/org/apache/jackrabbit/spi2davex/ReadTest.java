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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.AbstractSPITest;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * <code>ConnectionTest</code>...
 */
public class ReadTest extends AbstractSPITest {

    private final String testPath = "/test";
    private NamePathResolver resolver;
    private RepositoryService rs;
    private SessionInfo si;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        rs = helper.getRepositoryService();
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

    public void testReadNode() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        rs.getItemInfos(si, nid);
    }

    public void testReadNonExistingNode() throws RepositoryException {
        NodeId nid = getNodeId(testPath + "/non-existing");
        try {
            rs.getItemInfos(si, nid);
            fail();
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    public void testReadNonExistingNode2() throws RepositoryException {
        NodeId nid = getNodeId(testPath + "/non-existing");
        try {
            rs.getNodeInfo(si, nid);
            fail();
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    public void testReadPropertyAsNode() throws RepositoryException {
        NodeId nid = getNodeId(testPath + "/jcr:primaryType");
        try {
            rs.getItemInfos(si, nid);
            fail();
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    public void testReadNonExistingProperty() throws RepositoryException {
        NodeId nid = getNodeId(testPath);
        PropertyId pid = getPropertyId(nid, NameConstants.JCR_CHILDNODEDEFINITION);

        try {
            rs.getPropertyInfo(si, pid);
            fail();
        } catch (ItemNotFoundException e) {
            // ok
        }
    }

    //--------------------------------------------------------------------------
    private NodeId getNodeId(String path) throws NamespaceException, RepositoryException {
        return rs.getIdFactory().createNodeId((String) null, resolver.getQPath(path));
    }

    private PropertyId getPropertyId(NodeId nId, Name propName) throws RepositoryException {
        return rs.getIdFactory().createPropertyId(nId, propName);
    }
}
