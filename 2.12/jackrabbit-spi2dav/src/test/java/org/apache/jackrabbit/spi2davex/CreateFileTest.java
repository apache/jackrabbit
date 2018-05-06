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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.spi.AbstractSPITest;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * <code>CreateFileTest</code>...
 */
public class CreateFileTest extends AbstractSPITest {
    
    private final String testPath = "/test";
    private NamePathResolver resolver;
    private RepositoryService rs;
    private SessionInfo si;

    private QValue lastModified;
    private QValue mimeType;
    private QValue enc;

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

        lastModified = rs.getQValueFactory().create(Calendar.getInstance());
        mimeType = rs.getQValueFactory().create("text/plain", PropertyType.STRING);
        enc = rs.getQValueFactory().create("utf-8", PropertyType.STRING);
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

    public void testCreateFile() throws RepositoryException, IOException {
        Name fileName = resolver.getQName("test.txt");
        createFile(fileName);
    }

    public void testCreateFileWithNonLatinCharacters() throws RepositoryException, IOException {
        Name fileName = resolver.getQName("\u0633\u0634.txt");
        createFile(fileName);
    }

    public void testPropertiesWithNonLatinCharacters() throws RepositoryException, IOException {
        Name fileName = resolver.getQName("\u0633\u0634.txt");
        createFile(fileName);

        NodeId nid = getNodeId(testPath + "/\u0633\u0634.txt/jcr:content");

        PropertyInfo pi = rs.getPropertyInfo(si, rs.getIdFactory().createPropertyId(nid, NameConstants.JCR_LASTMODIFIED));
        assertEquals(lastModified, pi.getValues()[0]);

        pi = rs.getPropertyInfo(si, rs.getIdFactory().createPropertyId(nid, NameConstants.JCR_MIMETYPE));
        assertEquals(mimeType, pi.getValues()[0]);

        pi = rs.getPropertyInfo(si, rs.getIdFactory().createPropertyId(nid, NameConstants.JCR_ENCODING));
        assertEquals(enc, pi.getValues()[0]);

        pi = rs.getPropertyInfo(si, rs.getIdFactory().createPropertyId(nid, NameConstants.JCR_DATA));
        assertEquals("\u0633\u0634", pi.getValues()[0].getString());
    }

    private void createFile(Name fileName) throws RepositoryException, IOException {
        NodeId root = getNodeId(testPath);

        Batch b = rs.createBatch(si, root);
        b.addNode(root, fileName, NameConstants.NT_FILE, null);

        String filePath = testPath + "/" + fileName.getLocalName();
        NodeId file = getNodeId(filePath);
        b.addNode(file, NameConstants.JCR_CONTENT, NameConstants.NT_RESOURCE, null);

        NodeId content = getNodeId(filePath + "/" + NameConstants.JCR_CONTENT);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_LASTMODIFIED), lastModified);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_MIMETYPE), mimeType);
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_ENCODING), enc);

        InputStream data = new ByteArrayInputStream("\u0633\u0634".getBytes("UTF-8"));
        b.addProperty(content, resolver.getQName(JcrConstants.JCR_DATA), rs.getQValueFactory().create(data));

        rs.submit(b);
    }
}