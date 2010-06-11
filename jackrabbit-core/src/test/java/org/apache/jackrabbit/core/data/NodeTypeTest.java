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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.test.AbstractJCRTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;

public class NodeTypeTest extends AbstractJCRTest {


    /**
     * Test a node type with a binary default value
     * @throws RepositoryException
     */
    public void testNodeTypesWithBinaryDefaultValue()
            throws RepositoryException, IOException {
        doTestNodeTypesWithBinaryDefaultValue(0);
        doTestNodeTypesWithBinaryDefaultValue(10);
        doTestNodeTypesWithBinaryDefaultValue(10000);
    }

    public void doTestNodeTypesWithBinaryDefaultValue(int len)
            throws RepositoryException, IOException {
        char[] chars = new char[len];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = 'a';
        }
        String def = new String(chars);

        String type = "test:foo" + len;
        String cnd =
            "<test='http://www.apache.org/jackrabbit/test'>\n"
            + "[" + type + "]\n - value(binary) = '" + def + "' m a";
        JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
            superuser.getWorkspace().getNodeTypeManager();
        if (!manager.hasNodeType(type)) {
            manager.registerNodeTypes(
                    new ByteArrayInputStream(cnd.getBytes("UTF-8")),
                    JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
        }

        Node root = superuser.getRootNode();
        Node node = root.addNode("testfoo" + len, type);
        Value value = node.getProperty("value").getValue();
        assertEquals(PropertyType.BINARY, value.getType());
        assertEquals(def, value.getString());
    }
    
    /**
     * Tests if it is possible to set a Long Value to a Date property
     * @see JCR-2652
     */
	public void testSetNonExactTypedPropertyValue() throws RepositoryException {
		Node node = testRootNode.addNode("foo", JcrConstants.NT_RESOURCE);
		ValueFactory vf = superuser.getValueFactory();
		Value value = vf.createValue("1234", 3);
		// Attempt to use the LongValue to set a Date property
		try {
			node.setProperty(JcrConstants.JCR_LASTMODIFIED, value);
			// Mandatory Properties will be set for JcrConstants.NT_RESOURCE
			node.setProperty(JcrConstants.JCR_DATA, "value");
			node.setProperty(JcrConstants.JCR_MIMETYPE, "text/plain");
		} catch (ConstraintViolationException e) {
            fail("No ConstraintViolation should happen if a LongValue is set to a Date property");
		}
		superuser.save();
		assertNotNull(node.getProperty(JcrConstants.JCR_LASTMODIFIED));
	}

}
