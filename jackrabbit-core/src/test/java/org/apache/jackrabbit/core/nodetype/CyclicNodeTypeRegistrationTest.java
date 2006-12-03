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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @test
 * @sources CyclicNodeTypeRegistrationTest
 * @executeClass org.apache.jackrabbit.test.api.nodetype.CyclicNodeTypeRegistrationTest
 * @keywords level1
 */
public class CyclicNodeTypeRegistrationTest extends AbstractJCRTest {
    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * The node type manager we use for the tests
     */
    private NodeTypeManager manager;

    /**
     * The node type registry we use for the tests
     */
    private NodeTypeRegistry ntreg;

    /**
     * The cyclic dependent node type definitions we use for the tests
     */
    private Collection ntDefCollection;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        //isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();

        // Get the NodeTypeManager from the Workspace.
        // Note that it must be cast from the generic JCR NodeTypeManager to the
        // Jackrabbit-specific implementation.
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();

        // Acquire the NodeTypeRegistry
        ntreg = ntmgr.getNodeTypeRegistry();


    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }

    /**
     * Tests, if it is possible to register node types with simple
     * cyclic dependencies.
     */
    public void testRegisterCyclicChildNodeTypes() {
        /**
         * Constructs node types with the following structure:
         * [foo]
         * + myBarInFoo (bar)
         *
         * [bar]
         * + myFooInBar (foo)
         */
        final NodeTypeDef foo = new NodeTypeDef();
        foo.setName(new QName("", "foo"));
        foo.setSupertypes(new QName[]{QName.NT_BASE});

        final NodeTypeDef bar = new NodeTypeDef();
        bar.setName(new QName("", "bar"));
        bar.setSupertypes(new QName[]{QName.NT_BASE});

        NodeDefImpl myBarInFoo = new NodeDefImpl();
        myBarInFoo.setRequiredPrimaryTypes(new QName[]{bar.getName()});
        myBarInFoo.setName(new QName("", "myBarInFoo"));
        myBarInFoo.setDeclaringNodeType(foo.getName());

        NodeDefImpl myFooInBar = new NodeDefImpl();
        myFooInBar.setRequiredPrimaryTypes(new QName[]{foo.getName()});
        myFooInBar.setName(new QName("", "myFooInBar"));
        myFooInBar.setDeclaringNodeType(bar.getName());

        foo.setChildNodeDefs(new NodeDefImpl[]{myBarInFoo});
        bar.setChildNodeDefs(new NodeDefImpl[]{myFooInBar});
        ntDefCollection = new LinkedList();
        ntDefCollection.add(foo);
        ntDefCollection.add(bar);

        try {
            ntreg.registerNodeTypes(ntDefCollection);
        } catch (InvalidNodeTypeDefException e) {
            assertFalse(e.getMessage(), true);
            e.printStackTrace();
        } catch (RepositoryException e) {
            assertFalse(e.getMessage(), true);
            e.printStackTrace();
        }
        boolean allNTsAreRegistered = ntreg.isRegistered(foo.getName()) && ntreg.isRegistered(bar.getName());
        assertTrue(allNTsAreRegistered);

    }

    /**
     * A simple check, if a missing node type is found
     */
    public void testRegisterSimpleMissingNodeTypes() {
        /**
         * Constructs node types with the following structure:
         * [foo]
         * + myNTInFoo (I_am_an_invalid_required_primary_type)
         *
         */
        final NodeTypeDef foo = new NodeTypeDef();
        foo.setName(new QName("", "foo"));
        foo.setSupertypes(new QName[]{QName.NT_BASE});


        NodeDefImpl myBarInFoo = new NodeDefImpl();
        myBarInFoo.setRequiredPrimaryTypes(new QName[]{new QName("", "I_am_an_invalid_required_primary_type")});
        myBarInFoo.setName(new QName("", "myNTInFoo"));
        myBarInFoo.setDeclaringNodeType(foo.getName());

        foo.setChildNodeDefs(new NodeDefImpl[]{myBarInFoo});
        ntDefCollection = new LinkedList();
        ntDefCollection.add(foo);

        try {
            ntreg.registerNodeTypes(ntDefCollection);
            assertFalse("Missing node type not found", true);
        } catch (InvalidNodeTypeDefException e) {
            assertTrue(true);

        } catch (RepositoryException e) {
            assertFalse("Wrong Exception thrown on missing node type.", true);
            e.printStackTrace();
        }
    }

    /**
     * Basically a test of a Graffito use case.
     */
    public void testRegisterCyclicChildNodeTypesAndSupertypes() {
        /**
         * Constructs node types with the following structure:
         * [Folder] > CmsObject
         * + folders (Folder)
         * + documents (Document)
         *
         * [CmsObject]
         * + parentFolder (Folder)
         *
         * [Document] > CmsObject
         * - size (long)
         *
         */

        final NodeTypeDef folder = new NodeTypeDef();
        folder.setName(new QName("", "Folder"));

        final NodeTypeDef cmsObject = new NodeTypeDef();
        cmsObject.setName(new QName("", "CmsObject"));
        cmsObject.setSupertypes(new QName[]{QName.NT_BASE});
        NodeDefImpl parentFolder = new NodeDefImpl();
        parentFolder.setRequiredPrimaryTypes(new QName[]{folder.getName()});
        parentFolder.setName(new QName("", "parentFolder"));
        parentFolder.setDeclaringNodeType(cmsObject.getName());
        cmsObject.setChildNodeDefs(new NodeDefImpl[]{parentFolder});


        final NodeTypeDef document = new NodeTypeDef();
        document.setName(new QName("", "Document"));
        document.setSupertypes(new QName[]{cmsObject.getName()});
        PropDefImpl sizeProp = new PropDefImpl();
        sizeProp.setName(new QName("", "size"));
        sizeProp.setRequiredType(PropertyType.LONG);
        sizeProp.setDeclaringNodeType(document.getName());
        document.setPropertyDefs(new PropDef[]{sizeProp});


        folder.setSupertypes(new QName[]{cmsObject.getName()});

        NodeDefImpl folders = new NodeDefImpl();
        folders.setRequiredPrimaryTypes(new QName[]{folder.getName()});
        folders.setName(new QName("", "folders"));
        folders.setDeclaringNodeType(folder.getName());

        NodeDefImpl documents = new NodeDefImpl();
        documents.setRequiredPrimaryTypes(new QName[]{document.getName()});
        documents.setName(new QName("", "documents"));
        documents.setDeclaringNodeType(folder.getName());

        folder.setChildNodeDefs(new NodeDefImpl[]{folders, documents});
        ntDefCollection = new LinkedList();
        ntDefCollection.add(folder);
        ntDefCollection.add(document);
        ntDefCollection.add(cmsObject);

        try {
            ntreg.registerNodeTypes(ntDefCollection);
        } catch (InvalidNodeTypeDefException e) {
            assertFalse(e.getMessage(), true);
            e.printStackTrace();
        } catch (RepositoryException e) {
            assertFalse(e.getMessage(), true);
            e.printStackTrace();
        }
        boolean allNTsAreRegistered = ntreg.isRegistered(folder.getName()) && ntreg.isRegistered(document.getName()) && ntreg.isRegistered(cmsObject.getName());
        assertTrue(allNTsAreRegistered);
    }
}