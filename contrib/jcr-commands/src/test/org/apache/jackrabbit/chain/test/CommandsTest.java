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

import java.io.File;
import java.util.Iterator;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.collections.IteratorUtils;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Commands tests
 */
public class CommandsTest extends AbstractChainTest
{

    /**
     * Tests AddNode
     * 
     * @throws Exception
     */
    public void testAddNode() throws Exception
    {
        this.testAddNode(catalog.getCommand("addTestNode"));
    }

    /**
     * Tests AddNode with context attributes
     * 
     * @throws Exception
     */
    public void testAddNodeWithKey() throws Exception
    {
        this.testAddNode(catalog.getCommand("addTestNodeWithKey"));
    }

    private void testAddNode(Command cmd) throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        assertFalse(n.hasNode("test"));
        cmd.execute(ctx);
        assertTrue(n.hasNode("test"));
    }

    /**
     * Tests ClearWorkspace
     * 
     * @throws Exception
     */
    public void testClearWorkspace() throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        assertFalse(n.hasNode("test"));
        catalog.getCommand("addTestNode").execute(ctx);
        assertTrue(n.hasNode("test"));
        clear();
        assertFalse(n.hasNode("test"));
    }

    /**
     * Tests ClearWorkspace
     * 
     * @throws Exception
     */
    public void testCollect() throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        // Add node
        assertFalse(n.hasNode("test"));
        addTestNode();
        assertTrue(n.hasNode("test"));
        // Collect
        assertFalse(ctx.get("children") != null);
        catalog.getCommand("collect").execute(ctx);
        assertTrue(ctx.get("children") != null);
        assertTrue(ctx.get("children") != null);
        int length = IteratorUtils.toArray((Iterator) ctx.get("children")).length;
        assertTrue(length == 2);
    }

    /**
     * Tests CD
     * 
     * @throws Exception
     */
    public void testCd() throws Exception
    {
        this.testCd(catalog.getCommand("cd"));
    }

    /**
     * Tests CD with key
     * 
     * @throws Exception
     */
    public void testCdWithKey() throws Exception
    {
        this.testCd(catalog.getCommand("cdWithKey"));
    }

    private void testCd(Command cmd) throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        addTestNode();
        assertTrue(n.getPath().equals("/"));
        // cd
        cmd.execute(ctx);
        n = CtxHelper.getCurrentNode(ctx);
        assertTrue(n.getPath().equals("/test"));
    }

    private void clear() throws Exception
    {
        catalog.getCommand("clear").execute(ctx);
    }

    /**
     * Tests Export Doc
     * 
     * @throws Exception
     */
    public void testExportDoc() throws Exception
    {
        testExport(catalog.getCommand("exportDoc"));
    }

    /**
     * Tests Export Doc
     * 
     * @throws Exception
     */
    public void testExportDocWithKey() throws Exception
    {
        testExport(catalog.getCommand("exportDocWithKey"));
    }

    /**
     * Tests Export Sys
     * 
     * @throws Exception
     */
    public void testExportSys() throws Exception
    {
        testExport(catalog.getCommand("exportSys"));
    }

    /**
     * Tests Export Sys with Key
     * 
     * @throws Exception
     */
    public void testExportSysWithKey() throws Exception
    {
        testExport(catalog.getCommand("exportSysWithKey"));
    }

    private void testExport(Command cmd) throws Exception
    {
        File f = new File("applications/test/export.xml");
        if (f.exists())
        {
            f.delete();
        }
        assertFalse(f.exists());
        Node n = CtxHelper.getCurrentNode(ctx);
        CtxHelper.setCurrentNode(ctx, n.addNode("test"));
        cmd.execute(ctx);
        assertTrue(f.exists());
        f.delete();
        assertFalse(f.exists());
    }

    /**
     * Tests read value
     * 
     * @throws Exception
     */
    public void testRead() throws Exception
    {
        catalog.getCommand("read").execute(ctx);
        assertTrue(ctx.get("value").equals("rep:root"));
    }

    /**
     * Tests read value with key
     * 
     * @throws Exception
     */
    public void testReadWithKey() throws Exception
    {
        catalog.getCommand("readWithKey").execute(ctx);
        assertTrue(ctx.get("value").equals("rep:root"));
    }

    /**
     * Tests remove
     * 
     * @throws Exception
     */
    public void testRemove() throws Exception
    {
        testRemove(catalog.getCommand("remove"));
    }

    /**
     * Tests remove with key
     * 
     * @throws Exception
     */
    public void testRemoveWithKey() throws Exception
    {
        testRemove(catalog.getCommand("removeWithKey"));
    }

    /**
     * Tests remove items
     * 
     * @throws Exception
     */
    public void testRemoveItems() throws Exception
    {
        testRemove(catalog.getCommand("removeItems"));
    }

    /**
     * Tests remove items with key
     * 
     * @throws Exception
     */
    public void testRemoveItemsWithKey() throws Exception
    {
        testRemove(catalog.getCommand("removeItemsWithKey"));
    }

    private void testRemove(Command cmd) throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        assertFalse(n.hasNode("test"));
        n.addNode("test");
        assertTrue(n.hasNode("test"));
        cmd.execute(ctx);
        assertFalse(n.hasNode("test"));
    }

    private void testSetProp(Command cmd) throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        assertFalse(n.hasProperty("testProp"));
        cmd.execute(ctx);
        assertTrue(n.hasProperty("testProp"));
        // TODO: .trim() is only for xxxfromfile, remove it
        assertTrue(n.getProperty("testProp").getValue().getString().trim()
            .equals("testValue"));
    }

    /**
     * Tests set property
     * 
     * @throws Exception
     */
    public void testSetProp() throws Exception
    {
        testSetProp(catalog.getCommand("setProp"));
    }

    /**
     * Tests remove items with key
     * 
     * @throws Exception
     */
    public void testSetPropWithKey() throws Exception
    {
        testSetProp(catalog.getCommand("setPropWithKey"));
    }

    /**
     * Tests set property from file
     * 
     * @throws Exception
     */
    public void testSetPropFromFile() throws Exception
    {
        testSetProp(catalog.getCommand("setPropFromFile"));
    }

    /**
     * Tests remove items with key
     * 
     * @throws Exception
     */
    public void testSetPropFromFileWithKey() throws Exception
    {
        testSetProp(catalog.getCommand("setPropFromFileWithKey"));
    }

    /**
     * Tests set property from file
     * 
     * @throws Exception
     */
    public void testSetMultiProp() throws Exception
    {
        testSetMultiProp(catalog.getCommand("setMultiProp"));
    }

    /**
     * Tests remove items with key
     * 
     * @throws Exception
     */
    public void testSetMultiWithKey() throws Exception
    {
        testSetMultiProp(catalog.getCommand("setMultiPropWithKey"));
    }

    private void testSetMultiProp(Command cmd) throws Exception
    {
        Node n = CtxHelper.getCurrentNode(ctx);
        assertFalse(n.hasProperty("testProp"));
        cmd.execute(ctx);
        assertTrue(n.hasProperty("testProp"));
        assertTrue(n.getProperty("testProp").getValues()[0].getString().equals(
            "testValue"));
        assertTrue(n.getProperty("testProp").getValues()[1].getString().equals(
            "testValue1"));
    }

}
