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
package org.apache.jackrabbit.core.persistence.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Tests that a node or a node bundle can not get internally corrupt.
 */
public class NodeCorruptionTest extends AbstractJCRTest {

    public void testCopyMultiSingleValue() throws Exception {
        Node root = superuser.getRootNode();
        String nodeName = "testCopyMulti" + System.currentTimeMillis();
        if (root.hasNode(nodeName)) {
            root.getNode(nodeName).remove();
            superuser.save();
        }
        Node test = root.addNode(nodeName);
        test.setProperty("x", "Hi");
        superuser.save();

        String wsp = superuser.getWorkspace().getName();
        String workspace2 = getAlternativeWorkspaceName();
        if (workspace2 == null) {
            throw new NotExecutableException();
        }
        Session s2 = getHelper().getSuperuserSession(workspace2);
        s2.getWorkspace().clone(wsp, "/" + nodeName, "/" + nodeName, true);
        
        Node test2 = s2.getRootNode().getNode(nodeName);
        test2.setProperty("x", (Value) null);
        test2.setProperty("x", new String[]{});
        s2.save();
        
        test.update(workspace2);

        try {
            Value[] values = test.getProperty("x").getValues();
            assertEquals(0, values.length);
        } catch (RepositoryException e) {
            // if we get here, it's a bug, as it is a multi-valued property now
            // anyway, let's see what happens if we try to read it as a single valued property
            test.getProperty("x").getValue();
            // even if that works: it's still a bug
            throw e;
        }
        
    }
    
    private String getAlternativeWorkspaceName() throws RepositoryException {
        String altWsp = null;
        String[] wsps = superuser.getWorkspace().getAccessibleWorkspaceNames();
        if (wsps.length == 1) {
            superuser.getWorkspace().createWorkspace("tmp");
            altWsp = "tmp";
        } else {
            for (String name : wsps) {
                if (!name.equals(superuser.getWorkspace().getName())) {
                    altWsp = name;
                    break;
                }
            }
        }
        return altWsp;
    }
    
}