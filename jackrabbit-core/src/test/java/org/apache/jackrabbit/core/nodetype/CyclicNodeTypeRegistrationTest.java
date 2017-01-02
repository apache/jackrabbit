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

import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import java.util.Collection;
import java.util.LinkedList;

/**
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
    private Collection<QNodeTypeDefinition> ntDefCollection;

    /**
     * The name factory
     */
    private NameFactory nameFactory;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        //isReadOnly = true;
        super.setUp();

        session = getHelper().getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
        nameFactory = NameFactoryImpl.getInstance();

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
            session = null;
        }
        ntreg = null;
        ntDefCollection = null;
        nameFactory = null;
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
        final QNodeTypeDefinitionBuilder foo = new QNodeTypeDefinitionBuilder();
        foo.setName(nameFactory.create("", "foo"));
        foo.setSupertypes(new Name[]{NameConstants.NT_BASE});

        final QNodeTypeDefinitionBuilder bar = new QNodeTypeDefinitionBuilder();
        bar.setName(nameFactory.create("", "bar"));
        bar.setSupertypes(new Name[]{NameConstants.NT_BASE});

        QNodeDefinitionBuilder myBarInFoo = new QNodeDefinitionBuilder();
        myBarInFoo.setRequiredPrimaryTypes(new Name[]{bar.getName()});
        myBarInFoo.setName(nameFactory.create("", "myBarInFoo"));
        myBarInFoo.setDeclaringNodeType(foo.getName());

        QNodeDefinitionBuilder myFooInBar = new QNodeDefinitionBuilder();
        myFooInBar.setRequiredPrimaryTypes(new Name[]{foo.getName()});
        myFooInBar.setName(nameFactory.create("", "myFooInBar"));
        myFooInBar.setDeclaringNodeType(bar.getName());

        foo.setChildNodeDefs(new QNodeDefinition[]{myBarInFoo.build()});
        bar.setChildNodeDefs(new QNodeDefinition[]{myFooInBar.build()});
        ntDefCollection = new LinkedList<QNodeTypeDefinition>();
        ntDefCollection.add(foo.build());
        ntDefCollection.add(bar.build());

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
        final QNodeTypeDefinitionBuilder foo = new QNodeTypeDefinitionBuilder();
        foo.setName(nameFactory.create("", "foo"));
        foo.setSupertypes(new Name[]{NameConstants.NT_BASE});


        QNodeDefinitionBuilder myBarInFoo = new QNodeDefinitionBuilder();
        myBarInFoo.setRequiredPrimaryTypes(new Name[]{nameFactory.create("", "I_am_an_invalid_required_primary_type")});
        myBarInFoo.setName(nameFactory.create("", "myNTInFoo"));
        myBarInFoo.setDeclaringNodeType(foo.getName());

        foo.setChildNodeDefs(new QNodeDefinition[]{myBarInFoo.build()});
        ntDefCollection = new LinkedList<QNodeTypeDefinition>();
        ntDefCollection.add(foo.build());

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

        final QNodeTypeDefinitionBuilder folder = new QNodeTypeDefinitionBuilder();
        folder.setName(nameFactory.create("", "Folder"));

        final QNodeTypeDefinitionBuilder cmsObject = new QNodeTypeDefinitionBuilder();
        cmsObject.setName(nameFactory.create("", "CmsObject"));
        cmsObject.setSupertypes(new Name[]{NameConstants.NT_BASE});

        QNodeDefinitionBuilder parentFolder = new QNodeDefinitionBuilder();
        parentFolder.setRequiredPrimaryTypes(new Name[]{folder.getName()});
        parentFolder.setName(nameFactory.create("", "parentFolder"));
        parentFolder.setDeclaringNodeType(cmsObject.getName());
        cmsObject.setChildNodeDefs(new QNodeDefinition[]{parentFolder.build()});


        final QNodeTypeDefinitionBuilder document = new QNodeTypeDefinitionBuilder();
        document.setName(nameFactory.create("", "Document"));
        document.setSupertypes(new Name[]{cmsObject.getName()});
        QPropertyDefinitionBuilder sizeProp = new QPropertyDefinitionBuilder();
        sizeProp.setName(nameFactory.create("", "size"));
        sizeProp.setRequiredType(PropertyType.LONG);
        sizeProp.setDeclaringNodeType(document.getName());
        document.setPropertyDefs(new QPropertyDefinition[]{sizeProp.build()});


        folder.setSupertypes(new Name[]{cmsObject.getName()});

        QNodeDefinitionBuilder folders = new QNodeDefinitionBuilder();
        folders.setRequiredPrimaryTypes(new Name[]{folder.getName()});
        folders.setName(nameFactory.create("", "folders"));
        folders.setDeclaringNodeType(folder.getName());

        QNodeDefinitionBuilder documents = new QNodeDefinitionBuilder();
        documents.setRequiredPrimaryTypes(new Name[]{document.getName()});
        documents.setName(nameFactory.create("", "documents"));
        documents.setDeclaringNodeType(folder.getName());

        folder.setChildNodeDefs(new QNodeDefinition[]{
                folders.build(), documents.build()});
        ntDefCollection = new LinkedList<QNodeTypeDefinition>();
        ntDefCollection.add(folder.build());
        ntDefCollection.add(document.build());
        ntDefCollection.add(cmsObject.build());

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
