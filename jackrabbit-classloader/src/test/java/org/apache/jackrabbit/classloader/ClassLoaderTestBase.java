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
package org.apache.jackrabbit.classloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ClassLoaderTestBase</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date$
 */
public class ClassLoaderTestBase extends TestCase {

    /** Logger for test cases */
    protected static final Logger log =
        LoggerFactory.getLogger("org.apache.jackrabbit.classloader.test");

    protected static final String WORKSPACE = "default";
    protected static final String USER = "admin";

    protected static final String PROVIDER_URL = "ClassLoader";
    protected static final String REPOSITORY_NAME = "ClassLoaderRepository";

    protected RepositoryImpl repository;
    protected Session session;

    private Set createdItems = new HashSet();

    public ClassLoaderTestBase() {
        super();
    }

    public ClassLoaderTestBase(String name) {
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
        InputStream config =
            RepositoryImpl.class.getResourceAsStream("repository.xml");
        String home = new File("target/cltest").getAbsolutePath();
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
            clearRepository(session);
            session.logout();
            session = null;
        }
    }

    //---------- RepositoryLoader ----------------------------------------------

    protected void loadRepository(Session session, InputStream ins) {
        if (ins == null) {
            ins = getClass().getResourceAsStream("/preload.properties");
            if (ins == null) {
                log.warn("Cannot find preload properties /preload.properties");
                return;
            }
        }

        List keys = new ArrayList();
        Properties props = new Properties();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ins));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                // cut off line comment
                int comment = line.indexOf('#');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }

                // trim leading and trailing whitespace
                line = line.trim();

                // ignore line of empty
                if (line.length() == 0) {
                    continue;
                }

                int sep = line.indexOf('=');
                if (sep < 0) {
                    continue;
                }

                String key = line.substring(0, sep).trim();

                StringBuffer buf = new StringBuffer(line.substring(sep+1).trim());

                while (line.endsWith("\\")) {
                    // cut off last back slash
                    buf.setLength(buf.length()-1);

                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }

                    buf.append(line);
                }

                key = loadConvert(key);
                String value = loadConvert(buf.toString());

                keys.add(key);
                props.setProperty(key, value);
            }
        } catch (IOException ioe) {
            // ignore
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {}
            }

            try {
                ins.close();
            } catch (IOException ignore) {}
        }

        for (Iterator ki=keys.iterator(); ki.hasNext(); ) {
            String path = (String) ki.next();
            String config = props.getProperty(path);
            StringTokenizer tokener = new StringTokenizer(config, ",");
            if (!tokener.hasMoreTokens()) {
                continue;
            }

            Node parent = null;
            try {
                parent = getParent(session, path);
            } catch (RepositoryException re) {
                log.warn("Cannot get parent of " + path, re);
            }

            if (parent == null) {
                continue;
            }

            try {
                String type = tokener.nextToken();
                if ("n".equalsIgnoreCase(type)) {
                    loadNode(parent, getName(path), tokener);
                    createdItems.add(path);
                } else if ("p".equalsIgnoreCase(type)) {
                    loadProperty(parent, getName(path), tokener);
                }
            } catch (RepositoryException re) {
                log.warn("Cannot create item " + path, re);
            }
        }

        try {
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (RepositoryException re) {
            log.warn("Cannot save session", re);
        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException re) {
                log.warn("Cannot even refresh the session");
            }
        }
    }

    protected void clearRepository(Session session) {
        for (Iterator ii=createdItems.iterator(); ii.hasNext(); ) {
            String path = (String) ii.next();
            try {
                if (!session.itemExists(path)) {
                    continue;
                }

                session.getItem(path).remove();
            } catch (RepositoryException re) {
                log.info("Cannot remove Item " + path, re);
            }
        }

        try {
            session.save();
        } catch (RepositoryException re) {
            log.warn("Cannot save removals", re);
        }

        createdItems.clear();
    }

    private void loadNode(Node parent, String name,
            StringTokenizer config) throws RepositoryException {

        // node type
        String primaryType;
        if (config.hasMoreTokens()) {
            primaryType = config.nextToken();
        } else {
            primaryType = "nt:unstructured";
        }

        Node node = parent.addNode(name, primaryType);

        // content URL
        if (config.hasMoreTokens()) {
            String urlString = config.nextToken();
            try {
                URL url;
                if (urlString.startsWith("classpath:")) {
                    urlString = urlString.substring("classpath:".length());
                    url = getClass().getResource(urlString);
                } else {
                    url = new URL(urlString);
                }
                URLConnection connection = url.openConnection();
                makeFileNode(node, connection);
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }
    }

    private void loadProperty(Node parent, String name,
            StringTokenizer config) throws RepositoryException {
        String typeName;
        if (config.hasMoreTokens()) {
            typeName = config.nextToken();
        } else {
            typeName = "";
        }
        int type;
        try {
            type = PropertyType.valueFromName(typeName);
        } catch (IllegalArgumentException iae) {
            type = PropertyType.STRING;
        }

        String stringValue = ""; // default value
        if (config.hasMoreTokens()) {
            stringValue = config.nextToken();
        }

        /* Property prop = */ parent.setProperty(name, stringValue, type);
    }

    static void makeFileNode(Node node, URLConnection content)
            throws RepositoryException {

        Node contentNode = node.addNode("jcr:content", "nt:unstructured");
        InputStream ins = null;
        try {
            ins = content.getInputStream();
            contentNode.setProperty("jcr:data", ins);
        } catch (IOException ioe) {
            // ignore, but redefine content data
            contentNode.setProperty("jcr:data", "mockdata", PropertyType.BINARY);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {}
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(content.getLastModified());
        contentNode.setProperty("jcr:lastModified", cal);

        String mimeType = content.getContentType();
        if (mimeType == null || mimeType.toLowerCase().indexOf("unknown") >= 0) {
            mimeType = URLConnection.guessContentTypeFromName(node.getName());
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        contentNode.setProperty("jcr:mimeType", mimeType);

        String encoding = content.getContentEncoding();
        if (encoding != null) {
            contentNode.setProperty("jcr:encoding", encoding);
        }
    }

    static Node getParent(Session session, String path)
            throws RepositoryException {

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }

        String parentPath = path.substring(0, lastSlash);
        if (parentPath.length() == 0) {
            return session.getRootNode();
        }

        try {
            Item item = session.getItem(parentPath);
            if (item.isNode()) {
                return (Node) item;
            }
        } catch (PathNotFoundException pnfe) {

            // create the intermediate node as an unstructured node
            Node parent = getParent(session, parentPath);
            if (parent != null) {
                lastSlash = parentPath.lastIndexOf('/');
                if (lastSlash < 0) {
                    return null;
                }
                String name = parentPath.substring(lastSlash+1);

                return parent.addNode(name, "nt:folder");
            }
        }

        return null;
    }

    private String getName(String path) {
        return path.substring(path.lastIndexOf('/')+1);
    }

    private String loadConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);

        for (int x=0; x<len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value=0;
            for (int i=0; i<4; i++) {
                aChar = theString.charAt(x++);
                switch (aChar) {
                  case '0': case '1': case '2': case '3': case '4':
                  case '5': case '6': case '7': case '8': case '9':
                     value = (value << 4) + aChar - '0';
                 break;
              case 'a': case 'b': case 'c':
                          case 'd': case 'e': case 'f':
                 value = (value << 4) + 10 + aChar - 'a';
                 break;
              case 'A': case 'B': case 'C':
                          case 'D': case 'E': case 'F':
                 value = (value << 4) + 10 + aChar - 'A';
                 break;
              default:
                              throw new IllegalArgumentException(
                                           "Malformed \\uxxxx encoding.");
                        }
                    }
                    outBuffer.append((char)value);
                } else {
                    if (aChar == 't') aChar = '\t';
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }
}
