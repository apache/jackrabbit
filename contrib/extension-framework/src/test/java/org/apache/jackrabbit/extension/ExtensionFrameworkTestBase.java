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
package org.apache.jackrabbit.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory;

/**
 * The <code>ExtensionFrameworkTestBase</code> TODO
 *
 * @author Felix Meschberger
 */
public class ExtensionFrameworkTestBase extends TestCase {

    /** Logger for test cases */
    protected static final Log log =
        LogFactory.getLog("org.apache.jackrabbit.extension.test");

    protected static final String WORKSPACE = "default";
    protected static final String USER = "admin";

    protected static final String PROVIDER_URL = "ClassLoader";
    protected static final String REPOSITORY_NAME = "ClassLoaderRepository";

    protected static final String ROOT_NODE = "/services";
    protected static final String ID1 = "org.apache.jackrabbit.app.services";
    protected static final String ID2 = "org.apache.jackrabbit.test";

    protected RepositoryImpl repository;
    protected Session session;

    public ExtensionFrameworkTestBase() {
        super();
    }

    public ExtensionFrameworkTestBase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        if (!"repositoryStart".equals(getName())) {
            Context ctx = getInitialContext();
            repository = (RepositoryImpl) ctx.lookup(REPOSITORY_NAME);

            Credentials creds = new SimpleCredentials(USER, USER.toCharArray());
            session = repository.login(creds, WORKSPACE);
        }
    }

    public void repositoryStart() throws Exception {
        InputStream config = getClass().getResourceAsStream("/repository.xml");
        String home = new File("cltest").getAbsolutePath();
        RepositoryConfig rc = RepositoryConfig.create(config, home);
        RepositoryImpl repository = RepositoryImpl.create(rc);

        try {
            Context ctx = getInitialContext();
            ctx.bind(REPOSITORY_NAME, repository);
        } catch (NamingException ne) {
            repository.shutdown();
            throw ne;
        }
    }

    public void repositoryStop() throws Exception {
        // this is special, logout here and clean repository
        disconnect();
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }

        Context ctx = getInitialContext();
        ctx.unbind(REPOSITORY_NAME);
    }

    protected void tearDown() throws Exception {
        disconnect();
        repository = null;
        super.tearDown();
    }

    private Context getInitialContext() throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
            DummyInitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, PROVIDER_URL);

        return new InitialContext(env);
    }

    private void disconnect() {
        if (session != null) {
            try {
                removeTestData(session);
            } catch (RepositoryException re) {
                // ignore
            }
            session.logout();
            session = null;
        }
    }

    //---------- RepositoryLoader ----------------------------------------------

    protected void fillTestData(Session session) throws RepositoryException {
        try {
            session.getItem(ROOT_NODE).remove();
            session.save();
        } catch (PathNotFoundException ignore) {
            // ok, if root no extisting
        }

        // make sure the root is available
        ensurePath(session, ROOT_NODE);

        fillExtension(session, ROOT_NODE+"/delivery/core", ID1, "delivery.core", null, null, null);
        fillExtension(session, ROOT_NODE+"/delivery/cache", ID1, "delivery.cache", null, null, null);
        fillExtension(session, ROOT_NODE+"/delivery/link", ID1, "delivery.link", null, null, null);
        fillExtension(session, ROOT_NODE+"/delivery/script", ID1, "delivery.script", null, null, null);
        fillExtension(session, ROOT_NODE+"/delivery/gfx", ID1, "delivery.gfx", null, null, null);

        fillExtension(session, ROOT_NODE+"/development/jsp", ID1, "development.jsp", null, null,
            "org.apache.jackrabbit.extension.configuration.ItemConfiguration");
        fillExtension(session, ROOT_NODE+"/development/ecma", ID1, "development.ecma", null, null, null);

        String devCore = ROOT_NODE + "/development/core";
        fillExtension(session, devCore, ID1, "development.core",
            "org.apache.jackrabbit.extension.DevCoreTest", devCore+"/classes", null);
        putClass(session, devCore, "DevCoreTest.class");

        // duplicate extension - ok for iterator, not ok for explicit finding
        fillExtension(session, ROOT_NODE+"/delivery/gfx2", ID1, "delivery.gfx", null, null, null);
    }

    protected static void removeTestData(Session session) throws RepositoryException {
        session.getItem(ROOT_NODE).remove();
        session.save();
    }

    private static void fillExtension(Session session, String path, String id,
            String name, String clazz, String classPath, String configClass)
            throws RepositoryException {

        // get the extension's node
        Node extNode = ensurePath(session, path);

        // mark as an extension
        extNode.addMixin(ExtensionManager.NODE_EXTENSION_TYPE);

        // fill rest
        trySetProperty(extNode, ExtensionDescriptor.PROP_REP_NAME, name, false);
        trySetProperty(extNode, ExtensionDescriptor.PROP_REP_ID, id, false);
        trySetProperty(extNode, ExtensionDescriptor.PROP_REP_CLASS, clazz, false);
        trySetProperty(extNode, ExtensionDescriptor.PROP_REP_CLASSPATH, classPath, true);
        trySetProperty(extNode, ExtensionDescriptor.PROP_REP_CONFIGURATION_CLASS, configClass, false);

        // save the node
        extNode.save();
    }

    protected static Node ensurePath(Session session, String path) throws RepositoryException {
        StringTokenizer tokener = new StringTokenizer(path, "/");
        Node node = session.getRootNode();
        while (tokener.hasMoreTokens()) {
            String label = tokener.nextToken();

            if (node.hasNode(label)) {
                node = node.getNode(label);
            } else {
                node = node.addNode(label, "nt:unstructured");
            }
        }

        // save all modifications
        session.save();

        return node;
    }

    protected static void trySetProperty(Node extNode, String prop, String value,
            boolean multiple) throws RepositoryException {

        // nothing if no value
        if (value == null || value.length() == 0) {
            return;
        }

        // single value
        if (!multiple) {
            extNode.setProperty(prop, value);
            return;
        }

        // split multivalue on ","
        List valueList = new ArrayList();
        StringTokenizer tokener = new StringTokenizer(value, ",");
        while (tokener.hasMoreTokens()) {
            valueList.add(tokener.nextToken());
        }

        // create value objects
        ValueFactory vf = extNode.getSession().getValueFactory();
        Value[] values = new Value[valueList.size()];
        for (int i=0; i < values.length; i++) {
            values[i] = vf.createValue((String) valueList.get(i));
        }

        // set the property
        extNode.setProperty(prop, values);
    }

    private void putClass(Session session, String extLoc, String cls) throws RepositoryException {
        String name = getClass().getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) name = name.substring(0, lastDot);
        extLoc += "/classes/" + name.replace('.', '/');
//        extLoc += "/classes/" +
//              base.getPackage().getName().replace('.', '/');

        Node pNode = ensurePath(session, extLoc);
        Node file = pNode.addNode(cls, "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", "application/octet-stream");
        content.setProperty("jcr:lastModified", Calendar.getInstance());

        InputStream ins = getClass().getResourceAsStream("/"+cls+".bin");
        if (ins != null) {
            try {
                content.setProperty("jcr:data", ins);
            } finally {
                try {
                    ins.close();
                } catch (IOException ignore) {}
            }
        }

        pNode.save();
    }
}
