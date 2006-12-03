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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.version.VersionException;

/**
 * <code>SessionMoveVersionExceptionTest</code> contains tests dealing with
 * moving nodes using {@link javax.jcr.Session#move(String, String)}.
 *
 * @test
 * @sources SessionMoveVersionExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.SessionMoveVersionExceptionTest
 * @keywords versioning
 */
public class SessionMoveVersionExceptionTest extends AbstractVersionTest {

    /**
     * Tries to move a node using {@link javax.jcr.Session#move(String, String)}
     * where the source parent is checked in. This should throw an {@link
     * javax.jcr.version.VersionException}.
     */
    public void testSessionMoveSourceCheckedInVersionException() throws RepositoryException {
        // add a node under a versionable node
        Node movingNode = versionableNode.addNode(nodeName1, nonVersionableNodeType.getName());
        versionableNode.save();
        // check the parent node in
        versionableNode.checkin();
        try {
            // try to move the sub node this should throw an VersionException
            // either instantly or upon save()
            superuser.move(movingNode.getPath(), nonVersionableNode.getPath());
            superuser.save();
            fail("Moving a node using Session.move() where parent node is " +
                    "versionable and checked in should throw a VersionException!");
        } catch (VersionException e) {
            // ok, works as expected
        }
    }

    /**
     * Tries to move a node using {@link javax.jcr.Session#move(String, String)}
     * where the destination parent is checked in. This should throw an {@link
     * javax.jcr.version.VersionException}.
     */
    public void testSessionMoveDestCheckedInVersionException() throws RepositoryException {
        // make sure versionable node is checked in
        versionableNode.checkin();

        try {
            // try to move the sub node this should throw an VersionException either instantly or upon save()
            superuser.move(nonVersionableNode.getPath(), versionableNode.getPath() + "/" + nodeName1);
            superuser.save();
            fail("Moving a node using Session.save() where destination parent " +
                    "node is versionable and checked in should throw a VersionException!");
        } catch (VersionException e) {
            // ok, works as expected
        }
    }
}
