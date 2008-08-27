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
package org.apache.jackrabbit.core.config;

import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authentication.DefaultLoginModule;
import org.apache.jackrabbit.core.security.simple.SimpleAccessManager;
import org.apache.jackrabbit.core.security.simple.SimpleSecurityManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/** <code>SecurityConfigTest</code>... */
public class SecurityConfigTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(SecurityConfigTest.class);

    private RepositoryConfigurationParser parser;

    protected void setUp() throws Exception {
        super.setUp();
        parser = new RepositoryConfigurationParser(new Properties());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConfig1() throws ConfigurationException {
        Element xml = parseXML(new InputSource(new StringReader(CONFIG_1)), true);
        SecurityConfig config = parser.parseSecurityConfig(xml);

        assertNotNull(config.getAppName());
        assertEquals("Jackrabbit", config.getAppName());

        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        assertNotNull(smc);
        assertTrue(smc.newInstance() instanceof SimpleSecurityManager);
        assertNull(smc.getWorkspaceAccessConfig());
        assertNull(smc.getWorkspaceName());

        assertNotNull(config.getAccessManagerConfig());
        assertTrue(config.getAccessManagerConfig().newInstance() instanceof SimpleAccessManager);

        assertNull(config.getLoginModuleConfig());
    }

    public void testConfig2() throws ConfigurationException {
        Element xml = parseXML(new InputSource(new StringReader(CONFIG_2)), true);
        SecurityConfig config = parser.parseSecurityConfig(xml);

        assertNotNull(config.getAppName());
        assertEquals("Jackrabbit", config.getAppName());

        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        assertNotNull(smc);
        assertTrue(smc.newInstance() instanceof DefaultSecurityManager);
        assertNull(smc.getWorkspaceAccessConfig());
        assertEquals("security", smc.getWorkspaceName());

        AccessManagerConfig amc = config.getAccessManagerConfig();
        assertNotNull(amc);
        assertTrue(amc.newInstance() instanceof DefaultAccessManager);

        LoginModuleConfig lmc = config.getLoginModuleConfig();
        assertNotNull(lmc);
        assertTrue(lmc.getLoginModule() instanceof DefaultLoginModule);
        Properties options = lmc.getParameters();
        assertNotNull(options);
        assertEquals("anonymous", options.getProperty("anonymousId"));
        assertEquals("admin", options.getProperty("adminId"));
        assertEquals("org.apache.jackrabbit.TestPrincipalProvider", options.getProperty("principalProvider"));
    }

    public void testInvalidConfig() {
        List invalid = new ArrayList();
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_1)));
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_2)));
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_3)));

        for (Iterator it = invalid.iterator(); it.hasNext();) {
            try {
                Element xml = parseXML((InputSource) it.next(), false);
                parser.parseSecurityConfig(xml);
                fail("Invalid config -> should fail.");
            } catch (ConfigurationException e) {
                // ok
            }
        }
    }

    private static Element parseXML(InputSource xml, boolean validate) throws ConfigurationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validate);
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (validate) {
                builder.setErrorHandler(new ConfigurationErrorHandler());
            }
            builder.setEntityResolver(ConfigurationEntityResolver.INSTANCE);
            Document document = builder.parse(xml);
            return document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Unable to create configuration XML parser", e);
        } catch (SAXParseException e) {
            throw new ConfigurationException("Configuration file syntax error. (Line: " + e.getLineNumber() + " Column: " + e.getColumnNumber() + ")", e);
        } catch (SAXException e) {
            throw new ConfigurationException("Configuration file syntax error. ", e);
        } catch (IOException e) {
            throw new ConfigurationException("Configuration file could not be read.", e);
        }
    }

    private static final String CONFIG_1 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.security.simple.SimpleSecurityManager\"></SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.simple.SimpleAccessManager\"></AccessManager>" +
            "    </Security>";

    private static final String CONFIG_2 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "        </SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.DefaultAccessManager\">" +
            "        </AccessManager>" +
            "        <LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">" +
            "           <param name=\"anonymousId\" value=\"anonymous\"/>" +
            "           <param name=\"adminId\" value=\"admin\"/>" +
            "           <param name=\"principalProvider\" value=\"org.apache.jackrabbit.TestPrincipalProvider\"/>" +
            "        </LoginModule>\n" +
            "    </Security>";

    private static final String INVALID_CONFIG_1 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.security.simple.SimpleSecurityManager\"></SecurityManager>" +
            "    </Security>";

    private static final String INVALID_CONFIG_2 =
            "    <Security>" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.security.simple.SimpleSecurityManager\"></SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.simple.SimpleAccessManager\"></AccessManager>" +
            "    </Security>";
    private static final String INVALID_CONFIG_3 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.simple.SimpleAccessManager\"></AccessManager>" +
            "    </Security>";
}