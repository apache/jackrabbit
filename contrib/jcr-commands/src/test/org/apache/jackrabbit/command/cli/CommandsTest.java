/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.command.cli;

import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.Version;

import junit.framework.TestCase;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Commands Test superclass
 */
public class CommandsTest extends TestCase
{
	/** config */
	protected static String CONFIG = "applications/test/repository.xml";

	/** home */
	protected static String HOME = "applications/test/repository";

	Log log = LogFactory.getLog(CommandsTest.class);

	static
	{
		try
		{
			ConfigParser parser = new ConfigParser();
			parser.parse(JcrClient.class.getResource("command.xml"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/** Context */
	protected Context ctx = new ContextBase();

	/** catalog */
	protected Catalog catalog = CatalogFactoryBase.getInstance().getCatalog();

	protected void setUp() throws Exception
	{
		super.setUp();

		// Start
		ctx.put("config", CONFIG);
		ctx.put("home", HOME);
		catalog.getCommand("startJackrabbitSingleton").execute(ctx);
		assertTrue(CommandHelper.getRepository(ctx) != null);

		// Login
		ctx.put("user", "u");
		ctx.put("password", "p");
		catalog.getCommand("login").execute(ctx);
		assertTrue(CommandHelper.getSession(ctx) != null);

		// clear workspace
		catalog.getCommand("clearWorkspace").execute(ctx);

		// add test node
		ctx.put("relPath", "test");
		catalog.getCommand("addNode").execute(ctx);
		ctx.remove("relPaht");

		CommandHelper.getSession(ctx).save();
	}

	protected Session createNewSession() throws Exception
	{
		Credentials c = new SimpleCredentials("ed", "ed".toCharArray());
		return CommandHelper.getRepository(ctx).login(c);
	}

	protected void tearDown() throws Exception
	{
		super.tearDown();
		// clear workspace
		catalog.getCommand("clearWorkspace").execute(ctx);
		// save
		CommandHelper.getSession(ctx).save();

		// Logout
		catalog.getCommand("logout").execute(ctx);

		ctx.clear();

	}

	protected Node getRoot() throws Exception
	{
		return CommandHelper.getNode(ctx, "/");
	}

	protected Node getTestNode() throws Exception
	{
		return CommandHelper.getNode(ctx, "/test");
	}

	public void testOrderBefore() throws Exception
	{
		CommandHelper.setCurrentNode(ctx, getTestNode());
		Node node1 = getTestNode().addNode("child");
		Node node2 = getTestNode().addNode("child");
		node1.setProperty("pos", 1);
		node2.setProperty("pos", 2);
		NodeIterator iter = getTestNode().getNodes("child");
		assertTrue(iter.nextNode().isSame(node1));
		assertTrue(iter.nextNode().isSame(node2));
		ctx.put("srcChild", "child[2]");
		ctx.put("destChild", "child[1]");
		catalog.getCommand("orderBefore").execute(ctx);
		iter = getTestNode().getNodes("child");
		assertTrue(iter.nextNode().isSame(node2));
		assertTrue(iter.nextNode().isSame(node1));
	}

	public void testMove() throws Exception
	{
		ctx.put("srcAbsPath", getTestNode().getPath());
		ctx.put("destAbsPath", "/test2");
		catalog.getCommand("move").execute(ctx);
		assertTrue(CommandHelper.hasNode(ctx, "/test2"));
	}

	public void testReadValue() throws Exception
	{
		getTestNode().setProperty("prop", "val");
		ctx.put("srcPath", getTestNode().getProperty("prop").getPath());
		ctx.put("dest", "myKey");
		catalog.getCommand("readValue").execute(ctx);
		assertTrue(ctx.get("myKey").equals("val"));
	}

	public void testCurrentNode() throws Exception
	{
		ctx.put("path", getTestNode().getPath());
		assertTrue(CommandHelper.getCurrentNode(ctx).getPath().equals("/"));
		catalog.getCommand("currentNode").execute(ctx);
		assertTrue(CommandHelper.getCurrentNode(ctx).getPath().equals(
				getTestNode().getPath()));
	}

	public void testCopy() throws Exception
	{
		ctx.put("srcAbsPath", getTestNode().getPath());
		ctx.put("destAbsPath", "/copy");
		assertFalse(CommandHelper.hasNode(ctx, "/copy"));
		catalog.getCommand("copy").execute(ctx);
		assertTrue(CommandHelper.hasNode(ctx, "/copy"));
	}

	public void testCollect() throws Exception
	{
		Command items = catalog.getCommand("collectItems");

		ctx.put("srcPath", "/");
		ctx.put("depth", "1");
		ctx.put("namePattern", "*");

		assertTrue(ctx.get("collected") == null);
		items.execute(ctx);
		assertTrue(IteratorUtils.toList((Iterator) ctx.get("collected")).size() == 3);

		ctx.put("scrPath", null);
		items.execute(ctx);
		assertTrue(IteratorUtils.toList((Iterator) ctx.get("collected")).size() == 3);

		ctx.put("depth", "2");
		items.execute(ctx);
		assertTrue(IteratorUtils.toList((Iterator) ctx.get("collected")).size() == 7);

		ctx.put("namePattern", null);
		items.execute(ctx);
		assertTrue(IteratorUtils.toList((Iterator) ctx.get("collected")).size() == 7);

		ctx.put("namePattern", "jcr:primaryType");
		items.execute(ctx);
		assertTrue(IteratorUtils.toList((Iterator) ctx.get("collected")).size() == 3);

	}

	public void testRefresh() throws Exception
	{
		Node n = getTestNode();
		assertFalse(n.hasNode("newNode"));
		Session s = createNewSession();
		s.getRootNode().getNode(getTestNode().getName()).addNode("newNode");
		s.save();
		s.logout();
		ctx.put("keepChanges", Boolean.TRUE.toString());
		catalog.getCommand("refresh").execute(ctx);
		assertTrue(n.hasNode("newNode"));
	}

	public void testRemoveItem() throws Exception
	{
		Node n = getTestNode();
		n.addNode("newNode");
		assertTrue(n.hasNode("newNode"));
		ctx.put("path", getTestNode().getPath() + "/newNode");
		catalog.getCommand("removeItem").execute(ctx);
		assertFalse(n.hasNode("newNode"));
	}

	public void testRemoveItems() throws Exception
	{
		Node n = getTestNode();
		n.addNode("newNode1");
		n.addNode("newNode2");
		assertTrue(n.getNodes().getSize() == 2);
		ctx.put("path", getTestNode().getPath());
		ctx.put("pattern", "new*");
		catalog.getCommand("removeItems").execute(ctx);
		assertTrue(n.getNodes().getSize() == 0);
	}

	public void testRename() throws Exception
	{
		Node n = getTestNode();
		CommandHelper.setCurrentNode(ctx, n);
		n.addNode("name1");
		ctx.put("srcPath", "name1");
		ctx.put("destPath", "name2");
		catalog.getCommand("rename").execute(ctx);
		assertTrue(n.hasNode("name2"));
	}

	public void testSaveNode() throws Exception
	{
		Node n = getTestNode();
		CommandHelper.setCurrentNode(ctx, n);
		n.addNode("newNode");
		ctx.put("path", n.getPath());
		catalog.getCommand("save").execute(ctx);
		CommandHelper.getSession(ctx).refresh(false);
		assertTrue(n.hasNode("newNode"));
	}

	public void testSetMultiValueProperty() throws Exception
	{
		Node n = getTestNode();
		CommandHelper.setCurrentNode(ctx, n);
		ctx.put("name", "myprop");
		ctx.put("value", "1,2");
		ctx.put("regExp", ",");
		catalog.getCommand("setMultiValueProperty").execute(ctx);
		assertTrue(n.hasProperty("myprop"));
		assertTrue(n.getProperty("myprop").getValues()[0].getString().equals(
				"1"));
		assertTrue(n.getProperty("myprop").getValues()[1].getString().equals(
				"2"));
	}

	public void testSetProperty() throws Exception
	{
		Node n = getTestNode();
		CommandHelper.setCurrentNode(ctx, n);
		ctx.put("name", "myprop");
		ctx.put("value", "myvalue");
		catalog.getCommand("setProperty").execute(ctx);
		assertTrue(n.hasProperty("myprop"));
		assertTrue(n.getProperty("myprop").getValue().getString().equals(
				"myvalue"));
	}

	public void testAddMixin() throws Exception
	{
		ctx.put("mixin", "mix:referenceable");
		ctx.put("path", getTestNode().getPath());
		catalog.getCommand("addMixin").execute(ctx);
		assertTrue(getTestNode().isNodeType("mix:referenceable"));
	}

	public void testRemoveMixin() throws Exception
	{
		getTestNode().addMixin("mix:referenceable");
		ctx.put("mixin", "mix:referenceable");
		ctx.put("path", getTestNode().getPath());
		catalog.getCommand("removeMixin").execute(ctx);
		assertFalse(getTestNode().isNodeType("mix:referenceable"));
	}

	public void testNamespace() throws Exception
	{
		// ctx.put("prefix", "mycomp");
		// ctx.put("uri", "http://mycomp");
		// catalog.getCommand("registerNamespace").execute(ctx);
		// assertTrue(getTestNode().getSession().getWorkspace()
		// .getNamespaceRegistry().getURI("mycomp")
		// .equals("http://mycomp"));
	}

	public void testCheckin() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.setProperty("prop", "value");
		n.getSession().save();
		ctx.put("path", n.getPath());
		catalog.getCommand("checkin").execute(ctx);
		// root version + checked in
		assertFalse(n.isCheckedOut());
	}

	public void testCheckout() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.getSession().save();
		n.checkin();
		ctx.put("path", n.getPath());
		catalog.getCommand("checkout").execute(ctx);
		assertTrue(n.isCheckedOut());
	}

	public void testRemoveVersion() throws Exception
	{
		// Node n = getTestNode();
		// n.addMixin("mix:versionable");
		// n.getSession().save();
		// n.checkin();
		// ctx.put("path", n.getPath());
		// ctx.put("name", "1.0");
		// catalog.getCommand("removeVersion").execute(ctx);
		// assertTrue(n.getVersionHistory().getAllVersions().getSize() == 1);
	}

	public void testAddVersionLabel() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.getSession().save();
		n.checkin();
		ctx.put("path", n.getPath());
		ctx.put("version", "1.0");
		ctx.put("label", "myversion");
		ctx.put("moveLabel", Boolean.TRUE.toString());
		catalog.getCommand("addVersionLabel").execute(ctx);
		assertTrue(Arrays.asList(n.getVersionHistory().getVersionLabels())
				.contains("myversion"));
	}

	public void testRemoveVersionLabel() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.getSession().save();
		Version v = n.checkin();
		n.getVersionHistory().addVersionLabel(v.getName(), "myversion", true);
		ctx.put("path", n.getPath());
		ctx.put("label", "myversion");
		catalog.getCommand("removeVersionLabel").execute(ctx);
		assertTrue(n.getVersionHistory().getVersionLabels().length == 0);
	}

	public void testRestore() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.getSession().save();
		Version v = n.checkin();
		n.checkout();
		n.setProperty("newprop", "newval");
		n.save();
		ctx.put("path", n.getPath());
		ctx.put("version", v.getName());
		ctx.put("removeExisting", Boolean.TRUE.toString());
		catalog.getCommand("restore").execute(ctx);
		assertFalse(n.hasProperty("newprop"));
	}

	public void testRestoreByLabel() throws Exception
	{
		Node n = getTestNode();
		n.addMixin("mix:versionable");
		n.getSession().save();
		Version v = n.checkin();
		n.getVersionHistory().addVersionLabel(v.getName(), "myversion", true);
		n.checkout();
		n.setProperty("newprop", "newval");
		n.save();
		ctx.put("path", n.getPath());
		ctx.put("label", "myversion");
		ctx.put("removeExisting", Boolean.TRUE.toString());
		catalog.getCommand("restoreByLabel").execute(ctx);
		assertFalse(n.hasProperty("newprop"));
	}

	// TODO: add ext, fs, lock, query, versioning(only Merge), export test cases

}
