/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core;

import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeManagerImpl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.util.TraversingItemVisitor;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

public class Test {
    private static Logger log = Logger.getLogger(Test.class);
    private static final String LOG_CONFIG_FILE_NAME = "log4j.properties";

    public static void main(String[] args) throws Exception {
	// location of config.xml & log4j.properties
	String configDir = System.getProperty("config.dir");
	if (configDir == null) {
	    // fallback to cwd
	    configDir = System.getProperty("user.dir");
	}
	PropertyConfigurator.configure(configDir + "/" + LOG_CONFIG_FILE_NAME);
	String configFile = configDir + "/" + RepositoryFactory.DEFAULT_CONFIG_FILE;

	// repository factory home dir
	String factoryHomeDir = System.getProperty("repository.factory.home");
	if (factoryHomeDir == null) {
	    // fallback to cwd
	    factoryHomeDir = System.getProperty("user.dir");
	}

	RepositoryFactory rf = RepositoryFactory.create(configFile, factoryHomeDir);
	Repository r = rf.getRepository("localfs");
	Session session = r.login(new SimpleCredentials("anonymous", "".toCharArray()), null);
	Workspace wsp = session.getWorkspace();

	NodeTypeManager ntMgr = wsp.getNodeTypeManager();
	NodeTypeIterator ntIter = ntMgr.getAllNodeTypes();
	while (ntIter.hasNext()) {
	    NodeType nt = ntIter.nextNodeType();
	    System.out.println("built-in nodetype: " + nt.getName());
	}

	System.out.println();
	((NodeTypeManagerImpl) ntMgr).dump(System.out);
	System.out.println();

	Node root = session.getRootNode();
/*
	String svExportFilePath = "d:/temp/sv_export0.xml";
	String dvExportFilePath = "d:/temp/dv_export0.xml";
	String importTargetName = "sandbox";

	wsp.exportSysView("/", new FileOutputStream(svExportFilePath), true, false);
	wsp.exportDocView("/", new FileOutputStream(dvExportFilePath), true, false);
	if (!root.hasNode(importTargetName)) {
	    root.addNode(importTargetName, "nt:unstructured");
	}
	FileInputStream fin = new FileInputStream(svExportFilePath);
	t.importXML("/" + importTargetName, fin);
	t.save();
*/
	String ntName = root.getProperty("jcr:primaryType").getString();
	session.setNamespacePrefix("bla", "http://www.jcp.org/jcr/nt/1.0");
	ntName = root.getProperty("jcr:primaryType").getString();
	session.setNamespacePrefix("nt", "http://www.jcp.org/jcr/nt/1.0");

	System.out.println("initial...");
	System.out.println();
	dumpTree(root, System.out);

	//t.move("/foo", "/misc/bla");
	System.out.println("after move...");
	System.out.println();
	dumpTree(root, System.out);

	if (root.canAddMixin("mix:versionable")) {
	    root.addMixin("mix:versionable");
	    if (root.canAddMixin("mix:accessControllable")) {
		root.addMixin("mix:accessControllable");
	    }
	    dumpTree(root, System.out);
	    boolean accessControlable = root.isNodeType("mix:accessControllable");
	    root.removeMixin("mix:versionable");
	    dumpTree(root, System.out);
	    root.save();
	}

	//root.setProperty("blob", new FileInputStream(new File("d:/temp/jckrabbit.zip")));

	if (root.hasProperty("blah")) {
	    root.remove("blah");
	}
	root.setProperty("blah", 1);
	root.setProperty("blah", 1.4);
	root.setProperty("blah", "blahblah");
	Node file = root.addNode("blu", "nt:file");
	file.addNode("jcr:content", "nt:unstructured");
	root.addNode("blu", "nt:folder");
	root.addNode("blu");

	Properties repProps = ((RepositoryImpl) r).getProperties();
	System.out.println("repository properties:");
	System.out.println(repProps);

	dumpTree(root, System.out);
	root.orderBefore("blu", null);
	dumpTree(root, System.out);
	root.orderBefore("blu[2]", "blu[1]");
	dumpTree(root, System.out);

	System.out.println("before save()...");
	System.out.println();
	dumpTree(root, System.out);

	root.save();

	if (root.hasProperty("blah")) {
	    root.setProperty("blah", (String) null);
	}
	if (!root.hasProperty("blah")) {
	    String[] strings = new String[]{"huey", "louie", null, "dewey"};
	    root.setProperty("blah", strings);
	    Value[] vals = root.getProperty("blah").getValues();
	    ArrayList list = new ArrayList();
	    for (int i = 0; i < vals.length; i++) {
		list.add(vals[i].getString());
	    }
	    System.out.println(list);
	}

	System.out.println("after save()...");
	System.out.println();
	dumpTree(root, System.out);

	if (root.hasProperty("blob")) {
	    Property blob = root.getProperty("blob");
	    InputStream in = blob.getStream();
	    // spool stream to temp file
	    FileOutputStream out = new FileOutputStream(new File("d:/temp/scratch.zip"));
	    try {
		byte[] buffer = new byte[8192];
		int read = 0;
		while ((read = in.read(buffer)) > 0) {
		    out.write(buffer, 0, read);
		}
	    } finally {
		out.close();
		in.close();
	    }
	}

	Node misc = root.addNode("misc", "nt:unstructured");
	misc.addMixin("mix:referenceable");
	Property link = misc.setProperty("link", PathValue.valueOf("../blu[2]"));
	root.save();
	Node linkTarget = link.getParent().getNode(link.getValue().getString());
	System.out.println(link.getPath() + " refers to " + linkTarget.getPath());

	root.setProperty("ref", new ReferenceValue(misc));
	root.save();
	PropertyIterator pi = misc.getReferences();
	while (pi.hasNext()) {
	    Property prop = pi.nextProperty();
	    Node target = prop.getNode();
	    System.out.println(prop.getPath() + " is a reference to " + target.getPath());
	}
/*
	misc.remove(".");
	try {
	    root.save();
	} catch (ConstraintViolationException cve) {
	    root.remove("ref");
	    root.save();
	}
*/
	root.setProperty("date", DateValue.valueOf("2003-07-28T06:18:57.848+01:00"));
	Property p = root.getProperty("date");
	Value val = p.getValue();
	Calendar d = val.getDate();

	Node imported = null;
	if (!root.hasNode("imported")) {
	    imported = root.addNode("imported", "nt:unstructured");
	} else {
	    imported = root.getNode("imported");
	}

	importNode(new File("d:/dev/jsr170/jackrabbit/src/java"), imported);

	if (root.hasNode("foo")) {
	    root.remove("foo");
	}

	Node n = root.addNode("foo", "nt:folder");
	Node n2 = n.addNode("foofile", "nt:file");
	Node n3 = n2.addNode("jcr:content", "nt:unstructured");
	Property prop1 = n3.setProperty("prop1", new LongValue(123));
	Property prop2 = n3.setProperty("prop2", new StringValue("blahblah"));

	System.out.println("before save()...");
	System.out.println();
	dumpTree(root, System.out);

	root.save();

	session.getWorkspace().copy("/imported", "/misc/blah");
	session.getWorkspace().move("/misc/blah", "/misc/blahblah");

	System.out.println("after save()...");
	System.out.println();
	dumpTree(root, System.out);

	n3.remove("prop1");

	System.out.println();
	dumpTree(root, System.out);

	System.out.println("before refresh()...");
	System.out.println();
	dumpTree(root, System.out);

	root.refresh(false);

	System.out.println("after refresh()...");
	System.out.println();
	dumpTree(root, System.out);

	System.out.println("exiting...");
	System.out.println();
	((WorkspaceImpl) wsp).dump(System.out);

	//wsp.exportSysView("/", new FileOutputStream("d:/temp/sv_export1.xml"), false, false);
	wsp.exportDocView("/", new FileOutputStream("d:/temp/dv_export1.xml"), false, false);

	repProps = ((RepositoryImpl) r).getProperties();
	System.out.println("repository properties:");
	System.out.println(repProps);

	((RepositoryImpl) r).shutdown();
    }

    public static void importNode(File file, Node parent) throws Exception {
	if (file.isDirectory()) {
	    Node newNode = parent.addNode(file.getName(), "nt:folder");
	    File[] children = file.listFiles();
	    if (children != null) {
		for (int i = 0; i < children.length; i++) {
		    importNode(children[i], newNode);
		}
	    }
	} else {
	    Node newNode = parent.addNode(file.getName(), "nt:file");
	    Node content = newNode.addNode("jcr:content", "nt:mimeResource");
	    content.setProperty("jcr:data", new FileInputStream(file));
	    content.setProperty("jcr:lastModified", Calendar.getInstance());
	    content.setProperty("jcr:mimeType", "application/octet-stream");
	}
    }

    public static void dumpTree(Node start, final PrintStream ps) throws RepositoryException {
	start.accept(new TraversingItemVisitor.Default() {
	    protected void entering(Node node, int i) throws RepositoryException {
		while (i-- > 0) {
		    System.out.print('\t');
		}
		ps.println("[node] " + node.getName());

		super.entering(node, i);
	    }

	    protected void entering(Property property, int i) throws RepositoryException {
		while (i-- > 0) {
		    System.out.print('\t');
		}
		StringBuffer sb = new StringBuffer();
		if (property.getDefinition().isMultiple()) {
		    Value[] values = property.getValues();
		    for (int j = 0; j < values.length; j++) {
			if (sb.length() > 0) {
			    sb.append(", ");
			}
			Value val = values[j];
			if (val.getType() == PropertyType.BINARY) {
			    sb.append("[binary]");
			} else {
			    sb.append(val.getString());
			}
		    }
		} else {
		    Value val = property.getValue();
		    if (val.getType() == PropertyType.BINARY) {
			sb.append("[binary]");
		    } else {
			sb.append(val.getString());
		    }
		}
		ps.println("[prop] " + property.getName() + " " + sb.toString());

		super.entering(property, i);
	    }
	});
    }
}
