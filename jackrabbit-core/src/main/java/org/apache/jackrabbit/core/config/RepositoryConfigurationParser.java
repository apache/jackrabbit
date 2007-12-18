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

import java.io.File;
import java.util.Properties;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Configuration parser. This class is used to parse the repository and
 * workspace configuration files.
 * <p>
 * The following code sample outlines the usage of this class:
 * <pre>
 *     Properties variables = ...; // parser variables
 *     RepositoryConfigurationParser parser =
 *         new RepositoryConfigurationParser(variables);
 *     RepositoryConfig rc = parser.parseRepositoryConfig(...);
 *     WorkspaceConfig wc = parser.parseWorkspaceConfig(...);
 * </pre>
 * <p>
 * Note that the configuration objects returned by this parser are not
 * initialized. The caller needs to initialize the configuration objects
 * before using them.
 */
public class RepositoryConfigurationParser extends ConfigurationParser {

    /** Name of the repository home directory parser variable. */
    public static final String REPOSITORY_HOME_VARIABLE = "rep.home";

    /** Name of the workspace home directory parser variable. */
    public static final String WORKSPACE_HOME_VARIABLE = "wsp.home";

    /** Name of the repository name parser variable. */
    public static final String WORKSPACE_NAME_VARIABLE = "wsp.name";

    /** Name of the security configuration element. */
    public static final String SECURITY_ELEMENT = "Security";

    /** Name of the access manager configuration element. */
    public static final String ACCESS_MANAGER_ELEMENT = "AccessManager";

    /** Name of the login module configuration element. */
    public static final String LOGIN_MODULE_ELEMENT = "LoginModule";

    /** Name of the general workspace configuration element. */
    public static final String WORKSPACES_ELEMENT = "Workspaces";

    /** Name of the workspace configuration element. */
    public static final String WORKSPACE_ELEMENT = "Workspace";

    /** Name of the versioning configuration element. */
    public static final String VERSIONING_ELEMENT = "Versioning";

    /** Name of the file system configuration element. */
    public static final String FILE_SYSTEM_ELEMENT = "FileSystem";

    /** Name of the cluster configuration element. */
    public static final String CLUSTER_ELEMENT = "Cluster";

    /** Name of the journal configuration element. */
    public static final String JOURNAL_ELEMENT = "Journal";
    
    /** Name of the data store configuration element. */
    public static final String DATA_STORE_ELEMENT = "DataStore";    

    /** Name of the persistence manager configuration element. */
    public static final String PERSISTENCE_MANAGER_ELEMENT =
        "PersistenceManager";

    /** Name of the search index configuration element. */
    public static final String SEARCH_INDEX_ELEMENT = "SearchIndex";

    /** Name of the ism locking configuration element. */
    public static final String ISM_LOCKING_ELEMENT = "ISMLocking";

    /** Name of the application name configuration attribute. */
    public static final String APP_NAME_ATTRIBUTE = "appName";

    /** Name of the root path configuration attribute. */
    public static final String ROOT_PATH_ATTRIBUTE = "rootPath";

    /** Name of the config root path configuration attribute. */
    public static final String CONFIG_ROOT_PATH_ATTRIBUTE = "configRootPath";

    /** Name of the maximum idle time configuration attribute. */
    public static final String MAX_IDLE_TIME_ATTRIBUTE = "maxIdleTime";

    /** Name of the default workspace configuration attribute. */
    public static final String DEFAULT_WORKSPACE_ATTRIBUTE =
        "defaultWorkspace";

    /** Name of the id configuration attribute. */
    public static final String ID_ATTRIBUTE = "id";

    /** Name of the syncDelay configuration attribute. */
    public static final String SYNC_DELAY_ATTRIBUTE = "syncDelay";

    /** Name of the default search index implementation class. */
    public static final String DEFAULT_QUERY_HANDLER =
        "org.apache.jackrabbit.core.query.lucene.SearchIndex";

    /** Name of the clustered configuration attribute. */
    public static final String CLUSTERED_ATTRIBUTE = "clustered";

    /** Default synchronization delay, in milliseconds. */
    public static final String DEFAULT_SYNC_DELAY = "5000";

    /**
     * Creates a new configuration parser with the given parser variables.
     *
     * @param variables parser variables
     */
    public RepositoryConfigurationParser(Properties variables) {
        super(variables);
    }

    /**
     * Parses repository configuration. Repository configuration uses the
     * following format:
     * <pre>
     *   &lt;Repository&gt;
     *     &lt;FileSystem ...&gt;
     *     &lt;Security appName="..."&gt;
     *       &lt;AccessManager ...&gt;
     *       &lt;LoginModule ... (optional)&gt;
     *     &lt;/Security&gt;
     *     &lt;Workspaces rootPath="..." defaultWorkspace="..."/&gt;
     *     &lt;Workspace ...&gt;
     *     &lt;Versioning ...&gt;
     *   &lt;/Repository&gt;
     * </pre>
     * <p>
     * The <code>FileSystem</code> element is a
     * {@link #parseBeanConfig(Element,String) bean configuration} element,
     * that specifies the file system implementation for storing global
     * repository information. The <code>Security</code> element contains
     * an <code>AccessManager</code> bean configuration element and the
     * JAAS name of the repository application. The <code>Workspaces</code>
     * element contains general workspace parameters, and the
     * <code>Workspace</code> element is a template for the individual
     * workspace configuration files. The <code>Versioning</code> element
     * contains
     * {@link #parseVersioningConfig(Element) versioning configuration} for
     * the repository.
     * <p>
     * In addition to the configured information, the returned repository
     * configuration object also contains the repository home directory path
     * that is given as the ${rep.home} parser variable. Note that the
     * variable <em>must</em> be available for the configuration document to
     * be correctly parsed.
     * <p>
     * {@link #replaceVariables(String) Variable replacement} is performed
     * on the security application name attribute, the general workspace
     * configuration attributes, and on the file system, access manager,
     * and versioning configuration information.
     * <p>
     * Note that the returned repository configuration object has not been
     * initialized.
     *
     * @param xml repository configuration document
     * @return repository configuration
     * @throws ConfigurationException if the configuration is broken
     * @see #parseBeanConfig(Element, String)
     * @see #parseVersioningConfig(Element)
     */
    public RepositoryConfig parseRepositoryConfig(InputSource xml)
            throws ConfigurationException {
        Element root = parseXML(xml);

        // Repository home directory
        String home = getVariables().getProperty(REPOSITORY_HOME_VARIABLE);

        // File system implementation
        FileSystemConfig fsc =
            new FileSystemConfig(parseBeanConfig(root, FILE_SYSTEM_ELEMENT));

        // Security configuration and access manager implementation
        Element security = getElement(root, SECURITY_ELEMENT);
        SecurityConfig securityConfig = parseSecurityConfig(security);

        // General workspace configuration
        Element workspaces = getElement(root, WORKSPACES_ELEMENT);
        String workspaceDirectory = replaceVariables(
                getAttribute(workspaces, ROOT_PATH_ATTRIBUTE));

        String workspaceConfigDirectory =
                getAttribute(workspaces, CONFIG_ROOT_PATH_ATTRIBUTE, null);

        String defaultWorkspace = replaceVariables(
                getAttribute(workspaces, DEFAULT_WORKSPACE_ATTRIBUTE));

        int maxIdleTime = Integer.parseInt(
                getAttribute(workspaces, MAX_IDLE_TIME_ATTRIBUTE, "0"));

        // Workspace configuration template
        Element template = getElement(root, WORKSPACE_ELEMENT);

        // Versioning configuration
        VersioningConfig vc = parseVersioningConfig(root);

        // Optional search configuration
        SearchConfig sc = parseSearchConfig(root);

        // Optional journal configuration
        ClusterConfig cc = parseClusterConfig(root);
        
        // Optional data store configuration
        DataStoreConfig dsc = parseDataStoreConfig(root);

        return new RepositoryConfig(home, securityConfig, fsc,
                workspaceDirectory, workspaceConfigDirectory, defaultWorkspace,
                maxIdleTime, template, vc, sc, cc, dsc, this);
    }

    /**
     * Parses security configuration. Security configuration
     * uses the following format:
     * <pre>
     *   &lt;Security appName="..."&gt;
     *     &lt;AccessManager ...&gt;
     *     &lt;LoginModule ... (optional)&gt;
     *   &lt;/Security&gt;
     * </pre>
     * <p/>
     * Both the <code>AccessManager</code> and <code>LoginModule</code>
     * elements are {@link #parseBeanConfig(Element,String) bean configuration}
     * elements.
     * <p/>
     * The login module is an optional feature of repository configuration.
     *
     * @param security the &lt;security> element.
     * @return the security configuration.
     * @throws ConfigurationException if the configuration is broken
     */
    public SecurityConfig parseSecurityConfig(Element security)
            throws ConfigurationException {
        String appName = getAttribute(security, APP_NAME_ATTRIBUTE);
        AccessManagerConfig amc = parseAccessManagerConfig(security);
        LoginModuleConfig lmc = parseLoginModuleConfig(security);
        return new SecurityConfig(appName, amc, lmc);
    }

    /**
     * Parses the access manager configuration.
     *
     * @param security the &lt;security> element.
     * @return the access manager configuration.
     * @throws ConfigurationException if the configuration is broken
     */
    public AccessManagerConfig parseAccessManagerConfig(Element security)
            throws ConfigurationException {
        return new AccessManagerConfig(
                parseBeanConfig(security, ACCESS_MANAGER_ELEMENT));
    }

    /**
     * Parses the login module configuration.
     *
     * @param security the &lt;security> element.
     * @return the login module configuration.
     * @throws ConfigurationException if the configuration is broken
     */
    public LoginModuleConfig parseLoginModuleConfig(Element security)
            throws ConfigurationException {
        // Optional login module
        Element loginModule = getElement(security, LOGIN_MODULE_ELEMENT, false);

        if (loginModule != null) {
            return new LoginModuleConfig(parseBeanConfig(security, LOGIN_MODULE_ELEMENT));
        } else {
            return null;
        }
    }

    /**
     * Parses workspace configuration. Workspace configuration uses the
     * following format:
     * <pre>
     *   &lt;Workspace name="..."&gt;
     *     &lt;FileSystem ...&gt;
     *     &lt;PersistenceManager ...&gt;
     *     &lt;SearchIndex ...&gt;
     *     &lt;ISMLocking ...&gt;
     *   &lt;/Workspace&gt;
     * </pre>
     * <p>
     * All the child elements (<code>FileSystem</code>,
     * <code>PersistenceManager</code>, and <code>SearchIndex</code>) are
     * {@link #parseBeanConfig(Element,String) bean configuration} elements.
     * In addition to bean configuration, the
     * {@link #parseSearchConfig(Element) search element} also contains
     * configuration for the search file system.
     * <p>
     * In addition to the configured information, the returned workspace
     * configuration object also contains the workspace home directory path
     * that is given as the ${wsp.home} parser variable. Note that the
     * variable <em>must</em> be available for the configuration document to
     * be correctly parsed.
     * <p>
     * Variable replacement is performed on the optional workspace name
     * attribute. If the name is not given, then the name of the workspace
     * home directory is used as the workspace name. Once the name has been
     * determined, it will be added as the ${wsp.name} variable in a temporary
     * configuration parser that is used to parse the contained configuration
     * elements.
     * <p>
     * The search index configuration element is optional. If it is not given,
     * then the workspace will not have search capabilities.
     * <p>
     * The ism locking configuration element is optional. If it is not given,
     * then a default implementation is used.
     * <p>
     * Note that the returned workspace configuration object has not been
     * initialized.
     *
     * @param xml workspace configuration document
     * @return workspace configuration
     * @throws ConfigurationException if the configuration is broken
     * @see #parseBeanConfig(Element, String)
     * @see #parseSearchConfig(Element)
     */
    public WorkspaceConfig parseWorkspaceConfig(InputSource xml)
            throws ConfigurationException {
        Element root = parseXML(xml);

        // Workspace home directory
        String home = getVariables().getProperty(WORKSPACE_HOME_VARIABLE);

        // Workspace name
        String name =
            getAttribute(root, NAME_ATTRIBUTE, new File(home).getName());

        // Clustered attribute
        boolean clustered = Boolean.valueOf(
                getAttribute(root, CLUSTERED_ATTRIBUTE, "true")).booleanValue();

        // Create a temporary parser that contains the ${wsp.name} variable
        Properties tmpVariables = (Properties) getVariables().clone();
        tmpVariables.put(WORKSPACE_NAME_VARIABLE, name);
        RepositoryConfigurationParser tmpParser = createSubParser(tmpVariables);

        // File system implementation
        FileSystemConfig fsc = new FileSystemConfig(
                tmpParser.parseBeanConfig(root, FILE_SYSTEM_ELEMENT));

        // Persistence manager implementation
        PersistenceManagerConfig pmc = tmpParser.parsePersistenceManagerConfig(root);

        // Search implementation (optional)
        SearchConfig sc = tmpParser.parseSearchConfig(root);

        // Item state manager locking configuration (optional)
        ISMLockingConfig ismLockingConfig = tmpParser.parseISMLockingConfig(root);

        return new WorkspaceConfig(home, name, clustered, fsc, pmc, sc, ismLockingConfig);
    }

    /**
     * Parses search index configuration. Search index configuration
     * uses the following format:
     * <pre>
     *   &lt;SearchIndex class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *     &lt;FileSystem ...&gt;
     *   &lt;/Search&gt;
     * </pre>
     * <p/>
     * Both the <code>SearchIndex</code> and <code>FileSystem</code>
     * elements are {@link #parseBeanConfig(Element,String) bean configuration}
     * elements. If the search implementation class is not given, then
     * a default implementation is used.
     * <p/>
     * The search index is an optional feature of workspace configuration.
     * If the search configuration element is not found, then this method
     * returns <code>null</code>.
     * <p/>
     * The FileSystem element in a search index configuration is optional.
     * However some implementations may require a FileSystem.
     *
     * @param parent parent of the <code>SearchIndex</code> element
     * @return search configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected SearchConfig parseSearchConfig(Element parent)
            throws ConfigurationException {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && SEARCH_INDEX_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                // Search implementation class
                String className = getAttribute(
                        element, CLASS_ATTRIBUTE, DEFAULT_QUERY_HANDLER);

                // Search parameters
                Properties parameters = parseParameters(element);

                // Optional file system implementation
                FileSystemConfig fsc = null;
                if (getElement(element, FILE_SYSTEM_ELEMENT, false) != null) {
                    fsc = new FileSystemConfig(
                            parseBeanConfig(element, FILE_SYSTEM_ELEMENT));
                }

                return new SearchConfig(className, parameters, fsc);
            }
        }
        return null;
    }

    /**
     * Parses ism locking configuration. ism locking configuration  uses the
     * following format:
     * <pre>
     *   &lt;ISMLocking class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/ISMLocking&gt;
     * </pre>
     * <p/>
     * The <code>ISMLocking</code> is a
     * {@link #parseBeanConfig(Element,String) bean configuration} element.
     * <p/>
     * The ism locking is an optional part of the  workspace configuration. If
     * the ism locking element is not found, then this method returns
     * <code>null</code>.
     *
     * @param parent parent of the <code>ISMLocking</code> element
     * @return search configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected ISMLockingConfig parseISMLockingConfig(Element parent)
            throws ConfigurationException {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && ISM_LOCKING_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                // ism locking implementation class
                String className = getAttribute(element, CLASS_ATTRIBUTE);

                // ism locking parameters
                Properties parameters = parseParameters(element);

                return new ISMLockingConfig(className, parameters);
            }
        }
        return null;
    }

    /**
     * Parses versioning configuration. Versioning configuration uses the
     * following format:
     * <pre>
     *   &lt;Versioning rootPath="..."&gt;
     *     &lt;FileSystem ...&gt;
     *     &lt;PersistenceManager ...&gt;
     *   &lt;/Versioning&gt;
     * </pre>
     * <p>
     * Both the <code>FileSystem</code> and <code>PersistenceManager</code>
     * elements are {@link #parseBeanConfig(Element,String) bean configuration}
     * elements. In addition to the bean parameter values,
     * {@link #replaceVariables(String) variable replacement} is performed
     * also on the versioning root path attribute.
     *
     * @param parent parent of the <code>Versioning</code> element
     * @return versioning configuration
     * @throws ConfigurationException if the configuration is broken
     */
    protected VersioningConfig parseVersioningConfig(Element parent)
            throws ConfigurationException {
        Element element = getElement(parent, VERSIONING_ELEMENT);

        // Versioning home directory
        String home =
            replaceVariables(getAttribute(element, ROOT_PATH_ATTRIBUTE));

        // File system implementation
        FileSystemConfig fsc = new FileSystemConfig(
                parseBeanConfig(element, FILE_SYSTEM_ELEMENT));

        // Persistence manager implementation
        PersistenceManagerConfig pmc = parsePersistenceManagerConfig(element);

        // Item state manager locking configuration (optional)
        ISMLockingConfig ismLockingConfig = parseISMLockingConfig(element);

        return new VersioningConfig(home, fsc, pmc, ismLockingConfig);
    }

    /**
     * Parses cluster configuration. Cluster configuration uses the following format:
     * <pre>
     *   &lt;Cluster&gt;
     *     &lt;Journal ...&gt;
     *   &lt;/Journal&gt;
     * </pre>
     * <p/>
     * <code>Cluster</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     * <p/>
     * Clustering is an optional feature. If the cluster element is not found, then this
     * method returns <code>null</code>.
     *
     * @param parent parent of the <code>Journal</code> element
     * @return journal configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected ClusterConfig parseClusterConfig(Element parent)
            throws ConfigurationException {

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && CLUSTER_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                String id = getAttribute(element, ID_ATTRIBUTE, null);
                long syncDelay = Long.parseLong(
                        getAttribute(element, SYNC_DELAY_ATTRIBUTE, DEFAULT_SYNC_DELAY));

                JournalConfig jc = parseJournalConfig(element);
                return new ClusterConfig(id, syncDelay, jc);
            }
        }
        return null;
    }

    /**
     * Parses journal configuration. Journal configuration uses the following format:
     * <pre>
     *   &lt;Journal class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/Journal&gt;
     * </pre>
     * <p/>
     * <code>Journal</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     *
     * @param cluster parent cluster element
     * @return journal configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected JournalConfig parseJournalConfig(Element cluster)
            throws ConfigurationException {

        return new JournalConfig(
                parseBeanConfig(cluster, JOURNAL_ELEMENT));
    }
    
    /**
     * Parses data store configuration. Data store configuration uses the following format:
     * <pre>
     *   &lt;DataStore class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/DataStore&gt;
     * </pre>
     * <p/>
     * <code>DataStore</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     *
     * @param cluster parent cluster element
     * @return journal configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected DataStoreConfig parseDataStoreConfig(Element parent)
            throws ConfigurationException {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && DATA_STORE_ELEMENT.equals(child.getNodeName())) {
                DataStoreConfig cfg = new DataStoreConfig(parseBeanConfig(
                        parent, DATA_STORE_ELEMENT));
                return cfg;
            }
        }
        return null;
    }    

    /**
     * Parses the PersistenceManager config.
     *
     * @param parent parent of the <code>PersistenceManager</code> element
     * @return persistence manager configuration
     * @throws ConfigurationException if the configuration is broken
     */
    protected PersistenceManagerConfig parsePersistenceManagerConfig(
            Element parent) throws ConfigurationException {
        return new PersistenceManagerConfig(
                parseBeanConfig(parent, PERSISTENCE_MANAGER_ELEMENT));
    }

    /**
     * Creates a new instance of a configuration parser but with overlayed
     * variables.
     *
     * @param variables the variables overlay
     * @return a new configuration parser instance
     */
    protected RepositoryConfigurationParser createSubParser(Properties variables) {
        // overlay the properties
        Properties props = new Properties(getVariables());
        props.putAll(variables);
        return new RepositoryConfigurationParser(props);
    }

}
