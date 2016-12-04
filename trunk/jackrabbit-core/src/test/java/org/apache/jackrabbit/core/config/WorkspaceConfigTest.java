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

import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.user.UserImporter;
import org.apache.jackrabbit.core.xml.ProtectedItemImporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Test cases for workspace configuration handling.
 */
public class WorkspaceConfigTest extends TestCase {

    private RepositoryConfigurationParser parser;

    protected void setUp() {
        Properties variables = new Properties();
        variables.setProperty("wsp.home", "target");
        parser = new RepositoryConfigurationParser(variables);
    }

    /**
     * Test that a standard workspace configuration file is
     * correctly parsed.
     *
     * @throws Exception on errors
     */
    public void testWorkspaceXml() throws Exception {
        InputStream xml = getClass().getClassLoader().getResourceAsStream(
                "org/apache/jackrabbit/core/config/workspace.xml");
        WorkspaceConfig config =
            parser.parseWorkspaceConfig(new InputSource(xml));

        assertEquals("target", config.getHomeDir());
        assertEquals("default", config.getName());

        PersistenceManagerConfig pmc = config.getPersistenceManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.persistence.obj.ObjectPersistenceManager",
                pmc.getClassName());
        assertTrue(pmc.getParameters().isEmpty());

        assertTrue(config.isSearchEnabled());

        WorkspaceSecurityConfig ws = config.getSecurityConfig();
        if (ws != null) {
            BeanConfig ppfConfig =  ws.getAccessControlProviderConfig();
            if (ppfConfig != null) {
                ppfConfig.newInstance(AccessControlProvider.class);
            }
        }
    }

    public void testImportConfig() throws Exception {
        // XML_1 ---------------------------------------------------------------
        Element xml = parseXML(new InputSource(new StringReader(XML_1)), true);
        ImportConfig config = parser.parseImportConfig(xml);

        List<? extends ProtectedItemImporter> ln = config.getProtectedItemImporters();
        assertEquals(2, ln.size());

        // XML_2 ---------------------------------------------------------------
        xml = parseXML(new InputSource(new StringReader(XML_2)), true);
        config = parser.parseImportConfig(xml);

        ln = config.getProtectedItemImporters();
        assertTrue(ln.isEmpty());

        // XML_3 ---------------------------------------------------------------
        xml = parseXML(new InputSource(new StringReader(XML_3)), true);
        config = parser.parseImportConfig(xml);

        ln = config.getProtectedItemImporters();
        assertEquals(4, ln.size());

        // XML_4 ---------------------------------------------------------------
        xml = parseXML(new InputSource(new StringReader(XML_4)), true);
        config = parser.parseImportConfig(xml);

        ln = config.getProtectedItemImporters();
        assertEquals(2, ln.size());

        // XML_5 ---------------------------------------------------------------
        xml = parseXML(new InputSource(new StringReader(XML_5)), true);
        config = parser.parseImportConfig(xml);

        List<? extends ProtectedItemImporter> lp = config.getProtectedItemImporters();
        assertEquals(1, lp.size());
        assertTrue(lp.get(0) instanceof UserImporter);
        assertEquals(UserImporter.ImportBehavior.NAME_BESTEFFORT, ((UserImporter)lp.get(0)).getImportBehavior());
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



    private static final String XML_1 =
            " <Workspace><Import>\n" +
                    "    <ProtectedNodeImporter class=\"org.apache.jackrabbit.core.xml.AccessControlImporter\"/>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.security.user.UserImporter\"/>\n" +
                    " </Import></Workspace>";

    private static final String XML_2 =
            " <Workspace><Import>\n" +
                    " </Import></Workspace>";

    private static final String XML_3 =
            " <Workspace><Import>\n" +
                    "    <ProtectedNodeImporter class=\"org.apache.jackrabbit.core.xml.AccessControlImporter\"/>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.security.user.UserImporter\"/>\n" +
                    "    <ProtectedNodeImporter class=\"org.apache.jackrabbit.core.xml.DefaultProtectedNodeImporter\"/>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.xml.DefaultProtectedPropertyImporter\"/>\n" +
                    " </Import></Workspace>";

    private static final String XML_4 =
            " <Workspace><Import>\n" +
                    "    <ProtectedNodeImporter class=\"org.apache.jackrabbit.core.xml.AccessControlImporter\"/>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.security.user.UserImporter\"/>\n" +
                    "    <ProtectedNodeImporter class=\"org.apache.jackrabbit.core.InvalidImporter\"/>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.InvalidImporter\"/>\n" +
                    " </Import></Workspace>";

    private static final String XML_5 =
            " <Workspace><Import>\n" +
                    "    <ProtectedPropertyImporter class=\"org.apache.jackrabbit.core.security.user.UserImporter\">" +
                    "       <param name=\"importBehavior\" value=\"besteffort\"/>" +
                    "    </ProtectedPropertyImporter>\n" +
                    " </Import></Workspace>";
}
