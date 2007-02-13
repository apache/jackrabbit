/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.test;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.api.NamespaceRegistryTest;
import org.apache.jackrabbit.test.api.SessionTest;
import org.apache.jackrabbit.test.api.SessionUUIDTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopySameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest;
import org.apache.jackrabbit.test.api.DocumentViewImportTest;
import org.apache.jackrabbit.test.api.SerializationTest;
import org.apache.jackrabbit.test.api.ValueFactoryTest;
import org.apache.jackrabbit.test.api.CheckPermissionTest;

/**
 * <code>TestAllPropertyRead</code>...
 */
public class TestGeneralWrite extends TestCase {

    private static Logger log = LoggerFactory.getLogger(TestGeneralWrite.class);

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {

        TestSuite suite = new TestSuite("javax.jcr General-Write");

        suite.addTestSuite(CheckPermissionTest.class);

        suite.addTestSuite(NamespaceRegistryTest.class);

        suite.addTestSuite(SessionTest.class);
        suite.addTestSuite(SessionUUIDTest.class);

        suite.addTestSuite(WorkspaceCloneReferenceableTest.class);
        suite.addTestSuite(WorkspaceCloneSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCloneTest.class);
        suite.addTestSuite(WorkspaceCloneVersionableTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesReferenceableTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesVersionableTest.class);
        suite.addTestSuite(WorkspaceCopyReferenceableTest.class);
        suite.addTestSuite(WorkspaceCopySameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCopyTest.class);
        suite.addTestSuite(WorkspaceCopyVersionableTest.class);
        suite.addTestSuite(WorkspaceMoveReferenceableTest.class);
        suite.addTestSuite(WorkspaceMoveSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceMoveTest.class);
        suite.addTestSuite(WorkspaceMoveVersionableTest.class);

        suite.addTestSuite(DocumentViewImportTest.class);
        suite.addTestSuite(SerializationTest.class);

        suite.addTestSuite(ValueFactoryTest.class);

        return suite;
    }
}
