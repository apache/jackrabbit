/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.*;

/**
 * Test cases for <code>Item.getAncestor(int)</code>.
 *
 * @test
 * @sources GetAncestorTest.java
 * @executeClass org.apache.jackrabbit.test.api.GetAncestorTest
 * @keywords level1
 */
public class GetAncestorTest extends AbstractJCRTest {

    protected Item item;

    protected void setUp() throws Exception {
        super.setUp();
        item = helper.getReadOnlySession().getRootNode();
    }

    /**
     * Test if the ancestor at depth = n, where n is the depth of this <code>Item</code>,
     * returns this <code>Item</code> itself.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfItemDepth() throws RepositoryException {
        Item itemAtDepth = item.getAncestor(item.getDepth());
        assertSame("The ancestor of depth = n, where n is the depth of this " +
                "Item must be the item itself.", item, itemAtDepth);
    }

    /**
     * Test if getting the ancestor of depth = n, where n is greater than depth
     * of this <code>Item</code>, throws an <code>ItemNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfGreaterDepth() throws RepositoryException {
        try {
            int greaterDepth = item.getDepth()+1;
            item.getAncestor(greaterDepth);
            fail("Getting ancestor of depth n, where n is greater than depth of" +
                    "this Item must throw an ItemNotFoundException");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Test if getting the ancestor of negative depth throws an
     * <code>ItemNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfNegativeDepth() throws RepositoryException {
        try {
            item.getAncestor(-1);
            fail("Getting ancestor of depth < 0 must throw an ItemNotFoundException.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }
}