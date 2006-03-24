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
package org.apache.jackrabbit.tck;

import org.apache.jackrabbit.tck.j2ee.RepositoryServlet;
import org.apache.jackrabbit.test.JNDIRepositoryStub;
import org.apache.jackrabbit.test.RepositoryStub;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;
import javax.naming.Context;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Set;

import junit.framework.TestSuite;
import junit.framework.TestCase;


/**
 * The <code>WebAppTestConfig</code> class reads and saves the config in the tck web app specific way.
 */
public class WebAppTestConfig {



    /** default property names */
    public final static String[] propNames = {JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_WORKSPACE_NAME,
                                              JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP,
                                              Context.PROVIDER_URL,
                                              Context.INITIAL_CONTEXT_FACTORY,
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
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME4,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_PROP_NAME1,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_PROP_NAME2,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NAMESPACES,
                                              JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODETYPE};

    /**
     * Reads the config entries from the repository
     *
     * @return test config
     */
    public static Map getConfig() {
        Map config = new HashMap();
        try {
            Session repSession = RepositoryServlet.getSession();
            Node configNode = repSession.getRootNode().getNode("testconfig");
            PropertyIterator pitr = configNode.getProperties();

            while (pitr.hasNext()) {
                Property p = pitr.nextProperty();
                config.put(p.getName(), p.getString());
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

        // add additional props
        Set keys = props.keySet();
        if (!keys.contains(JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP)) {
            props.put(JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP, "");
        }
        if (!keys.contains(Context.PROVIDER_URL)) {
            props.put(Context.PROVIDER_URL, "");
        }
        if (!keys.contains(Context.INITIAL_CONTEXT_FACTORY)) {
            props.put(Context.INITIAL_CONTEXT_FACTORY, "");
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
        Iterator allPropNames = getCurrentConfig().keySet().iterator();

        while (allPropNames.hasNext()) {
            String pName = (String) allPropNames.next();
            setEntry(pName, request, testConfig);
        }

        // save
        testConfig.save();
    }

    /**
     * This method saves a single property
     *
     * @param propName property name
     * @param propValue property value
     * @param repSession session
     * @throws RepositoryException
     */
    public static void saveProperty(String propName, String propValue, Session repSession) throws RepositoryException {
        // create config node if not yet existing
        Node testConfig;
        if (repSession.getRootNode().hasNode("testconfig")) {
            testConfig = repSession.getRootNode().getNode("testconfig");
        } else {
            testConfig = repSession.getRootNode().addNode("testconfig", "nt:unstructured");
            repSession.getRootNode().save();
        }

        testConfig.setProperty(propName, propValue);

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

    /**
     * Returns all test case specific configuration entries
     *
     * @param suite test suite
     * @return all test case specific conf entries
     */
    public static Map getTestCaseSpecificConfigs(TestSuite suite) {
        Map currentConfig = getCurrentConfig();
        Map configs = new HashMap();

        // check for "package" defined props
        String pname = suite.getName();
        if ("versioning".equals(pname)) {
            pname = "version";
        }
        configs.putAll(getProperties(pname, currentConfig));

        Enumeration allTestClasses = suite.tests();

        while (allTestClasses.hasMoreElements()) {
            TestSuite aTest = (TestSuite) allTestClasses.nextElement();
            String name = aTest.getName();
            name = name.substring(name.lastIndexOf(".") + 1);

            // check for class defined props
            configs.putAll(getProperties(name, currentConfig));

            // goto methods
            Enumeration testMethods = aTest.tests();

            while (testMethods.hasMoreElements()) {
                TestCase tc = (TestCase) testMethods.nextElement();
                String methodname = tc.getName();
                String fullName = name + "." + methodname;
                configs.putAll(getProperties(fullName, currentConfig));
            }
        }
        return configs;
    }

    /**
     * Returns the current configuration
     * @return
     */
    public static Map getCurrentConfig() {
        Map conf = getOriConfig();
        conf.putAll(getConfig());

        // fill all empty (== null) values with an empty string
        Iterator itr = conf.keySet().iterator();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            String val = (String) conf.get(key);
            if (val == null) {
                conf.put(key, "");
            }
        }
        return conf;
    }

    /**
     * Returns all properties which property name starts with <code>"javax.jcr.tck." + name</code>
     *
     * @param name property name "extension"
     * @param config configuration
     * @return all properties which apply the above mentioned rule
     */
    private static Map getProperties(String name, Map config) {
        Map props = new HashMap();

        String pname = RepositoryStub.PROP_PREFIX + "." + name;

        Iterator itr = config.keySet().iterator();

        while (itr.hasNext()) {
            String key = (String) itr.next();
            if (key.startsWith(pname)) {
                props.put(key, config.get(key));
            }
        }
        return props;
    }

    /**
     * Removes the custom config entries
     *
     * @throws RepositoryException
     */
    public static void resetConfiguration() throws RepositoryException {
        Session repSession = RepositoryServlet.getSession();
        if (repSession.getRootNode().hasNode("testconfig")) {
            repSession.getRootNode().getNode("testconfig").remove();
        }
        repSession.getRootNode().save();
    }
}
