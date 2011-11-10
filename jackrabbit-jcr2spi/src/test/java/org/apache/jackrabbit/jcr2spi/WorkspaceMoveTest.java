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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.ItemExistsException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>WorkspaceMoveTest</code>...
 */
public class WorkspaceMoveTest extends MoveTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected boolean isSessionMove() {
        return false;
    }

    /**
     * Tries to move a node using to a location where a property already exists
     * with same name.
     * <br/> <br/>
     * With JCR 1.0 this should throw an <code>{@link javax.jcr.ItemExistsException}</code>.
     * With JCR 2.0 the support for same-named property and node is optional and
     * the expected behaviour depends on the
     * {@link Repository#OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED} descriptor.
     */
    @Override
    public void testMovePropertyExists() throws RepositoryException, NotExecutableException {
        // try to create a property with the name of the node to be moved
        // to the destination parent
        Property destProperty;
        try {
            destProperty = destParentNode.setProperty(nodeName2, "anyString");
            destParentNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot create property with name '" +nodeName2+ "' and value 'anyString' at move destination.");
        }

        // TODO: fix 2.0 behaviour according to the OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED descriptor
        if ("1.0".equals(getHelper().getRepository().getDescriptor(Repository.SPEC_VERSION_DESC))) {
            try {
                // move the node
                doMove(moveNode.getPath(), destProperty.getPath());
                fail("Moving a node to a location where a property exists must throw ItemExistsException");
            } catch (ItemExistsException e) {
                // ok, works as expected
            }
        } else {
            // JCR 2.0 move the node: same name property and node must be supported
            doMove(moveNode.getPath(), destProperty.getPath());
        }
    }

    public void testMoveTransientPropertyExists() throws RepositoryException, NotExecutableException {
        // try to create a property with the name of the node to be moved
        // to the destination parent
        Property destProperty;
        try {
            destProperty = destParentNode.setProperty(nodeName2, "anyString");
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot create property with name '" +nodeName2+ "' and value 'anyString' at move destination.");
        }

        // TODO: fix 2.0 behaviour according to the OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED descriptor
        // workspace-move the node (must succeed)
        doMove(moveNode.getPath(), destProperty.getPath());
         if ("1.0".equals(getHelper().getRepository().getDescriptor(Repository.SPEC_VERSION_DESC))) {
             try {
                 // saving transient new property must fail
                 destParentNode.save();
                 fail("Saving new transient property must fail");
            } catch (RepositoryException e) {
                // ok.
             }
         } else {
             // JCR 2.0: saving must succeed.
             destParentNode.save();
         }
    }
}