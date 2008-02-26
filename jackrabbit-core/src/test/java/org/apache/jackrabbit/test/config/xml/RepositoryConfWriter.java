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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.test.config.AccessManagerConf;
import org.apache.jackrabbit.test.config.BeanConf;
import org.apache.jackrabbit.test.config.ClusterConf;
import org.apache.jackrabbit.test.config.JournalConf;
import org.apache.jackrabbit.test.config.LoginModuleConf;
import org.apache.jackrabbit.test.config.PersistenceManagerConf;
import org.apache.jackrabbit.test.config.RepositoryConf;
import org.apache.jackrabbit.test.config.SearchConf;
import org.apache.jackrabbit.test.config.SecurityConf;
import org.apache.jackrabbit.test.config.VersioningConf;
import org.apache.jackrabbit.test.config.WorkspaceConf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>RepositoryConfigurationWriter</code> ...
 *
 */
public class RepositoryConfWriter {
	
	public static void write(RepositoryConf rc, String file) throws ConfException {
		write(rc, new File(file));
	}
	
	public static void write(RepositoryConf rc, File file) throws ConfException {
        Writer configWriter = null;
        try {
            configWriter = new FileWriter(file);
            
            Element root = createRepositoryConfElement(rc);
            
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(root), new StreamResult(configWriter));
        } catch (Exception e) {
            throw new ConfException(
                    "Failed to create repository configuration at path "
                    + file.getPath(), e);
        } finally {
            try {
            	if (configWriter != null) {
            		configWriter.close();
            	}
            } catch (IOException ignore) {
            }
        }
	}

	public static Element createRepositoryConfElement(RepositoryConf rc) throws ConfException {
		RepositoryConfWriter writer = new RepositoryConfWriter();
		Element root = writer.writeRepositoryConf(rc);
		// make it the root element
		writer.document.appendChild(root);
		return root;
	}

	public static Element createWorkspaceConfElement(WorkspaceConf wc) throws ConfException {
		RepositoryConfWriter writer = new RepositoryConfWriter();
		return writer.writeWorkspaceConf(wc);
	}

	/** Static factory for creating DOM DocumentBuilder instances. */
    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    
    protected Document document;
    
	protected RepositoryConfWriter() throws ConfException {
        try {
			document = BUILDER_FACTORY.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new ConfException("Could create SAX parser for RepositoryConfWriter", e);
		}
	}
	
	protected Element writeRepositoryConf(RepositoryConf rc) {
		Element element = document.createElement("Repository");
		element.appendChild(writeBeanConf(rc.getFileSystemConf(), Xml.FILE_SYSTEM_ELEMENT));
		element.appendChild(writeSecurityConf(rc.getSecurityConf()));
		
		Element workspaces = document.createElement(Xml.WORKSPACES_ELEMENT);
		workspaces.setAttribute(Xml.ROOT_PATH_ATTRIBUTE, rc.getWorkspaceDirectory());
		workspaces.setAttribute(Xml.CONFIG_ROOT_PATH_ATTRIBUTE, rc.getWorkspaceConfigDirectory());
		workspaces.setAttribute(Xml.DEFAULT_WORKSPACE_ATTRIBUTE, rc.getDefaultWorkspaceName());
		element.appendChild(workspaces);
		
		element.appendChild(writeWorkspaceConf(rc.getWorkspaceConfTemplate()));
		
		element.appendChild(writeVersioningConf(rc.getVersioningConf()));
		element.appendChild(writeSearchConf(rc.getSearchConf()));
		
		if (rc.getClusterConf() != null) {
			element.appendChild(writeClusterConf(rc.getClusterConf()));
		}
		
		return element;
	}
	
	protected Element writeWorkspaceConf(WorkspaceConf wc) {
		Element element = document.createElement(Xml.WORKSPACE_ELEMENT);
		element.setAttribute(Xml.NAME_ATTRIBUTE, wc.getName());
		element.setAttribute(Xml.CLUSTERED_ATTRIBUTE, wc.isClustered() ? "true" : "false");

		element.appendChild(writeBeanConf(wc.getFileSystemConf(), Xml.FILE_SYSTEM_ELEMENT));
		element.appendChild(writePersistenceManagerConf(wc.getPersistenceManagerConf()));
		element.appendChild(writeSearchConf(wc.getSearchConf()));
		return element;
	}
	
	protected Element writeBeanConf(BeanConf bc, String name) {
		Element element = document.createElement(name);
		element.setAttribute(Xml.CLASS_ATTRIBUTE, bc.getClassName());
		for (Enumeration keys = bc.getParameters().keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			String value = bc.getParameters().getProperty(key);
			
			Element child = document.createElement(Xml.PARAM_ELEMENT);
			child.setAttribute(Xml.NAME_ATTRIBUTE, key);
			child.setAttribute(Xml.VALUE_ATTRIBUTE, value);
			element.appendChild(child);
		}
		return element;
	}

	protected Element writeSecurityConf(SecurityConf sc) {
		Element element = document.createElement(Xml.SECURITY_ELEMENT);
		element.setAttribute(Xml.APP_NAME_ATTRIBUTE, sc.getAppName());
		
		element.appendChild(writeAccessManagerConf(sc.getAccessManagerConf()));
		
		if (sc.getLoginModuleConf() != null) {
			element.appendChild(writeLoginModuleConf(sc.getLoginModuleConf()));
		}
		return element;
	}

	protected Element writeAccessManagerConf(AccessManagerConf ac) {
		return writeBeanConf(ac, Xml.ACCESS_MANAGER_ELEMENT);
	}

	protected Element writeLoginModuleConf(LoginModuleConf lc) {
		return writeBeanConf(lc, Xml.LOGIN_MODULE_ELEMENT);
	}

	protected Element writeSearchConf(SearchConf sc) {
		Element element = writeBeanConf(sc, Xml.SEARCH_INDEX_ELEMENT);
		if (sc.getFileSystemConf() != null) {
			element.appendChild(writeBeanConf(sc.getFileSystemConf(), Xml.FILE_SYSTEM_ELEMENT));
		}
		return element;
	}

	protected Element writeVersioningConf(VersioningConf vc) {
		Element element = document.createElement(Xml.VERSIONING_ELEMENT);
		element.setAttribute(Xml.ROOT_PATH_ATTRIBUTE, vc.getHomeDir());
		
		element.appendChild(writeBeanConf(vc.getFileSystemConf(), Xml.FILE_SYSTEM_ELEMENT));
		element.appendChild(writePersistenceManagerConf(vc.getPersistenceManagerConf()));
		return element;
	}

	protected Element writeClusterConf(ClusterConf cc) {
		Element element = document.createElement(Xml.CLUSTER_ELEMENT);
		element.setAttribute(Xml.ID_ATTRIBUTE, cc.getId());
		element.setAttribute(Xml.SYNC_DELAY_ATTRIBUTE, Long.toString(cc.getSyncDelay()));
		
		element.appendChild(writeJournalConf(cc.getJournalConf()));
		return element;
	}

	protected Element writeJournalConf(JournalConf jc) {
		return writeBeanConf(jc, Xml.JOURNAL_ELEMENT);
	}

	protected Element writePersistenceManagerConf(PersistenceManagerConf pmc) {
		return writeBeanConf(pmc, Xml.PERSISTENCE_MANAGER_ELEMENT);
	}

}
