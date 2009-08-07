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

import javax.jcr.version.Version;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.InvalidItemStateException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.util.Arrays;

/**
 * <code>CheckinTest</code> covers tests related to {@link javax.jcr.Node#checkin()}.
 *
 * @test
 * @sources CheckinTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.CheckinTest
 * @keywords versioning
 */
public class CheckinTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();

        versionableNode.checkout();
    }

    /**
     * Test if Node.isCheckedOut() return false after calling Node.checkin()
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testIsCheckedOut() throws RepositoryException {
        versionableNode.checkin();
        assertTrue("After calling Node.checkin() on a versionable node N, N.isCheckedOut() must return false", versionableNode.isCheckedOut() == false);
    }

    /**
     * Test if the node's jcr:predecessors property contains an empty value array
     * after checkin.
     *
     * @throws RepositoryException
     */
    public void testCheckinRemovesPredecessorProperty() throws RepositoryException {

        versionableNode.checkin();
        Value[] predecessorsValue = versionableNode.getProperty(jcrPredecessors).getValues();

        assertTrue("Checkin must set the node's jcr:predecessors property to the empty array", predecessorsValue.length == 0);
    }

    /**
     * Test if the nodes jcr:predecessors property is copied to the new version
     * on Node.checkin().
     *
     * @throws RepositoryException
     */
    public void testPredecessorIsCopiedToNewVersion() throws RepositoryException {

        Value[] nPredecessorsValue = versionableNode.getProperty(jcrPredecessors).getValues();

        Version v = versionableNode.checkin();
        Value[] vPredecessorsValue = v.getProperty(jcrPredecessors).getValues();

        assertEquals("The versionable checked-out node's jcr:predecessors property is copied to the new version on checkin.", Arrays.asList(nPredecessorsValue), Arrays.asList(vPredecessorsValue));
    }

    /**
     * Test if Node.checkin() on a checked-in node has no effect.
     *
     * @throws RepositoryException
     */
    public void testMultipleCheckinHasNoEffect() throws RepositoryException {

        Version v = versionableNode.checkin();
        try {
            Version v2 = versionableNode.checkin();

            assertTrue("Calling checkin() on a node that is already checked-in must not have an effect.", v.isSame(v2));
        } catch (RepositoryException e) {
            fail("Calling checkin() on a node that is already checked-in must not throw an exception.");
        }
    }

    /**
     * Test if versionable node N's jcr:baseVersion property is set to refer to
     * the new version after checkin.
     *
     * @throws RepositoryException
     */
    public void testBaseVersionAfterCheckin() throws RepositoryException {
        Version v = versionableNode.checkin();
        Value baseVersionRef = versionableNode.getProperty(jcrBaseVersion).getValue();

        assertEquals("Checked-in node's jcr:baseVersion property is set to refer to the version created on checkin.", superuser.getValueFactory().createValue(v), baseVersionRef);
    }

    /**
     * Test if Node.checkin() throws InvalidItemStateException if the node
     * has unsaved changes pending.
     *
     * @throws RepositoryException
     */
    public void testCheckinWithPendingChanges() throws RepositoryException {
        try {
            // modify node without calling save()
            versionableNode.setProperty(propertyName1, propertyValue);
            versionableNode.checkin();

            fail("InvalidItemStateException must be thrown on attempt to checkin a node having any unsaved changes pending.");
        } catch (InvalidItemStateException e) {
            // ok
        }
    }

    /**
     * Test if Node.isCheckedOut() returns false after Node.checkin().
     *
     * @throws RepositoryException
     */
    public void testIsNotCheckedOut() throws RepositoryException {
        versionableNode.checkin();
        boolean isCheckedOut = versionableNode.isCheckedOut();

        assertFalse("Node.isCheckedOut() must return false after Node.checkin().", isCheckedOut);
    }

    /**
     * Test if Node.checkin() adds another version to the VersionHistory
     *
     * @throws RepositoryException
     */
    public void testCheckinCreatesNewVersion() throws RepositoryException {

        long initialNumberOfVersions = getNumberOfVersions(versionableNode.getVersionHistory());
        versionableNode.checkin();
        long numberOfVersions = getNumberOfVersions(versionableNode.getVersionHistory());

        assertTrue("Checkin must create a new Version in the VersionHistory.", numberOfVersions == initialNumberOfVersions + 1);
    }

    /**
     * Test calling Node.checkin() on a non-versionable node.
     *
     * @throws RepositoryException
     */
    public void testCheckinNonVersionableNode() throws RepositoryException {
        try {
            nonVersionableNode.checkin();
            fail("Node.checkin() on a non versionable node must throw UnsupportedRepositoryOperationException");
        } catch (UnsupportedRepositoryOperationException e) {
            //success
        }
    }
}