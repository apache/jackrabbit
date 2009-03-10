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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.ItemExistsException;

/**
 * <code>WorkspaceMoveTest</code>...
 */
public class WorkspaceMoveTest extends MoveTest {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected boolean isSessionMove() {
        return false;
    }


    /**
     * Tries to move a node using to a location where a property already exists
     * with same name.
     * <br/> <br/>
     * This should throw an <code>{@link javax.jcr.ItemExistsException}</code>.
     */
    public void testMovePropertyExistsException() throws RepositoryException, NotExecutableException {
        // try to create a property with the name of the node to be moved
        // to the destination parent
        Property destProperty;
        try {
            destProperty = destParentNode.setProperty(nodeName2, "anyString");
            destParentNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot create property with name '" +nodeName2+ "' and value 'anyString' at move destination.");
        }

        try {
            // move the node
            doMove(moveNode.getPath(), destProperty.getPath());
            fail("Moving a node to a location where a property exists must throw ItemExistsException");
        } catch (ItemExistsException e) {
            // ok, works as expected
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

        // workspace-move the node (must succeed)
        doMove(moveNode.getPath(), destProperty.getPath());
        try {
            // saving transient new property must fail
            destParentNode.save();
            fail("Saving new transient property must fail");
        } catch (RepositoryException e) {
            // ok.
        }
    }
}