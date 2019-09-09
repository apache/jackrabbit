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
package org.apache.jackrabbit.test.api;

import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Compliance tests for section 6.12 Lifecycle Management.
 *
 */
public class LifecycleTest extends AbstractJCRTest {

    private String path;

    private String transition;

    protected void setUp() throws Exception {
        super.setUp();
        checkSupportedOption(Repository.OPTION_LIFECYCLE_SUPPORTED);
        ensureKnowsNodeType(superuser, NodeType.MIX_LIFECYCLE);

        path = getProperty("lifecycleNode");
        if (path == null) {
            path = testRoot + "/lifecycle";
        }

        transition = getProperty("lifecycleTransition");
        if (transition == null) {
            transition = "identity";
        }
    }

    public void testGetAllowedLifecycleTransitions()
            throws RepositoryException, NotExecutableException {
        Node node = superuser.getNode(path);
        try {
            String[] transitions = node.getAllowedLifecycleTransistions();
            assertNotNull(
                    "Return value of getAllowedLifecycleTransitions is null",
                    transitions);

            for (int i = 0; i < transitions.length; i++) {
                if (transition.equals(transitions[i])) {
                    return;
                }
            }
            fail("Configured lifecycle transition \"" + transition
                    + "\" is not among the allowed transitions from node "
                    + path);
        } catch (UnsupportedRepositoryOperationException e) {
            fail("Unable to get allowed lifecycle transitions for node "
                    + path + ": " + e.getMessage());
        }
    }

    public void testFollowLifecycleTransition()
            throws RepositoryException, NotExecutableException {
        Node node = superuser.getNode(path);
        try {
            node.followLifecycleTransition(transition);
            // Note that there is nothing much here for us to check,
            // as the spec doesn't specify any fixed behaviour for
            // this method!
        } catch (UnsupportedRepositoryOperationException e) {
            fail("Unable to follow lifecycle transition \"" + transition
                    + "\" for node " + path + ": " + e.getMessage());
        } catch (InvalidLifecycleTransitionException e) {
            fail("Unable to follow lifecycle transition \"" + transition
                    + "\" for node " + path + ": " + e.getMessage());
        }
    }
}
