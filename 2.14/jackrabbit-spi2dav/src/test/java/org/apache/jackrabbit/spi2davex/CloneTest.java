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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * <code>CloneTest</code>...
 */
public class CloneTest extends AbstractSPITest {

    private final String testPath = "/test";
    private NamePathResolver resolver;
    private RepositoryService rs;
    private SessionInfo si;

    private SessionInfo sInfo;
    private NodeId clonedId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        rs = helper.getRepositoryService();
        si = helper.getAdminSessionInfo();
        NamespaceResolver nsResolver = new AbstractNamespaceResolver() {
            public String getURI(String prefix) throws NamespaceException {
                return ("jcr".equals(prefix)) ? "http://www.jcp.org/jcr/1.0" : prefix;
            }
            public String getPrefix(String uri) throws NamespaceException {
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

        // todo: retrieve second wsp-name from config
        sInfo = rs.obtain(si, "test");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (si != null) {
                Batch b = rs.createBatch(si, getNodeId("/"));
                b.remove(getNodeId(testPath));
                rs.submit(b);
            }
            if (sInfo != null && clonedId != null) {
                Batch b = rs.createBatch(sInfo, getNodeId("/"));
                b.remove(clonedId);
                rs.submit(b);
            }
        } catch (RepositoryException e) {
            // cleanup failed... ignore.
        } finally {
            if (sInfo != null) {
                rs.dispose(sInfo);                
            }
            if (si != null) {
                rs.dispose(si);
            }
            super.tearDown();
        }
    }

    private NodeId getNodeId(String path) throws RepositoryException {
        return rs.getIdFactory().createNodeId((String) null, resolver.getQPath(path));
    }

    public void testClone() throws RepositoryException {
        NodeId srcId = getNodeId(testPath);
        NodeId destParentId = getNodeId("/");
        rs.clone(sInfo, si.getWorkspaceName(), srcId, destParentId, resolver.getQName("destname"), true);

        clonedId = getNodeId("/destname");
        NodeInfo nInfo = rs.getNodeInfo(sInfo, clonedId);
        Iterator<? extends ItemInfo> it = rs.getItemInfos(sInfo, clonedId);

        assertTrue(it.hasNext());
        NodeInfo nInfo2 = (NodeInfo) it.next();
        assertEquals(nInfo.getId(), nInfo2.getId());
        assertEquals(nInfo.getNodetype(), nInfo2.getNodetype());
    }
}
