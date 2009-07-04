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
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * 
 */
public final class ReferencesTest extends AbstractJCRTest {

    private String uniquePrefix;

    private static int cnt = 0;

    /**
     * {@inheritDoc}
     */
    public void setUp() throws Exception {
        uniquePrefix = "referencesTest" + System.currentTimeMillis() + "-" + cnt;
        cnt++;
        Session session = createSession();
        Node a = getTestRootNode(session).addNode("A");
        Node b = a.addNode("B");
        a.addMixin("mix:referenceable");
        b.addMixin("mix:referenceable");
        getTestRootNode(session).addNode("C");
        saveAndlogout(session);
    }

    /**
     * Tries to create a double back-reference to "ref to B" property.
     * 
     * @throws Exception on test error
     */
    public void testDoubleBackReference() throws Exception {
        Session session1 = createSession();
        Node bses1 = getTestRootNode(session1).getNode("A").getNode("B");
        getTestRootNode(session1).getNode("C").setProperty("ref to B", bses1);

        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B", bses2);

        saveAndlogout(session1, session2);
        assertRemoveTestNodes();
    }

    /**
     * Tries to create a single back-reference to "ref to B" property which does not exist.
     * 
     * @throws Exception on test error
     */
    public void testBackRefToNonExistingProp() throws Exception {
        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B", bses2);

        Session session3 = createSession();
        getTestRootNode(session3).getNode("C").setProperty("ref to B", new Value[]{});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Tries to create a single back-reference to "ref" property for both A and B whereas "ref" is single
     * valued and points to A.
     * 
     * @throws Exception on test error
     */
    public void testMisdirectedBackRef() throws Exception {
        Session session2 = createSession();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref", bses2);

        Session session3 = createSession();
        Node ases3 = getTestRootNode(session3).getNode("A");
        getTestRootNode(session3).getNode("C").setProperty("ref", ases3);

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Variant of {@link #testDoubleBackReference()} for mult-valued props.
     * 
     * @throws Exception on test error
     */
    public void testDoubleBackRefReferenceMultiValued() throws Exception {
        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B",
            new Value[]{valFac2.createValue(bses2), valFac2.createValue(bses2)});

        Session session3 = createSession();
        ValueFactory valFac3 = session3.getValueFactory();
        Node bses3 = getTestRootNode(session3).getNode("A").getNode("B");
        getTestRootNode(session3).getNode("C").setProperty("ref to B",
            new Value[]{valFac3.createValue(bses3), valFac3.createValue(bses3)});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Variant of {@link #testMisdirectedBackRef()} for multi-valued props.
     * 
     * @throws Exception on test error
     */
    public void testMisdirectedBackRefMultiValued() throws Exception {
        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node ases2 = getTestRootNode(session2).getNode("A");
        getTestRootNode(session2).getNode("C").setProperty("ref",
            new Value[]{valFac2.createValue(ases2), valFac2.createValue(ases2)});

        Session session3 = createSession();
        ValueFactory valFac3 = session3.getValueFactory();
        Node bses3 = getTestRootNode(session3).getNode("A").getNode("B");
        getTestRootNode(session3).getNode("C").setProperty("ref", new Value[]{valFac3.createValue(bses3)});

        saveAndlogout(session2, session3);
        assertRemoveTestNodes();
    }

    /**
     * Regular references usage.
     * 
     * @throws Exception on test error
     */
    public void testRegularReference() throws Exception {
        Session session1 = createSession();
        Node bses1 = getTestRootNode(session1).getNode("A").getNode("B");
        getTestRootNode(session1).getNode("A").setProperty("ref to B", bses1);

        Session session2 = createSession();
        ValueFactory valFac2 = session2.getValueFactory();
        Node bses2 = getTestRootNode(session2).getNode("A").getNode("B");
        getTestRootNode(session2).getNode("C").setProperty("ref to B",
            new Value[]{valFac2.createValue(bses2), valFac2.createValue(bses2)});
        getTestRootNode(session2).getNode("C").setProperty("another ref to B", bses2);

        saveAndlogout(session1, session2);
        assertRemoveTestNodes();
    }

    private void assertRemoveTestNodes() throws RepositoryException {
        Session session = createSession();
        getTestRootNode(session).remove();
        assertSave(session);
        session.logout();
    }

    /**
     * @param session the session to save
     */
    private void assertSave(Session session) {
        try {
            session.save();
        } catch (RepositoryException e) {
            fail("saving session failed: " + e.getMessage());
        }
    }

    /**
     * @return a super user session
     * @throws RepositoryException on error
     */
    private Session createSession() throws RepositoryException {
        return getHelper().getSuperuserSession();
    }

    private void saveAndlogout(Session... sessions) throws RepositoryException {
        if (sessions != null) {
            for (Session session : sessions) {
                session.save();
                session.logout();
            }
        }
    }

    /**
     * @param session the session to use
     * @return a node which is more or less unique per testcase
     * @throws RepositoryException on error
     */
    private Node getTestRootNode(Session session) throws RepositoryException {
        if (session.getRootNode().hasNode(uniquePrefix)) {
            return session.getRootNode().getNode(uniquePrefix);
        } else {
            return session.getRootNode().addNode(uniquePrefix);
        }
    }
}
