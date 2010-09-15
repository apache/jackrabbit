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
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * <code>ExtensionTest</code>...
 */
public class ExtensionTest extends AbstractSPITest {

    private String testPath;
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
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            removeTestNode(testPath);
        } finally {
            rs.dispose(si);
            super.tearDown();
        }
    }

    private void createTestNode(String testPath) throws RepositoryException {
        Batch b = rs.createBatch(si, getNodeId("/"));
        String name = Text.getName(testPath);
        b.addNode(getNodeId("/"), resolver.getQName(name), NameConstants.NT_UNSTRUCTURED, null);
        rs.submit(b);
    }

    private void removeTestNode(String path) throws RepositoryException {
        Batch b = rs.createBatch(si, getNodeId("/"));
        b.remove(getNodeId(path));
        rs.submit(b);
    }

    private NodeId getNodeId(String path) throws NamespaceException, RepositoryException {
        return rs.getIdFactory().createNodeId((String) null, resolver.getQPath(path));
    }

    private void assertProperDepthExtensionHandling(String path, String name, boolean create) throws RepositoryException {
        Name testName = resolver.getQName(name);
        if (create) {
            createTestNode(path);
            testPath  = path;
        }

        NodeInfo nInfo = rs.getNodeInfo(si, getNodeId(path));
        //System.out.println("NodeInfo: " + nInfo.getPath().getNameElement().getName());
        assertEquals(testName, nInfo.getPath().getName());

        Iterator<? extends ItemInfo > it = rs.getItemInfos(si, getNodeId(path));
        assertTrue(it.hasNext());
        nInfo = (NodeInfo) it.next();
        //System.out.println("ItemInfo: " + nInfo.getPath().getNameElement().getName());
        assertEquals(testName, nInfo.getPath().getName());
    }

    public void testJsonExtension() throws RepositoryException {
        assertProperDepthExtensionHandling("/test.json", "test.json", true);
    }

    public void testNumberExtension() throws RepositoryException {
        assertProperDepthExtensionHandling("/test.24", "test.24", true);
    }

    public void testNumberJsonExtension() throws RepositoryException {
        assertProperDepthExtensionHandling("/test.5.json", "test.5.json", true);
    }

    public void testNumberJsonExtension2() throws RepositoryException {
        assertProperDepthExtensionHandling("/test.5.json.json", "test.5.json.json", true);
    }

    public void testMultipleNodes() throws RepositoryException {
        createTestNode("/test");
        try {
            assertProperDepthExtensionHandling("/test.json", "test.json", true);
            assertProperDepthExtensionHandling("/test", "test", false);
        } finally {
            removeTestNode("/test");
        }
    }

     public void testIndex() throws RepositoryException {
         Name testName = resolver.getQName("test");
         testPath = "/test";
         createTestNode("/test");
         createTestNode("/test");

         NodeInfo nInfo = rs.getNodeInfo(si, getNodeId("/test[2]"));
         //System.out.println("NodeInfo: " + nInfo.getPath().getNameElement().getName());
         assertEquals(testName, nInfo.getPath().getName());

         Iterator<? extends ItemInfo> it = rs.getItemInfos(si, getNodeId("/test[2]"));
         assertTrue(it.hasNext());
         nInfo = (NodeInfo) it.next();
         //System.out.println("ItemInfo: " + nInfo.getPath().getNameElement().getName());
         assertEquals(testName, nInfo.getPath().getName());

         removeTestNode("/test[2]");
     }
}
