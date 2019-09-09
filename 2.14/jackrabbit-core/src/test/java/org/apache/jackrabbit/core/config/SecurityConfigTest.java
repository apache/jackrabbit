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
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.core.security.user.UserPerWorkspaceUserManager;
import org.apache.jackrabbit.core.security.authentication.DefaultLoginModule;
import org.apache.jackrabbit.core.security.simple.SimpleAccessManager;
import org.apache.jackrabbit.core.security.simple.SimpleSecurityManager;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.UserManager;
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
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

/** <code>SecurityConfigTest</code>... */
public class SecurityConfigTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(SecurityConfigTest.class);

    private RepositoryConfigurationParser parser;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        parser = new RepositoryConfigurationParser(new Properties());
    }

    @Override
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
        assertTrue(smc.newInstance(JackrabbitSecurityManager.class) instanceof SimpleSecurityManager);
        assertNull(smc.getWorkspaceAccessConfig());
        assertNull(smc.getWorkspaceName());

        assertNotNull(config.getAccessManagerConfig());
        assertTrue(config.getAccessManagerConfig().newInstance(AccessManager.class) instanceof SimpleAccessManager);

        assertNull(config.getLoginModuleConfig());
    }

    public void testConfig2() throws ConfigurationException {
        Element xml = parseXML(new InputSource(new StringReader(CONFIG_2)), true);
        SecurityConfig config = parser.parseSecurityConfig(xml);

        assertNotNull(config.getAppName());
        assertEquals("Jackrabbit", config.getAppName());

        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        assertNotNull(smc);
        assertTrue(smc.newInstance(JackrabbitSecurityManager.class) instanceof DefaultSecurityManager);
        assertNull(smc.getWorkspaceAccessConfig());
        assertEquals("security", smc.getWorkspaceName());

        assertNull(smc.getUserManagerConfig());

        AccessManagerConfig amc = config.getAccessManagerConfig();
        assertNotNull(amc);
        assertTrue(amc.newInstance(AccessManager.class) instanceof DefaultAccessManager);

        LoginModuleConfig lmc = config.getLoginModuleConfig();
        assertNotNull(lmc);
        assertTrue(lmc.getLoginModule() instanceof DefaultLoginModule);
        Properties options = lmc.getParameters();
        assertNotNull(options);
        assertEquals("anonymous", options.getProperty("anonymousId"));
        assertEquals("admin", options.getProperty("adminId"));
        assertEquals("org.apache.jackrabbit.TestPrincipalProvider", options.getProperty("principalProvider"));
    }

    public void testConfig3() throws ConfigurationException {
        Element xml = parseXML(new InputSource(new StringReader(CONFIG_3)), true);
        SecurityConfig config = parser.parseSecurityConfig(xml);

        SecurityManagerConfig smc = config.getSecurityManagerConfig();

        assertEquals(ItemBasedPrincipal.class, smc.getUserIdClass());

        UserManagerConfig umc = smc.getUserManagerConfig();
        assertNotNull(umc);
        Properties params = umc.getParameters();
        assertNotNull(params);

        assertFalse(params.containsKey(UserManagerImpl.PARAM_COMPATIBLE_JR16));
        assertTrue(Boolean.parseBoolean(params.getProperty(UserManagerImpl.PARAM_AUTO_EXPAND_TREE)));
        assertEquals(4, Integer.parseInt(params.getProperty(UserManagerImpl.PARAM_DEFAULT_DEPTH)));
        assertEquals(2000, Long.parseLong(params.getProperty(UserManagerImpl.PARAM_AUTO_EXPAND_SIZE)));
    }

    public void testUserManagerConfig() throws RepositoryException, UnsupportedRepositoryOperationException {
        Element xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_INVALID)), true);
        UserManagerConfig umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            umc.getUserManager(UserManagerImpl.class, new Class[] {String.class}, "invalid");
            fail("Nonexisting umgr implementation -> instanciation must fail.");
        } catch (ConfigurationException e) {
            // success
        }

        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_IMPL)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();

        // assignable from same class as configured
        UserManager um = umc.getUserManager(UserManagerImpl.class, new Class[] {
            SessionImpl.class, String.class}, superuser, "admin");
        assertNotNull(um);
        assertTrue(um instanceof UserManagerImpl);
        assertTrue(um.isAutoSave());
        try {
            um.autoSave(false);
            fail("must not be allowed");
        } catch (RepositoryException e) {
            // success
        }

        // derived class expected -> must fail
        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_IMPL)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            um = umc.getUserManager(UserPerWorkspaceUserManager.class, new Class[] {
                    SessionImpl.class, String.class}, superuser, "admin");
            fail("UserManagerImpl is not assignable from derived class");
        } catch (ConfigurationException e) {
            // success
        }

        // passing invalid parameter types
        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_IMPL)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            um = umc.getUserManager(UserManagerImpl.class, new Class[] {
                    Session.class}, superuser, "admin");
            fail("Invalid parameter types -> must fail.");
        } catch (ConfigurationException e) {
            // success
        }

        // passing wrong arguments        
        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_IMPL)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            um = umc.getUserManager(UserManagerImpl.class, new Class[] {
                    SessionImpl.class, String.class}, superuser, 21);
            fail("Invalid init args -> must fail.");
        } catch (ConfigurationException e) {
            // success
        }

        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_DERIVED)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        // assignable from defines base class
        um = umc.getUserManager(UserManagerImpl.class, new Class[] {
            SessionImpl.class, String.class}, superuser, "admin");
        assertNotNull(um);
        assertTrue(um instanceof UserPerWorkspaceUserManager);
        // but: configured class creates a umgr that requires session.save
        assertTrue(um.isAutoSave());
        // changing autosave behavior must succeed.
        um.autoSave(false);

        // test authorizable-action configuration
        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_WITH_ACTIONS)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        AuthorizableAction[] actions = umc.getAuthorizableActions();
        assertEquals(2, actions.length);

        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_WITH_INVALID_ACTIONS)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            actions = umc.getAuthorizableActions();
            fail("Invalid configuration - must fail");
        } catch (ConfigurationException e) {
            // success
        }

        xml = parseXML(new InputSource(new StringReader(USER_MANAGER_CONFIG_WITH_INVALID_ACTIONS_2)), true);
        umc = parser.parseSecurityConfig(xml).getSecurityManagerConfig().getUserManagerConfig();
        try {
            actions = umc.getAuthorizableActions();
            fail("Invalid configuration - must fail");
        } catch (ConfigurationException e) {
            // success
        }
    }

    /**
     * 
     * @throws Exception
     */
    public void testPrincipalProviderConfig() throws Exception {
        PrincipalProviderRegistry ppr = new ProviderRegistryImpl(null);

        // standard config
        Element xml = parseXML(new InputSource(new StringReader(PRINCIPAL_PROVIDER_CONFIG)), true);
        LoginModuleConfig lmc = parser.parseSecurityConfig(xml).getLoginModuleConfig();        
        PrincipalProvider pp = ppr.registerProvider(lmc.getParameters());
        assertEquals(pp, ppr.getProvider(pp.getClass().getName()));
        assertEquals("org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider", pp.getClass().getName());

        // config specifying an extra name
        xml = parseXML(new InputSource(new StringReader(PRINCIPAL_PROVIDER_CONFIG1)), true);
        lmc = parser.parseSecurityConfig(xml).getLoginModuleConfig();
        pp = ppr.registerProvider(lmc.getParameters());
        assertEquals(pp, ppr.getProvider("test"));
        assertEquals("org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider", pp.getClass().getName());

        // use alternative class config
        xml = parseXML(new InputSource(new StringReader(PRINCIPAL_PROVIDER_CONFIG2)), true);
        lmc = parser.parseSecurityConfig(xml).getLoginModuleConfig();
        pp = ppr.registerProvider(lmc.getParameters());
        assertEquals(pp, ppr.getProvider("test2"));
        assertEquals("org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider", pp.getClass().getName());

        // all 3 providers must be registered despite the fact the all configs
        // specify the same provider class
        assertEquals(3, ppr.getProviders().length);

    }

    public void testInvalidConfig() {
        List<InputSource> invalid = new ArrayList<InputSource>();
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_1)));
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_2)));
        invalid.add(new InputSource(new StringReader(INVALID_CONFIG_3)));

        for (Object anInvalid : invalid) {
            try {
                Element xml = parseXML((InputSource) anInvalid, false);
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

    private static final String CONFIG_3 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "           <UserManager class=\"\">" +
            "           <param name=\"defaultDepth\" value=\"4\"/>" +
            "           <param name=\"autoExpandTree\" value=\"true\"/>" +
            "           <param name=\"autoExpandSize\" value=\"2000\"/>" +
            "           </UserManager>" +
            "           <UserIdClass class=\"org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal\"/>" +
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

    private static final String USER_MANAGER_CONFIG_INVALID =
                    "    <Security appName=\"Jackrabbit\">" +
                    "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
                    "           <UserManager class=\"org.apache.jackrabbit.core.security.user.NonExisting\" />" +
                    "        </SecurityManager>" +
                    "    </Security>";

    private static final String USER_MANAGER_CONFIG_IMPL =
                    "    <Security appName=\"Jackrabbit\">" +
                    "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
                    "           <UserManager class=\"org.apache.jackrabbit.core.security.user.UserManagerImpl\" />" +
                    "        </SecurityManager>" +
                    "    </Security>";

    private static final String USER_MANAGER_CONFIG_DERIVED =
                    "    <Security appName=\"Jackrabbit\">" +
                    "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
                    "           <UserManager class=\"org.apache.jackrabbit.core.security.user.UserPerWorkspaceUserManager\" />" +
                    "        </SecurityManager>" +
                    "    </Security>";

    private static final String USER_MANAGER_CONFIG_WITH_ACTIONS =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "           <UserManager class=\"org.apache.jackrabbit.core.security.user.UserManagerImpl\">" +
            "              <AuthorizableAction class=\"org.apache.jackrabbit.core.security.user.action.AccessControlAction\">" +
            "                 <param name=\"groupPrivilegeNames\" value=\"jcr:read, jcr:write\"/>" +
            "                 <param name=\"userPrivilegeNames\" value=\" jcr:read    ,  jcr:readAccessControl  \"/>" +
            "              </AuthorizableAction>" +
            "              <AuthorizableAction class=\"org.apache.jackrabbit.core.security.user.action.ClearMembershipAction\"/>" +
            "           </UserManager>" +
            "           <UserIdClass class=\"org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal\"/>" +
            "        </SecurityManager>" +
            "    </Security>";

    private static final String USER_MANAGER_CONFIG_WITH_INVALID_ACTIONS =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "           <UserManager class=\"org.apache.jackrabbit.core.security.user.UserManagerImpl\">" +
            "              <AuthorizableAction class=\"org.apache.jackrabbit.core.security.user.action.NonExistingAction\"/>" +
            "           </UserManager>" +
            "           <UserIdClass class=\"org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal\"/>" +
            "        </SecurityManager>" +
            "    </Security>";

    private static final String USER_MANAGER_CONFIG_WITH_INVALID_ACTIONS_2 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "           <UserManager class=\"org.apache.jackrabbit.core.security.user.UserManagerImpl\">" +
            "              <AuthorizableAction class=\"org.apache.jackrabbit.core.security.user.action.AccessControlAction\">" +
            "                 <param name=\"invalidParam\" value=\"any value\"/>" +
            "              </AuthorizableAction>" +
            "           </UserManager>" +
            "           <UserIdClass class=\"org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal\"/>" +
            "        </SecurityManager>" +
            "    </Security>";

    private static final String PRINCIPAL_PROVIDER_CONFIG =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "        </SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.DefaultAccessManager\">" +
            "        </AccessManager>" +
            "        <LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">" +
            "           <param name=\"anonymousId\" value=\"anonymous\"/>" +
            "           <param name=\"adminId\" value=\"admin\"/>" +
            "           <param name=\"principalProvider\" value=\"org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider\"/>" +
            "        </LoginModule>\n" +
            "    </Security>";

    private static final String PRINCIPAL_PROVIDER_CONFIG1 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "        </SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.DefaultAccessManager\">" +
            "        </AccessManager>" +
            "        <LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">" +
            "           <param name=\"anonymousId\" value=\"anonymous\"/>" +
            "           <param name=\"adminId\" value=\"admin\"/>" +
            "           <param name=\"principalProvider\" value=\"org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider\"/>" +
            "           <param name=\"principal_provider.name\" value=\"test\"/>" +
            "        </LoginModule>\n" +
            "    </Security>";

    private static final String PRINCIPAL_PROVIDER_CONFIG2 =
            "    <Security appName=\"Jackrabbit\">" +
            "        <SecurityManager class=\"org.apache.jackrabbit.core.DefaultSecurityManager\" workspaceName=\"security\">" +
            "        </SecurityManager>" +
            "        <AccessManager class=\"org.apache.jackrabbit.core.security.DefaultAccessManager\">" +
            "        </AccessManager>" +
            "        <LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">" +
            "           <param name=\"anonymousId\" value=\"anonymous\"/>" +
            "           <param name=\"adminId\" value=\"admin\"/>" +
            "           <param name=\"principal_provider.class\" value=\"org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider\"/>" +
            "           <param name=\"principal_provider.name\" value=\"test2\"/>" +
            "        </LoginModule>\n" +
            "    </Security>";
}
