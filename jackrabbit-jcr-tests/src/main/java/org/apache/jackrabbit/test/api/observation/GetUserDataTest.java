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
package org.apache.jackrabbit.test.api.observation;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>GetUserDataTest</code> performs observation tests with user data set
 * on the observation manager.
 */
public class GetUserDataTest extends AbstractObservationTest {

    public void testSave() throws RepositoryException {
        runWithUserData(new Callable() {
            public void call() throws RepositoryException {
                testRootNode.addNode(nodeName1, testNodeType);
                testRootNode.getSession().save();
            }
        }, ALL_TYPES);
    }

    public void testWorkspaceOperation() throws RepositoryException {
        testRootNode.addNode(nodeName1);
        testRootNode.getSession().save();

        runWithUserData(new Callable() {
            public void call() throws RepositoryException {
                String src = testRoot + "/" + nodeName1;
                String dest = testRoot + "/" + nodeName2;
                superuser.getWorkspace().move(src, dest);
            }
        }, ALL_TYPES);
    }

    public void testVersioning()
            throws RepositoryException, NotExecutableException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);

        final Node n1 = testRootNode.addNode(nodeName1);
        ensureMixinType(n1, mixVersionable);
        testRootNode.getSession().save();

        runWithUserData(new Callable() {
            public void call() throws RepositoryException {
                n1.checkin();
            }
        }, Event.NODE_ADDED); // get events for added version node
    }

    protected void runWithUserData(final Callable c, int eventTypes)
            throws RepositoryException {
        final String data = createRandomString(5);
        Event[] events = getEvents(new Callable() {
            public void call() throws RepositoryException {
                obsMgr.setUserData(data);
                c.call();
            }
        }, eventTypes);

        assertTrue("no events returned", events.length > 0);
        for (int i = 0; i < events.length; i++) {
            assertEquals("Wrong user data", data, events[i].getUserData());
        }
    }

}
