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

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that includes all testcases for the package
 * <code>javax.jcr</code>.
 */
public class TestAll extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this
     * package.
     *
     * @return a <code>Test</code> suite that executes all tests inside this
     *         package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("javax.jcr tests");

        // ADD TEST CLASSES HERE:

        // level 1 tests
        suite.addTestSuite(RootNodeTest.class);
        suite.addTestSuite(NodeReadMethodsTest.class);
        suite.addTestSuite(PropertyTypeTest.class);
        suite.addTestSuite(NodeDiscoveringNodeTypesTest.class);

        suite.addTestSuite(BinaryPropertyTest.class);
        suite.addTestSuite(BooleanPropertyTest.class);
        suite.addTestSuite(DatePropertyTest.class);
        suite.addTestSuite(DoublePropertyTest.class);
        suite.addTestSuite(LongPropertyTest.class);
        suite.addTestSuite(NamePropertyTest.class);
        suite.addTestSuite(PathPropertyTest.class);
        suite.addTestSuite(ReferencePropertyTest.class);
        suite.addTestSuite(StringPropertyTest.class);
        suite.addTestSuite(UndefinedPropertyTest.class);

        suite.addTestSuite(NamespaceRegistryReadMethodsTest.class);
        suite.addTestSuite(NamespaceRemappingTest.class);
        suite.addTestSuite(NodeIteratorTest.class);
        suite.addTestSuite(PropertyReadMethodsTest.class);
        suite.addTestSuite(RepositoryDescriptorTest.class);
        suite.addTestSuite(SessionReadMethodsTest.class);
        suite.addTestSuite(WorkspaceReadMethodsTest.class);
        suite.addTestSuite(ReferenceableRootNodesTest.class);

        suite.addTestSuite(ExportSysViewTest.class);
        suite.addTestSuite(ExportDocViewTest.class);

        // level 2 tests
        suite.addTestSuite(AddNodeTest.class);
        suite.addTestSuite(NamespaceRegistryTest.class);
        suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(SessionTest.class);
        suite.addTestSuite(SessionUUIDTest.class);
        suite.addTestSuite(NodeTest.class);
        suite.addTestSuite(NodeUUIDTest.class);
        suite.addTestSuite(NodeOrderableChildNodesTest.class);
        suite.addTestSuite(PropertyTest.class);

        suite.addTestSuite(SetValueBinaryTest.class);
        suite.addTestSuite(SetValueBooleanTest.class);
        suite.addTestSuite(SetValueDateTest.class);
        suite.addTestSuite(SetValueDoubleTest.class);
        suite.addTestSuite(SetValueLongTest.class);
        suite.addTestSuite(SetValueReferenceTest.class);
        suite.addTestSuite(SetValueStringTest.class);
        suite.addTestSuite(SetValueConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetValueValueFormatExceptionTest.class);
        suite.addTestSuite(SetValueVersionExceptionTest.class);

        suite.addTestSuite(SetPropertyBooleanTest.class);
        suite.addTestSuite(SetPropertyCalendarTest.class);
        suite.addTestSuite(SetPropertyDoubleTest.class);
        suite.addTestSuite(SetPropertyInputStreamTest.class);
        suite.addTestSuite(SetPropertyLongTest.class);
        suite.addTestSuite(SetPropertyNodeTest.class);
        suite.addTestSuite(SetPropertyStringTest.class);
        suite.addTestSuite(SetPropertyValueTest.class);
        suite.addTestSuite(SetPropertyConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetPropertyAssumeTypeTest.class);

        suite.addTestSuite(NodeItemIsModifiedTest.class);
        suite.addTestSuite(NodeItemIsNewTest.class);
        suite.addTestSuite(PropertyItemIsModifiedTest.class);
        suite.addTestSuite(PropertyItemIsNewTest.class);

        suite.addTestSuite(NodeAddMixinTest.class);
        suite.addTestSuite(NodeCanAddMixinTest.class);
        suite.addTestSuite(NodeRemoveMixinTest.class);

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

        suite.addTestSuite(RepositoryLoginTest.class);
        suite.addTestSuite(ImpersonateTest.class);
        suite.addTestSuite(CheckPermissionTest.class);

        suite.addTestSuite(DocumentViewImportTest.class);
        suite.addTestSuite(SerializationTest.class);

        suite.addTestSuite(ValueFactoryTest.class);

        return suite;
    }
}