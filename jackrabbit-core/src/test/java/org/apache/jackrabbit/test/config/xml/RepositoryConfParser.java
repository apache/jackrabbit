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
package org.apache.jackrabbit.test.config.xml;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.apache.jackrabbit.test.config.AccessManagerConf;
import org.apache.jackrabbit.test.config.ClusterConf;
import org.apache.jackrabbit.test.config.FileSystemConf;
import org.apache.jackrabbit.test.config.JournalConf;
import org.apache.jackrabbit.test.config.LoginModuleConf;
import org.apache.jackrabbit.test.config.PersistenceManagerConf;
import org.apache.jackrabbit.test.config.RepositoryConf;
import org.apache.jackrabbit.test.config.SearchConf;
import org.apache.jackrabbit.test.config.SecurityConf;
import org.apache.jackrabbit.test.config.VersioningConf;
import org.apache.jackrabbit.test.config.WorkspaceConf;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RepositoryConfParser extends ConfParser {

    public static RepositoryConf read(String file) throws ConfException {
    	return read(new File(file));
    }
    
    public static RepositoryConf read(File file) throws ConfException {
		URI uri = file.toURI();
		return read(new InputSource(uri.toString()));
    }
    
    public static RepositoryConf read(InputSource xml) throws ConfException {
    	RepositoryConfParser parser = new RepositoryConfParser();
        Element root = parser.parseXML(xml);
    	return parser.parseRepositoryConf(root);
    }
    
//    public static WorkspaceConf readWorkspaceConf(InputSource xml) throws ConfException {
//    	RepositoryConfParser parser = new RepositoryConfParser();
//        Element root = parser.parseXML(xml);
//    	return parser.parseWorkspaceConf(root);
//    }
    
    protected RepositoryConf parseRepositoryConf(Element root) throws ConfException {
        // Repository home directory
        // (variable will be replaced later when converting RepositoryConf to RepositoryConfig)
        String home = "${" + Xml.REPOSITORY_HOME_VARIABLE + "}";

        // File system implementation
        FileSystemConf fsc =
            new FileSystemConf(parseBeanConf(root, Xml.FILE_SYSTEM_ELEMENT));

        // Security Configuration and access manager implementation
        Element security = getElement(root, Xml.SECURITY_ELEMENT);
        SecurityConf securityConf = parseSecurityConf(security);

        // General workspace Configuration
        Element workspaces = getElement(root, Xml.WORKSPACES_ELEMENT);
        String workspaceDirectory = getAttribute(workspaces, Xml.ROOT_PATH_ATTRIBUTE);

        String workspaceConfDirectory =
                getAttribute(workspaces, Xml.CONFIG_ROOT_PATH_ATTRIBUTE, null);

        String defaultWorkspace = getAttribute(workspaces, Xml.DEFAULT_WORKSPACE_ATTRIBUTE);

        int maxIdleTime = Integer.parseInt(
                getAttribute(workspaces, Xml.MAX_IDLE_TIME_ATTRIBUTE, "0"));

        // Workspace Configuration template
        WorkspaceConf wcTemplate = parseWorkspaceConf(getElement(root, Xml.WORKSPACE_ELEMENT));

        // Versioning Configuration
        VersioningConf vc = parseVersioningConf(root);

        // Optional search Configuration
        SearchConf sc = parseSearchConf(root);

        // Optional journal Configuration
        ClusterConf cc = parseClusterConf(root);
        
        return new RepositoryConf(home, securityConf, fsc,
                workspaceDirectory, workspaceConfDirectory, defaultWorkspace,
                maxIdleTime, wcTemplate, vc, sc, cc);
    }

    protected WorkspaceConf parseWorkspaceConf(Element element) throws ConfException {
		// Workspace name
		String name = getAttribute(element, NAME_ATTRIBUTE);
		
		// Clustered attribute
		boolean clustered = Boolean.valueOf(
		        getAttribute(element, Xml.CLUSTERED_ATTRIBUTE, "true")).booleanValue();
		
		// File system implementation
		FileSystemConf fsc = new FileSystemConf(parseBeanConf(element, Xml.FILE_SYSTEM_ELEMENT));
		
		// Persistence manager implementation
		PersistenceManagerConf pmc = parsePersistenceManagerConf(element);
		
		// Search implementation (optional)
		SearchConf sc = parseSearchConf(element);
		
		return new WorkspaceConf(name, clustered, fsc, pmc, sc);
	}
	
    protected SecurityConf parseSecurityConf(Element security)
            throws ConfException {
        String appName = getAttribute(security, Xml.APP_NAME_ATTRIBUTE);
        AccessManagerConf amc = parseAccessManagerConf(security);
        LoginModuleConf lmc = parseLoginModuleConf(security);
        return new SecurityConf(appName, amc, lmc);
    }

    protected AccessManagerConf parseAccessManagerConf(Element security)
            throws ConfException {
        return new AccessManagerConf(
                parseBeanConf(security, Xml.ACCESS_MANAGER_ELEMENT));
    }

    protected LoginModuleConf parseLoginModuleConf(Element security)
            throws ConfException {
        // Optional login module
        Element loginModule = getElement(security, Xml.LOGIN_MODULE_ELEMENT, false);

        if (loginModule != null) {
            return new LoginModuleConf(parseBeanConf(security, Xml.LOGIN_MODULE_ELEMENT));
        } else {
            return null;
        }
    }

    protected SearchConf parseSearchConf(Element parent)
            throws ConfException {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && Xml.SEARCH_INDEX_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                // Search implementation class
                String className = getAttribute(
                        element, Xml.CLASS_ATTRIBUTE, Xml.DEFAULT_QUERY_HANDLER);

                // Search parameters
                Properties parameters = parseParameters(element);

                // Optional file system implementation
                FileSystemConf fsc = null;
                if (getElement(element, Xml.FILE_SYSTEM_ELEMENT, false) != null) {
                    fsc = new FileSystemConf(
                            parseBeanConf(element, Xml.FILE_SYSTEM_ELEMENT));
                }

                return new SearchConf(className, parameters, fsc);
            }
        }
        return null;
    }

    protected VersioningConf parseVersioningConf(Element parent)
            throws ConfException {
        Element element = getElement(parent, Xml.VERSIONING_ELEMENT);

        // Versioning home directory
        String home = getAttribute(element, Xml.ROOT_PATH_ATTRIBUTE);

        // File system implementation
        FileSystemConf fsc = new FileSystemConf(
                parseBeanConf(element, Xml.FILE_SYSTEM_ELEMENT));

        // Persistence manager implementation
        PersistenceManagerConf pmc = parsePersistenceManagerConf(element);

        return new VersioningConf(home, fsc, pmc);
    }

    protected ClusterConf parseClusterConf(Element parent)
            throws ConfException {

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && Xml.CLUSTER_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                String id = getAttribute(element, Xml.ID_ATTRIBUTE, null);
                long syncDelay = Long.parseLong(
                        getAttribute(element, Xml.SYNC_DELAY_ATTRIBUTE, Xml.DEFAULT_SYNC_DELAY));

                JournalConf jc = parseJournalConf(element);
                return new ClusterConf(id, syncDelay, jc);
            }
        }
        return null;
    }

    protected JournalConf parseJournalConf(Element cluster)
            throws ConfException {

        return new JournalConf(
                parseBeanConf(cluster, Xml.JOURNAL_ELEMENT));
    }

    protected PersistenceManagerConf parsePersistenceManagerConf(
            Element parent) throws ConfException {
        return new PersistenceManagerConf(
                parseBeanConf(parent, Xml.PERSISTENCE_MANAGER_ELEMENT));
    }

}
