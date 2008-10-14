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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

/** <code>GetPropertyTest</code>... */
public class GetPropertyTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(GetPropertyTest.class);

    private String prop1Path;
    private String prop2Path;

    private Session readOnly;

    protected void setUp() throws Exception {
        super.setUp();

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Property p = n.setProperty(propertyName1, "string1");
        prop1Path = p.getPath();

        p = n.setProperty(propertyName2, "string2");
        prop2Path = p.getPath();

        testRootNode.save();

        readOnly = helper.getReadOnlySession();
    }

    protected void tearDown() throws Exception {
        if (readOnly != null) {
            readOnly.logout();
        }
        super.tearDown();
    }

    public void testItemExists() throws RepositoryException {
        assertTrue(readOnly.itemExists(prop1Path));
        assertTrue(readOnly.itemExists(prop2Path));
    }

    public void testGetItem() throws RepositoryException {
        assertFalse(readOnly.getItem(prop1Path).isNode());
        assertFalse(readOnly.getItem(prop2Path).isNode());
    }

    public void testHasProperty() throws RepositoryException {
        String testPath = testRootNode.getPath();
        Node trn = (Node) readOnly.getItem(testPath);

        assertTrue(trn.hasProperty(prop1Path.substring(testPath.length() + 1)));
        assertTrue(trn.hasProperty(prop2Path.substring(testPath.length() + 1)));
    }

    public void testGetProperty() throws RepositoryException {
        String testPath = testRootNode.getPath();
        Node trn = (Node) readOnly.getItem(testPath);

        trn.getProperty(prop1Path.substring(testPath.length() + 1));
        trn.getProperty(prop2Path.substring(testPath.length() + 1));
    }
}