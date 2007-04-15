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
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.util.Text;

public class ExpandingArchiveClassPathEntryTest extends ClassLoaderTestBase {

    private static final String NODE_TYPE = "rep:jarFile";

    private static final String ROOT = "/test";
    private static final String JAR_PATH = ROOT + "/mock.jar";

    protected void setUp() throws Exception {
        super.setUp();

        if (session.itemExists(ROOT)) {
            log.info("Removing old test root entry");
            session.getItem(ROOT).remove();
            session.save();
        }
    }

    protected void tearDown() throws Exception {
        if (session.itemExists(ROOT)) {
            session.getItem(ROOT).remove();
            session.save();
        }

        super.tearDown();
    }

    public void testCanExpand() throws RepositoryException {
        // check for the node type - may or may not exist
        try {
            session.getWorkspace().getNodeTypeManager().getNodeType(NODE_TYPE);
            log.info("Node type " + NODE_TYPE + " already registered");
        } catch (NoSuchNodeTypeException nsoe) {
            // expected behaviour
            log.info("Node type " + NODE_TYPE + " not registered yet");
        }

        boolean canExpand = ExpandingArchiveClassPathEntry.canExpandArchives(session);
        assertTrue("Expecting archives to be expandable", canExpand);

        // check for the node type - must exist
        session.getWorkspace().getNodeTypeManager().getNodeType(NODE_TYPE);
        log.info("Node type " + NODE_TYPE + " already registered");
    }

    public void testExpand() throws IOException, RepositoryException {
        URL url = getClass().getResource("/mock.jar");

        Node parent = getParent(session, JAR_PATH);
        Node jar = parent.addNode(Text.getName(JAR_PATH), "nt:file");
        makeFileNode(jar, url.openConnection());
        session.save();

        Property prop = Util.getProperty(session.getItem(JAR_PATH));

        ExpandingArchiveClassPathEntry pe =
            new ExpandingArchiveClassPathEntry(prop, JAR_PATH);

        ClassLoaderResource res = pe.getResource("mock/aDir/anotherFile.txt");
        assertNotNull("anotherFile.txt expected to exist", res);

        url = res.getURL();
        assertNotNull("anotherFile's URL missing", url);

        String data = new String(res.getBytes());
        log.info("URL : {}", url);
        log.info("Path: {}", res.getPath());
        log.info("Prop: {}", res.getProperty().getPath());
        log.info("Data: '{}'", data);
        log.info("Size: {} (bytes: {})", new Integer(res.getContentLength()),
            new Integer(res.getBytes().length));
        log.info("Time: {}", new Date(res.getLastModificationTime()));
    }
}
