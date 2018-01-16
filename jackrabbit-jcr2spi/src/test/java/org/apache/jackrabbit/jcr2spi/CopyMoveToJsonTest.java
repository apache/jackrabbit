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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class CopyMoveToJsonTest extends AbstractJCRTest {

    private String jsondata = "{\"foo\":\"bar\"}";

    public void testCreateJson() throws Exception {
        createJsonNode("test.json");

        Session s = getHelper().getReadOnlySession();
        try {
            Property p = s.getNode(testRoot).getNode("test.json").getNode(JcrConstants.JCR_CONTENT)
                    .getProperty(JcrConstants.JCR_DATA);
            assertEquals(jsondata, IOUtils.toString(p.getBinary().getStream(), "UTF-8"));
        } finally {
            s.logout();
        }
    }

    public void testCopyJson() throws Exception {
        Node test = createJsonNode("test.json");
        test.getSession().getWorkspace().copy(test.getPath(), test.getParent().getPath() + "/target.json");

        Session s = getHelper().getReadOnlySession();
        try {
            Property p = s.getNode(testRoot).getNode("target.json").getNode(JcrConstants.JCR_CONTENT)
                    .getProperty(JcrConstants.JCR_DATA);
            assertEquals(jsondata, IOUtils.toString(p.getBinary().getStream(), "UTF-8"));
        } finally {
            s.logout();
        }
    }

    public void testMoveJson() throws Exception {
        Node test = createJsonNode("test.json");
        test.getSession().getWorkspace().move(test.getPath(), test.getParent().getPath() + "/target.json");

        Session s = getHelper().getReadOnlySession();
        try {
            Property p = s.getNode(testRoot).getNode("target.json").getNode(JcrConstants.JCR_CONTENT)
                    .getProperty(JcrConstants.JCR_DATA);
            assertEquals(jsondata, IOUtils.toString(p.getBinary().getStream(), "UTF-8"));
        } finally {
            s.logout();
        }
    }

    private Node createJsonNode(String name) throws RepositoryException, UnsupportedEncodingException {
        Node test = testRootNode.addNode(name, JcrConstants.NT_FILE);
        Node content = test.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
        content.setProperty(JcrConstants.JCR_DATA,
                test.getSession().getValueFactory().createBinary(new ByteArrayInputStream(jsondata.getBytes("UTF-8"))));
        test.getSession().save();
        return test;
    }
}
