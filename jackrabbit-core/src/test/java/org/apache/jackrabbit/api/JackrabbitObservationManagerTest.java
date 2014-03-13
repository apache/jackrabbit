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
package org.apache.jackrabbit.api;

import static javax.jcr.observation.Event.NODE_ADDED;

import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.api.observation.JackrabbitEventFilter;
import org.apache.jackrabbit.api.observation.JackrabbitObservationManager;
import org.apache.jackrabbit.test.api.observation.AbstractObservationTest;
import org.apache.jackrabbit.test.api.observation.EventResult;

public class JackrabbitObservationManagerTest extends AbstractObservationTest {

    public void testImplementsJackrabbitObservationManager() throws RepositoryException {
        assertTrue(obsMgr instanceof JackrabbitObservationManager);
    }

    public void testDisjunctPaths() throws ExecutionException, InterruptedException, RepositoryException {
        JackrabbitObservationManager oManager = (JackrabbitObservationManager) obsMgr;
        EventResult listener = new EventResult(log);
        JackrabbitEventFilter filter = new JackrabbitEventFilter()
                .setAdditionalPaths('/' + testPath + "/a", '/' + testPath + "/x")
                .setEventTypes(NODE_ADDED);
        oManager.addEventListener(listener, filter);
        try {
            Node b = testRootNode.addNode("a").addNode("b");
            b.addNode("c");
            Node y = testRootNode.addNode("x").addNode("y");
            y.addNode("z");
            testRootNode.getSession().save();

            Event[] added = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
            checkNodeAdded(added, new String[] {"a/b", "x/y"}, null);
        } finally {
            oManager.removeEventListener(listener);
        }
    }

}
