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
package org.apache.jackrabbit.core.security.authentication;

import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.ConfigurationEntityResolver;
import org.apache.jackrabbit.core.config.ConfigurationErrorHandler;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.core.security.authentication.token.TokenBasedAuthentication;
import org.apache.jackrabbit.core.security.principal.FallbackPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * <code>DefaultLoginModuleTest</code>...
 */
public class DefaultLoginModuleTest extends AbstractJCRTest {

    private static final String DEFAULT_CONFIG =
            "<Security appName=\"Jackrabbit\">" +
            "<LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">\n" +
            "   <param name=\"anonymousId\" value=\"anonymous\"/>\n" +
            "   <param name=\"adminId\" value=\"admin\"/>\n" +
            "</LoginModule>" +
            "</Security>";

    private static final String DISABLE_TOKEN_CONFIG =
            "<Security appName=\"Jackrabbit\">" +
            "<LoginModule class=\"org.apache.jackrabbit.core.security.authentication.DefaultLoginModule\">\n" +
            "   <param name=\"anonymousId\" value=\"anonymous\"/>\n" +
            "   <param name=\"adminId\" value=\"admin\"/>\n" +
            "   <param name=\"disableTokenAuth\" value=\"true\"/>\n" +
            "</LoginModule>" +
            "</Security>";
    
    private SimpleCredentials simpleCredentials = new SimpleCredentials("admin", "admin".toCharArray());
    private Session securitySession;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        RepositoryConfig rc = ((RepositoryImpl) superuser.getRepository()).getConfig();
        String workspaceName = rc.getSecurityConfig().getSecurityManagerConfig().getWorkspaceName();
        if (workspaceName == null) {
            workspaceName = rc.getDefaultWorkspaceName();
        }
        securitySession = getHelper().getSuperuserSession(workspaceName);
    }

    @Override
    protected void cleanUp() throws Exception {
        if (securitySession != null && securitySession.isLive()) {
            securitySession.logout();
        }
        super.cleanUp();
    }

    public void testSimpleCredentialsLogin() throws Exception {
        AuthContext ac = getAuthContext(simpleCredentials, DEFAULT_CONFIG);
        ac.login();
        ac.logout();
    }

    public void testSimpleCredentialsLoginLogout() throws Exception {
        AuthContext ac = getAuthContext(simpleCredentials, DEFAULT_CONFIG);
        ac.login();

        Subject subject = ac.getSubject();
        assertFalse(subject.getPrincipals().isEmpty());
        assertFalse(subject.getPublicCredentials().isEmpty());
        assertFalse(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());

        ac.logout();
        assertTrue(subject.getPrincipals().isEmpty());
        assertTrue(subject.getPublicCredentials().isEmpty());
        assertTrue(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());
    }

    public void testTokenCredentialsLoginLogout() throws Exception {
        simpleCredentials.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        try {
            // login with simple credentials forcing token creation.
            AuthContext ac = getAuthContext(simpleCredentials, DEFAULT_CONFIG);
            ac.login();

            Subject subject = ac.getSubject();

            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
            assertFalse(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());
            assertFalse(subject.getPublicCredentials(TokenCredentials.class).isEmpty());
            assertEquals(2, subject.getPublicCredentials(Credentials.class).size());

            TokenCredentials tokenCredentials = subject.getPublicCredentials(TokenCredentials.class).iterator().next();

            ac.logout();

            // second login with token credentials
            ac = getAuthContext(tokenCredentials, DEFAULT_CONFIG);
            ac.login();

            subject = ac.getSubject();

            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
            assertFalse(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());
            assertFalse(subject.getPublicCredentials(TokenCredentials.class).isEmpty());
            assertEquals(2, subject.getPublicCredentials(Credentials.class).size());

            ac.logout();
            assertTrue(subject.getPrincipals().isEmpty());            
            assertTrue(subject.getPublicCredentials().isEmpty());
            assertTrue(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());
            assertTrue(subject.getPublicCredentials(TokenCredentials.class).isEmpty());
        } finally {
            simpleCredentials.removeAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE);
        }
    }

    public void testDisabledTokenCredentials() throws Exception {
        simpleCredentials.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        try {
            AuthContext ac = getAuthContext(simpleCredentials, DISABLE_TOKEN_CONFIG);
            ac.login();

            Subject subject = ac.getSubject();

            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
            assertFalse(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());
            assertTrue(subject.getPublicCredentials(TokenCredentials.class).isEmpty());
            assertEquals(1, subject.getPublicCredentials(Credentials.class).size());

            ac.logout();
        } finally {
            simpleCredentials.removeAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE);
        }
    }

    public void testDisabledTokenCredentials2() throws Exception {
        simpleCredentials.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        try {
            AuthContext ac = getAuthContext(simpleCredentials, DEFAULT_CONFIG);
            ac.login();
            Subject subj = ac.getSubject();
            assertFalse(subj.getPublicCredentials(SimpleCredentials.class).isEmpty());
            assertFalse(subj.getPublicCredentials(TokenCredentials.class).isEmpty());

            TokenCredentials tokenCredentials = subj.getPublicCredentials(TokenCredentials.class).iterator().next();
            ac.logout();

            // test login with token credentials
            ac = getAuthContext(tokenCredentials, DEFAULT_CONFIG);
            ac.login();
            ac.logout();

            // test login with token credentials if token-auth is disabled.
            try {
                ac = getAuthContext(tokenCredentials, DISABLE_TOKEN_CONFIG);
                ac.login();
                ac.logout();
                fail();
            } catch (LoginException e) {
                // success
            }

        } finally {
            simpleCredentials.removeAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE);
        }
    }

    public void testTokenConfigurationWithJaas() throws Exception {
        // define the location of the JAAS configuration
        System.setProperty(
                "java.security.auth.login.config",
                "target/test-classes/jaas.config");

        simpleCredentials.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
        try {
            AuthContext ac = getJAASAuthContext(simpleCredentials, "defaultLoginModuleTest");
            ac.login();

            Subject subject = ac.getSubject();

            assertFalse(subject.getPrincipals().isEmpty());
            assertFalse(subject.getPublicCredentials().isEmpty());
            assertFalse(subject.getPublicCredentials(SimpleCredentials.class).isEmpty());

            assertTrue(subject.getPublicCredentials(TokenCredentials.class).isEmpty());

            assertEquals(1, subject.getPublicCredentials(Credentials.class).size());

            ac.logout();
        } finally {
            simpleCredentials.removeAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE);
        }
    }

    private AuthContext getAuthContext(Credentials creds, String config) throws RepositoryException {
        CallbackHandler ch = new CallbackHandlerImpl(creds,
                securitySession, new ProviderRegistryImpl(new FallbackPrincipalProvider()),
                "admin", "anonymous");
        return new LocalAuthContext(getLoginModuleConfig(config), ch, null);
    }

    private AuthContext getJAASAuthContext(Credentials creds, String appName) {
        CallbackHandler ch = new CallbackHandlerImpl(creds,
                securitySession, new ProviderRegistryImpl(new FallbackPrincipalProvider()),
                "admin", "anonymous");
        return new JAASAuthContext(appName, ch, null);
    }

    private static LoginModuleConfig getLoginModuleConfig(String config) throws ConfigurationException {
        return new RepositoryConfigurationParser(new Properties()).parseLoginModuleConfig(parseXML(new InputSource(new StringReader(config)), false));
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
}