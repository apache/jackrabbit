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
package org.apache.jackrabbit.chain.test;

import javax.jcr.Node;

import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.cli.JcrParser;

/**
 * Command line interfaces tests
 */
public class CliTest extends AbstractChainTest
{
    JcrParser parser = new JcrParser();

    Node testNode;

    /** execute the given command */
    private void execute(String input) throws Exception
    {
        parser.parse(input);

        // populate ctx
        parser.populateContext(ctx);

        // Execute command
        long start = System.currentTimeMillis();
        parser.getCommand().execute(ctx);
        long elapsed = System.currentTimeMillis() - start;

        // depopulate ctx
        parser.dePopulateContext(ctx);
    }

    public void testAddMixin() throws Exception
    {
        execute("addmixin test mix:referenceable");
        assertTrue(testNode.isNodeType("mix:referenceable"));
    }

    public void testAddNode() throws Exception
    {
        CtxHelper.setCurrentNode(ctx, testNode);
        execute("addnode mynode");
        assertTrue(testNode.hasNode("mynode"));
    }

    public void testCopy() throws Exception
    {
        getRoot().save();
        execute("copy test /test2");
        assertTrue(getRoot().hasNode("test2"));
    }

    public void testCd() throws Exception
    {
        execute("cd test");
        assertTrue(CtxHelper.getCurrentNode(ctx).isSame(testNode));
    }

    public void testMove() throws Exception
    {
        execute("move test /test2");
        assertFalse(getRoot().hasNode("test"));
        assertTrue(getRoot().hasNode("test2"));
    }

    public void testRefresh() throws Exception
    {
        testNode.remove();
        assertFalse(getRoot().hasNode("test"));
        execute("refresh");
        assertTrue(getRoot().hasNode("test"));
    }

    public void testRemoveItem() throws Exception
    {
        assertTrue(getRoot().hasNode("test"));
        execute("remove test");
        assertFalse(getRoot().hasNode("test"));
    }

    public void testRemoveItems() throws Exception
    {
        getRoot().addNode("test2");
        assertTrue(getRoot().hasNode("test"));
        assertTrue(getRoot().hasNode("test2"));
        execute("removeitems test*");
        assertFalse(getRoot().hasNode("test"));
        assertFalse(getRoot().hasNode("test2"));
    }

    public void testRemoveMixin() throws Exception
    {
        testNode.addMixin("mix:referenceable");
        assertTrue(testNode.isNodeType("mix:referenceable"));
        execute("removemixin test mix:referenceable");
        assertFalse(testNode.isNodeType("mix:referenceable"));
    }

    public void testRename() throws Exception
    {
        execute("rename test test2");
        assertFalse(getRoot().hasNode("test"));
        assertTrue(getRoot().hasNode("test2"));
    }

    public void testSetMultivalueProperty() throws Exception
    {
        // Comma separated
        execute("setmultivalueproperty multiprop \"prop1,prop2\" -regexp ,");
        assertTrue(getRoot().hasProperty("multiprop"));
        assertTrue(getRoot().getProperty("multiprop").getValues()[0]
            .getString().equals("prop1"));
        assertTrue(getRoot().getProperty("multiprop").getValues()[1]
            .getString().equals("prop2"));

        // Semicolon separated
        execute("setmultivalueproperty multiprop \"prop1;prop2\" -regexp ;");
        assertTrue(getRoot().hasProperty("multiprop"));
        assertTrue(getRoot().getProperty("multiprop").getValues()[0]
            .getString().equals("prop1"));
        assertTrue(getRoot().getProperty("multiprop").getValues()[1]
            .getString().equals("prop2"));
    }

    public void testSetProperty() throws Exception
    {
        execute("setproperty myprop myvalue");
        assertFalse(getRoot().hasNode("myprop"));
        assertTrue(getRoot().getProperty("myprop").getValue().getString()
            .equals("myvalue"));
    }

    /**
     * @inheritDoc
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        testNode = CtxHelper.getCurrentNode(ctx).addNode("test");
    }
}
