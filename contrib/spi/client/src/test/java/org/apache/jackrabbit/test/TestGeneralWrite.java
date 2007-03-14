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
