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
package org.apache.jackrabbit.tck;

import org.apache.jackrabbit.tck.j2ee.RepositoryServlet;
import org.apache.jackrabbit.test.JNDIRepositoryStub;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;


/**
 * The <code>WebAppTestConfig</code> class reads and saves the config in the tck web app specific way.
 */
public class WebAppTestConfig {
    public final static String[] propNames = {JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_WORKSPACE_NAME,
                                              JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP, "java.naming.provider.url", "java.naming.factory.initial",
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_NAME,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_PWD,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_NAME,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_PWD,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_NAME,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_PWD,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_TESTROOT,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME1,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME2,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME3,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODETYPE};

    /**
     * Reads the config entries from the repository
     *
     * @return test config
     */
    public static Map getConfig() {
        Map config = new HashMap();
        try {
            Session repSession = RepositoryServlet.getSession(null);
            Node configNode = repSession.getRootNode().getNode("testconfig");

            for (int i = 0; i < propNames.length; i++) {
                String pName = propNames[i];
                config.put(pName, configNode.getProperty(pName).getString());
            }
        } catch (RepositoryException e) {
            return new HashMap();
        }
        return config;
    }

    /**
     * Reads the original config from the property file.
     *
     * @return original read only config
     */
    public static Map getOriConfig() {
        Properties props = new Properties();
        InputStream is = WebAppTestConfig.class.getClassLoader().getResourceAsStream(JNDIRepositoryStub.STUB_IMPL_PROPS);
        if (is != null) {
            try {
                props.load(is);
            } catch (IOException e) {
                // ignore
            }
        }
        return props;
    }

    /**
     * Saves the configuration entries which needs to be set.
     *
     * @param request request with config changes
     * @param repSession <code>Session</code> used to write config
     * @throws RepositoryException
     */
    public static void save(HttpServletRequest request, Session repSession) throws RepositoryException {
        // create config node if not yet existing
        Node testConfig;
        if (repSession.getRootNode().hasNode("testconfig")) {
            testConfig = repSession.getRootNode().getNode("testconfig");
        } else {
            testConfig = repSession.getRootNode().addNode("testconfig", "nt:unstructured");
            repSession.getRootNode().save();
        }

        // save config entries
        for (int i = 0; i < propNames.length; i++) {
            String pName = propNames[i];
            setEntry(pName, request, testConfig);
        }

        // save
        testConfig.save();
    }

    /**
     * Set config entry
     *
     * @param propname config property name
     * @param request request to read property value
     * @param testConfig  test config <code>Node</code>
     * @throws RepositoryException
     */
    private static void setEntry(String propname, HttpServletRequest request, Node testConfig) throws RepositoryException {
        if (request.getParameter(propname) != null) {
            testConfig.setProperty(propname, request.getParameter(propname));
        }
    }
}
