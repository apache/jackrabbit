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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.util.ISO8601;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.*;
import java.util.*;

public class Test {
    private static Logger log = Logger.getLogger(Test.class);
    private static final String LOG_CONFIG_FILE_NAME = "log4j.properties";

    public static void main(String[] args) throws Exception {
        // config dir: location of repository.xml & log4j.properties
        String configDir = System.getProperty("config.dir");
        if (configDir == null) {
            // fallback to cwd
            configDir = System.getProperty("user.dir");
        }
        PropertyConfigurator.configure(configDir + "/" + LOG_CONFIG_FILE_NAME);
        String configFile = configDir + "/" + RepositoryConfig.CONFIG_FILE_NAME;

        // repository home dir
        String repHomeDir = System.getProperty("repository.home");
        if (repHomeDir == null) {
            // fallback to cwd
            repHomeDir = System.getProperty("user.dir");
        }
        // set up the environment for creating the initial context
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
        env.put(Context.PROVIDER_URL, "localhost");
        InitialContext ctx = new InitialContext(env);

        RegistryHelper.registerRepository(ctx, "repo", configFile, repHomeDir, true);
        Repository r = (Repository) ctx.lookup("repo");
        Session session = r.login(new SimpleCredentials("anonymous", "".toCharArray()), null);

        Workspace wsp = session.getWorkspace();

        Node root = session.getRootNode();

        System.out.println("initial...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);
/*
        if (root.canAddMixin("mix:versionable")) {
            root.addMixin("mix:versionable");
            dumpTree(root, System.out);
            boolean versionable = root.isNodeType("mix:versionable");
            root.removeMixin("mix:versionable");
            root.save();
        }
*/
        //root.setProperty("blob", new FileInputStream(new File("d:/temp/jackrabbit.zip")));

        if (root.hasProperty("blah")) {
            Property p = root.getProperty("blah");
            root.getProperty("blah").remove();
            System.out.println("before save()...");
            System.out.println();
            dumpTree(root, System.out);
            ((SessionImpl)session).dump(System.out);
            root.save();
            System.out.println("after save()...");
            System.out.println();
            dumpTree(root, System.out);
            ((SessionImpl)session).dump(System.out);
            if (root.hasProperty("blah")) {
                p = root.getProperty("blah");
            }
        }

        Property p1 = root.setProperty("blah", 1);
        root.setProperty("blah", 1.4);
        root.setProperty("blah", "blahblah");

//        root.save();
//        if (true) return;

/*
        Node file = root.addNode("blu", "nt:file");
        file.addNode("jcr:content", "nt:unstructured");
        root.addNode("blu", "nt:folder");
        root.addNode("blu");

        Properties repProps = r.getProperties();
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
        ((SessionImpl)session).dump(System.out);
*/
        root.save();

        System.out.println("after save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

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
/*
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
*/
        System.out.println("before save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        root.save();

        System.out.println("after save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);
/*
        Node linkTarget = link.getParent().getNode(link.getValue().getString());
        System.out.println(link.getPath() + " refers to " + linkTarget.getPath());

        root.setProperty("ref", new ReferenceValue(misc));
*/
        boolean mult = root.getProperty("blah").getDefinition().isMultiple();

        System.out.println("before save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        root.save();

        mult = root.getProperty("blah").getDefinition().isMultiple();

        System.out.println("after save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);
/*
        PropertyIterator pi = misc.getReferences();
        while (pi.hasNext()) {
            Property prop = pi.nextProperty();
            Node target = prop.getNode();
            System.out.println(prop.getPath() + " is a reference to " + target.getPath());
        }
*/
/*
	misc.remove();
	try {
	    root.save();
	} catch (ConstraintViolationException cve) {
	    root.getProperty("ref").remove();
	    root.save();
	}
*/
/*
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

        //importNode(new File("d:/dev/jackrabbit/src/test"), imported);

        if (root.hasNode("foo")) {
            root.getNode("foo").remove();
        }

        Node n = root.addNode("foo", "nt:folder");
        Node n2 = n.addNode("foofile", "nt:file");
        Node n3 = n2.addNode("jcr:content", "nt:unstructured");
        Property prop1 = n3.setProperty("prop1", new LongValue(123));
        Property prop2 = n3.setProperty("prop2", new StringValue("blahblah"));

        System.out.println("before save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        root.save();

        System.out.println("after save()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        session.getWorkspace().copy("/imported", "/misc/blah");
        session.getWorkspace().move("/misc/blah", "/misc/blahblah");

        System.out.println("after Workspace.copy/move()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        prop1.remove();

        System.out.println();
        dumpTree(root, System.out);

        System.out.println("before refresh()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        root.refresh(false);

        System.out.println("after refresh()...");
        System.out.println();
        dumpTree(root, System.out);
        ((SessionImpl)session).dump(System.out);

        System.out.println("exiting...");
        System.out.println();
        ((WorkspaceImpl) wsp).dump(System.out);

        wsp.exportSysView("/", new FileOutputStream("d:/temp/sv_export1.xml"), false, false);
        //wsp.exportDocView("/", new FileOutputStream("d:/temp/dv_export1.xml"), false, false);

        repProps = r.getProperties();
        System.out.println("repository properties:");
        System.out.println(repProps);
*/
        //((RepositoryImpl) r).shutdown();
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
            Node content = newNode.addNode("jcr:content", "nt:resource");
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
                String status = node.isModified() ? "*" : (node.isNew() ? "+" : " ");
                ps.println("[node] " + status + " " + node.getName());

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
                String status = property.isModified() ? "*" : (property.isNew() ? "+" : " ");
                ps.println("[prop] " + status + " " + property.getName() + " " + sb.toString());

                super.entering(property, i);
            }
        });
    }
}
