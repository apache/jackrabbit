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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreFactory;
import org.apache.jackrabbit.core.data.MultiDataStore;
import org.apache.jackrabbit.core.data.MultiDataStoreAware;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemFactory;
import org.apache.jackrabbit.core.journal.AbstractJournal;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.QueryHandlerFactory;
import org.apache.jackrabbit.core.state.DefaultISMLocking;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ISMLockingFactory;
import org.apache.jackrabbit.core.util.RepositoryLock;
import org.apache.jackrabbit.core.util.RepositoryLockMechanism;
import org.apache.jackrabbit.core.util.RepositoryLockMechanismFactory;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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

    /** Name of the repository configuration file parser variable. */
    public static final String REPOSITORY_CONF_VARIABLE = "rep.conf";

    /** Name of the workspace home directory parser variable. */
    public static final String WORKSPACE_HOME_VARIABLE = "wsp.home";

    /** Name of the repository name parser variable. */
    public static final String WORKSPACE_NAME_VARIABLE = "wsp.name";

    /** Name of the security configuration element. */
    public static final String SECURITY_ELEMENT = "Security";

    /** Name of the security manager configuration element. */
    public static final String SECURITY_MANAGER_ELEMENT = "SecurityManager";

    /** Name of the access manager configuration element. */
    public static final String ACCESS_MANAGER_ELEMENT = "AccessManager";

    /** Name of the login module configuration element. */
    public static final String LOGIN_MODULE_ELEMENT = "LoginModule";

    /**
     * Name of the optional WorkspaceAccessManager element defining which
     * implementation of WorkspaceAccessManager to be used.
     */
    private static final String WORKSPACE_ACCESS_ELEMENT = "WorkspaceAccessManager";

    /**
     * Name of the optional UserManagerConfig element that defines the
     * configuration options for the user manager.
     */
    private static final String USER_MANAGER_ELEMENT = "UserManager";

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

    /** Name of the data source configuration element. */
    public static final String DATASOURCES_ELEMENT = "DataSources";

    /** Name of the data source configuration element. */
    public static final String DATASOURCE_ELEMENT = "DataSource";

    /** Name of the journal configuration element. */
    public static final String JOURNAL_ELEMENT = "Journal";

    /** Name of the data store configuration element. */
    public static final String DATA_STORE_ELEMENT = "DataStore";

    /** Name of the repository lock mechanism configuration element. */
    public static final String REPOSITORY_LOCK_MECHANISM_ELEMENT =
        "RepositoryLockMechanism";

    /** Name of the persistence manager configuration element. */
    public static final String PERSISTENCE_MANAGER_ELEMENT =
        "PersistenceManager";

    /** Name of the search index configuration element. */
    public static final String SEARCH_INDEX_ELEMENT = "SearchIndex";

    /** Name of the ism locking configuration element. */
    public static final String ISM_LOCKING_ELEMENT = "ISMLocking";

    /** Name of the application name configuration attribute. */
    public static final String APP_NAME_ATTRIBUTE = "appName";

    /** Name of the workspace containing security data. */
    public static final String WSP_NAME_ATTRIBUTE = "workspaceName";

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

    /** Name of the stopDelay configuration attribute. */
    public static final String STOP_DELAY_ATTRIBUTE = "stopDelay";

    /** Name of the default search index implementation class. */
    public static final String DEFAULT_QUERY_HANDLER =
        "org.apache.jackrabbit.core.query.lucene.SearchIndex";

    /** Name of the clustered configuration attribute. */
    public static final String CLUSTERED_ATTRIBUTE = "clustered";

    /** Name of the primary DataStore class attribute. */
    public static final String PRIMARY_DATASTORE_ATTRIBUTE = "primary";

    /** Name of the archive DataStore class attribute. */
    public static final String ARCHIVE_DATASTORE_ATTRIBUTE = "archive";

    /** Default synchronization delay, in milliseconds. */
    public static final String DEFAULT_SYNC_DELAY = "5000";

    /**
     * Default stop delay, in milliseconds or -1 if the default is derived
     * from the sync delay.
     */
    public static final String DEFAULT_STOP_DELAY = "-1";

    /** Name of the workspace specific security configuration element */
    private static final String WSP_SECURITY_ELEMENT = "WorkspaceSecurity";

    /**
     * Name of the optional AccessControlProvider element defining which
     * implementation of AccessControlProvider should be used.
     */
    private static final String AC_PROVIDER_ELEMENT = "AccessControlProvider";

    /**
     * Optional configuration elements with the user manager configuration.
     * @see org.apache.jackrabbit.core.security.user.action.AuthorizableAction
     */
    private static final String AUTHORIZABLE_ACTION = "AuthorizableAction";

    /**
     * The repositories {@link ConnectionFactory}. 
     */
    protected final ConnectionFactory connectionFactory;

    protected BeanFactory beanFactory = new SimpleBeanFactory();

    protected BeanConfigVisitor configVisitor = new NoOpConfigVisitor();

    /**
     * Element specifying the class of principals used to retrieve the userID
     * in the 'class' attribute.
     */
    private static final String USERID_CLASS_ELEMENT = "UserIdClass";

    /**
     * Name of the optional XmlImport config entry inside the workspace configuration.
     */
    private static final String IMPORT_ELEMENT = "Import";
    private static final String IMPORT_PII_ELEMENT = "ProtectedItemImporter";
    private static final String IMPORT_PNI_ELEMENT = "ProtectedNodeImporter";
    private static final String IMPORT_PPI_ELEMENT = "ProtectedPropertyImporter";

    /**
     * Name of the cluster node id file.
     */
    private static final String CLUSTER_NODE_ID_FILE = "cluster_node.id";

    /**
     * Creates a new configuration parser with the given parser variables
     * and connection factory.
     *
     * @param variables parser variables
     * @param connectionFactory connection factory
     */
    protected RepositoryConfigurationParser(
            Properties variables, ConnectionFactory connectionFactory) {
        super(variables);
        this.connectionFactory = connectionFactory;
    }

    /**
     * Creates a new configuration parser with the given parser variables.
     *
     * @param variables parser variables
     */
    public RepositoryConfigurationParser(Properties variables) {
        this(variables, new ConnectionFactory());
    }

    /**
     * Parses repository configuration. Repository configuration uses the
     * following format:
     * <pre>
     *   &lt;Repository&gt;
     *     &lt;FileSystem ...&gt;
     *     &lt;Security appName="..."&gt;
     *       &lt;SecurityManager ...&gt;
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
        Element root = parseXML(xml, true);

        // Repository home directory
        String home = getVariables().getProperty(REPOSITORY_HOME_VARIABLE);

        // File system implementation
        FileSystemFactory fsf = getFileSystemFactory(root, FILE_SYSTEM_ELEMENT);

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

        // Query handler implementation
        QueryHandlerFactory qhf = getQueryHandlerFactory(root);

        // Optional journal configuration
        ClusterConfig cc = parseClusterConfig(root, new File(home));

        // Optional data store factory
        DataStoreFactory dsf = getDataStoreFactory(root, home);

        RepositoryLockMechanismFactory rlf = getRepositoryLockMechanismFactory(root);

        // Optional data source configuration
        DataSourceConfig dsc = parseDataSourceConfig(root);

        return new RepositoryConfig(home, securityConfig, fsf,
                workspaceDirectory, workspaceConfigDirectory, defaultWorkspace,
                maxIdleTime, template, vc, qhf, cc, dsf, rlf, dsc, connectionFactory, this);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BeanConfig parseBeanConfig(Element parent, String name) throws ConfigurationException {
        BeanConfig cfg = super.parseBeanConfig(parent, name);
        cfg.setConnectionFactory(connectionFactory);
        cfg.setInstanceFactory(beanFactory);
        configVisitor.visit(cfg);
        return cfg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BeanConfig parseBeanConfig(Element element) throws ConfigurationException {
        BeanConfig cfg = super.parseBeanConfig(element);
        cfg.setConnectionFactory(connectionFactory);
        cfg.setInstanceFactory(beanFactory);
        configVisitor.visit(cfg);
        return cfg;
    }

    /**
     * Parses security configuration. Security configuration
     * uses the following format:
     * <pre>
     *   &lt;Security appName="..."&gt;
     *     &lt;SecurityManager ...&gt;
     *     &lt;AccessManager ...&gt;
     *     &lt;LoginModule ... (optional)&gt;
     *   &lt;/Security&gt;
     * </pre>
     * <p>
     * The <code>SecurityManager</code>, the <code>AccessManager</code>
     * and <code>LoginModule</code> are all
     * {@link #parseBeanConfig(Element,String) bean configuration}
     * elements.
     * <p>
     * The login module is an optional feature of repository configuration.
     *
     * @param security the &lt;security&gt; element.
     * @return the security configuration.
     * @throws ConfigurationException if the configuration is broken
     */
    public SecurityConfig parseSecurityConfig(Element security)
            throws ConfigurationException {
        String appName = getAttribute(security, APP_NAME_ATTRIBUTE);

        SecurityManagerConfig smc = parseSecurityManagerConfig(security);
        AccessManagerConfig amc = parseAccessManagerConfig(security);
        LoginModuleConfig lmc = parseLoginModuleConfig(security);

        return new SecurityConfig(appName, smc, amc, lmc);
    }

    /**
     * Parses the security manager configuration.
     *
     * @param security the &lt;security&gt; element.
     * @return the security manager configuration or <code>null</code>.
     * @throws ConfigurationException if the configuration is broken
     */
    public SecurityManagerConfig parseSecurityManagerConfig(Element security)
            throws ConfigurationException {
        // Optional security manager config entry
        Element smElement = getElement(security, SECURITY_MANAGER_ELEMENT, false);
        if (smElement != null) {
            BeanConfig bc = parseBeanConfig(smElement);
            String wspAttr = getAttribute(smElement, WSP_NAME_ATTRIBUTE, null);

            BeanConfig wac = null;
            Element element = getElement(smElement, WORKSPACE_ACCESS_ELEMENT, false);
            if (element != null) {
                wac = parseBeanConfig(smElement, WORKSPACE_ACCESS_ELEMENT);
            }

            UserManagerConfig umc = null;
            element = getElement(smElement, USER_MANAGER_ELEMENT, false);
            if (element != null) {
                Element[] acElements = getElements(element, AUTHORIZABLE_ACTION, false);
                BeanConfig[] aaConfig = new BeanConfig[acElements.length];
                for (int i = 0; i < acElements.length; i++) {
                    aaConfig[i] = parseBeanConfig(acElements[i]);
                }
                umc = new UserManagerConfig(parseBeanConfig(element), aaConfig);
            }

            BeanConfig uidcc = null;
            element = getElement(smElement, USERID_CLASS_ELEMENT, false);
            if (element != null) {
                uidcc = parseBeanConfig(element);
            }

            return new SecurityManagerConfig(bc, wspAttr, wac, umc, uidcc);
        } else {
            return null;
        }
    }

    /**
     * Parses the access manager configuration.
     *
     * @param security the &lt;security&gt; element.
     * @return the access manager configuration or <code>null</code>.
     * @throws ConfigurationException if the configuration is broken
     */
    public AccessManagerConfig parseAccessManagerConfig(Element security)
            throws ConfigurationException {
        // Optional access manager config entry
        Element accessMgr = getElement(security, ACCESS_MANAGER_ELEMENT, false);
        if (accessMgr != null) {
            return new AccessManagerConfig(parseBeanConfig(accessMgr));
        } else {
            return null;
        }
    }

    /**
     * Parses the login module configuration.
     *
     * @param security the &lt;security&gt; element.
     * @return the login module configuration or <code>null</code>.
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
     *     &lt;WorkspaceSecurity ...&gt;
     *     &lt;ISMLocking ...&gt;
     *   &lt;/Workspace&gt;
     * </pre>
     * <p>
     * All the child elements (<code>FileSystem</code>,
     * <code>PersistenceManager</code>, and <code>SearchIndex</code>) are
     * {@link #parseBeanConfig(Element,String) bean configuration} elements.
     * In addition to bean configuration, the
     * search element also contains
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
     * @see #parseWorkspaceSecurityConfig(Element)
     */
    public WorkspaceConfig parseWorkspaceConfig(InputSource xml)
            throws ConfigurationException {

        Element root = parseXML(xml);
        return parseWorkspaceConfig(root);
    }

    /**
     * Parse workspace config.
     *
     * @param root root element of the workspace configuration
     * @return The workspace configuration
     * @throws ConfigurationException
     * @see #parseWorkspaceConfig(InputSource)
     */
    protected WorkspaceConfig parseWorkspaceConfig(Element root)
            throws ConfigurationException {

        // Workspace home directory
        String home = getVariables().getProperty(WORKSPACE_HOME_VARIABLE);

        // Workspace name
        String name = getAttribute(root, "name", new File(home).getName());

        // Clustered attribute
        boolean clustered = Boolean.valueOf(
                getAttribute(root, CLUSTERED_ATTRIBUTE, "true"));

        // Create a temporary parser that contains the ${wsp.name} variable
        Properties tmpVariables = (Properties) getVariables().clone();
        tmpVariables.put(WORKSPACE_NAME_VARIABLE, name);
        RepositoryConfigurationParser tmpParser = createSubParser(tmpVariables);

        // File system implementation
        FileSystemFactory fsf =
            tmpParser.getFileSystemFactory(root, FILE_SYSTEM_ELEMENT);

        // Persistence manager implementation
        PersistenceManagerConfig pmc = tmpParser.parsePersistenceManagerConfig(root);

        // Query handler implementation
        QueryHandlerFactory qhf = tmpParser.getQueryHandlerFactory(root);

        // Item state manager locking configuration (optional)
        ISMLockingFactory ismLockingFactory =
            tmpParser.getISMLockingFactory(root);

        // workspace specific security configuration
        WorkspaceSecurityConfig workspaceSecurityConfig = tmpParser.parseWorkspaceSecurityConfig(root);

        // optional config for import handling
        ImportConfig importConfig = tmpParser.parseImportConfig(root);

        // default lock timeout
        String to = getAttribute(root, "defaultLockTimeout", new Long(Long.MAX_VALUE).toString());
        long defaultLockTimeout;
        try {
            defaultLockTimeout = Long.parseLong(to);
        }
        catch (NumberFormatException ex) {
            throw new ConfigurationException("defaultLockTimeout must be an integer value", ex);
        }

        return new WorkspaceConfig(
                home, name, clustered, fsf, pmc, qhf,
                ismLockingFactory, workspaceSecurityConfig, importConfig, defaultLockTimeout);
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
     * <p>
     * Both the <code>SearchIndex</code> and <code>FileSystem</code>
     * elements are {@link #parseBeanConfig(Element,String) bean configuration}
     * elements. If the search implementation class is not given, then
     * a default implementation is used.
     * <p>
     * The search index is an optional feature of workspace configuration.
     * If the search configuration element is not found, then this method
     * returns <code>null</code>.
     * <p>
     * The FileSystem element in a search index configuration is optional.
     * However some implementations may require a FileSystem.
     *
     * @param parent parent of the <code>SearchIndex</code> element
     * @return query handler factory
     */
    protected QueryHandlerFactory getQueryHandlerFactory(final Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && SEARCH_INDEX_ELEMENT.equals(child.getNodeName())) {
                return new QueryHandlerFactory() {
                    public QueryHandler getQueryHandler(QueryHandlerContext context)
                            throws RepositoryException {
                        Element element = (Element) child;

                        // Optional file system implementation
                        FileSystem fs = null;
                        if (getElement(element, FILE_SYSTEM_ELEMENT, false) != null) {
                            fs = getFileSystemFactory(
                                    element, FILE_SYSTEM_ELEMENT).getFileSystem();
                        }

                        // Search implementation class
                        String className = getAttribute(
                                element, CLASS_ATTRIBUTE, DEFAULT_QUERY_HANDLER);
                        BeanConfig config = new BeanConfig(
                                className, parseParameters(element));

                        QueryHandler handler =
                            config.newInstance(QueryHandler.class);
                        try {
                            handler.init(fs, context);
                            return handler;
                        } catch (IOException e) {
                            throw new RepositoryException(
                                    "Unable to initialize query handler: " + handler, e);
                        }
                    }
                };
            }
        }
        return null;
    }


    /**
     * Read the optional WorkspaceSecurity Element of Workspace's configuration.
     * It uses the following format:
     * <pre>
     *   &lt;WorkspaceSecurity&gt;
     *     &lt;AccessControlProvider class="..." (optional)&gt;
     *   &lt;/WorkspaceSecurity&gt;
     * </pre>
     *
     * @param parent Workspace-Root-Element
     * @return a new <code>WorkspaceSecurityConfig</code> or <code>null</code>
     *         if none is configured.
     * @throws ConfigurationException
     */
    public WorkspaceSecurityConfig parseWorkspaceSecurityConfig(Element parent)
        throws ConfigurationException {

        BeanConfig acProviderConfig = null;
        Element element = getElement(parent, WSP_SECURITY_ELEMENT, false);
        if (element != null) {
            Element provFact = getElement(element, AC_PROVIDER_ELEMENT, false);
            if (provFact != null) {
                acProviderConfig = parseBeanConfig(element, AC_PROVIDER_ELEMENT);
                acProviderConfig.setValidate(false); // JCR-1920
            }
            return new WorkspaceSecurityConfig(acProviderConfig);
        }
        return null;
    }

    /**
     * Read the optional Import Element of Workspace's configuration. It uses
     * the following format:
     * <pre>
     *   &lt;Import&gt;
     *     &lt;ProtectedNodeImporter class="..." (optional)&gt;
     *     &lt;ProtectedNodeImporter class="..." (optional)&gt;
     *     ...
     *     &lt;ProtectedPropertyImporter class="..." (optional)&gt;
     *   &lt;/Import&gt;
     * </pre>
     *
     * @param parent Workspace-Root-Element
     * @return a new <code>ImportConfig</code> or <code>null</code> if none is
     *         configured.
     * @throws ConfigurationException
     */
    public ImportConfig parseImportConfig(Element parent) throws ConfigurationException {
        List<BeanConfig> protectedItemImporters = new ArrayList<BeanConfig>();
        Element element = getElement(parent, IMPORT_ELEMENT, false);
        if (element != null) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (IMPORT_PNI_ELEMENT.equals(child.getNodeName()) ||
                            IMPORT_PPI_ELEMENT.equals(child.getNodeName()) ||
                            IMPORT_PII_ELEMENT.equals(child.getNodeName())) {
                        BeanConfig bc = parseBeanConfig((Element) child);
                        bc.setValidate(false);
                        protectedItemImporters.add(bc);
                    } // else: some other entry -> ignore.
                }
            }
            return new ImportConfig(protectedItemImporters);
        }
        return null;
    }

    /**
     * Returns an ISM locking factory that creates {@link ISMLocking} instances
     * based on the given configuration. ISM locking configuration uses the
     * following format:
     * <pre>
     *   &lt;ISMLocking class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/ISMLocking&gt;
     * </pre>
     * <p>
     * The <code>ISMLocking</code> is a
     * {@link #parseBeanConfig(Element,String) bean configuration} element.
     * <p>
     * ISM locking is an optional part of the  workspace configuration. If
     * the ISM locking element is not found, then the returned factory will
     * create instances of the {@link DefaultISMLocking} class.
     *
     * @param parent parent of the <code>ISMLocking</code> element
     * @return ISM locking factory
     */
    protected ISMLockingFactory getISMLockingFactory(final Element parent) {
        return new ISMLockingFactory() {
            public ISMLocking getISMLocking() throws RepositoryException {
                Element element = getElement(parent, ISM_LOCKING_ELEMENT, false);
                if (element != null) {
                    return parseBeanConfig(element).newInstance(ISMLocking.class);
                } else {
                    return new DefaultISMLocking();
                }
            }
        };
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
        FileSystemFactory fsf =
            getFileSystemFactory(element, FILE_SYSTEM_ELEMENT);

        // Persistence manager implementation
        PersistenceManagerConfig pmc = parsePersistenceManagerConfig(element);

        // Item state manager locking configuration (optional)
        ISMLockingFactory ismLockingFactory =
            getISMLockingFactory(element);

        return new VersioningConfig(home, fsf, pmc, ismLockingFactory);
    }

    /**
     * Parses cluster configuration. Cluster configuration uses the following format:
     * <pre>
     *   &lt;Cluster&gt;
     *     &lt;Journal ...&gt;
     *   &lt;/Journal&gt;
     * </pre>
     * <p>
     * <code>Cluster</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     * <p>
     * Clustering is an optional feature. If the cluster element is not found, then this
     * method returns <code>null</code>.
     *
     * @param parent parent of the <code>Journal</code> element
     * @param home repository home directory
     * @return cluster configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected ClusterConfig parseClusterConfig(Element parent, File home)
            throws ConfigurationException {

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && CLUSTER_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;

                // Find the cluster node id
                String id =
                    System.getProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID);
                String value = getAttribute(element, ID_ATTRIBUTE, null);
                if (value != null) {
                    id = replaceVariables(value);
                } else if (id == null) {
                    File file = new File(home, CLUSTER_NODE_ID_FILE);
                    try {
                        if (file.exists() && file.canRead()) {
                            id = FileUtils.readFileToString(file).trim();
                        } else {
                            id = UUID.randomUUID().toString();
                            FileUtils.writeStringToFile(file, id);
                        }
                    } catch (IOException e) {
                        throw new ConfigurationException(
                                "Failed to access cluster node id: " + file, e);
                    }
                }

                long syncDelay = Long.parseLong(replaceVariables(getAttribute(
                        element, SYNC_DELAY_ATTRIBUTE, DEFAULT_SYNC_DELAY)));
                long stopDelay = Long.parseLong(replaceVariables(getAttribute(
                        element, STOP_DELAY_ATTRIBUTE, "-1")));

                JournalFactory jf = getJournalFactory(element, home, id);
                return new ClusterConfig(id, syncDelay, stopDelay, jf);
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
     * <p>
     * <code>Journal</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     *
     * @param cluster parent cluster element
     * @param home repository home directory
     * @param id cluster node id
     * @return journal factory
     * @throws ConfigurationException if the configuration is broken
     */
    protected JournalFactory getJournalFactory(
            final Element cluster, final File home, final String id)
            throws ConfigurationException {
        return new JournalFactory() {
            public Journal getJournal(NamespaceResolver resolver)
                    throws RepositoryException {
                BeanConfig config = parseBeanConfig(cluster, JOURNAL_ELEMENT);
                Journal journal = config.newInstance(Journal.class);
                if (journal instanceof AbstractJournal) {
                    ((AbstractJournal) journal).setRepositoryHome(home);
                }
                try {
                    journal.init(id, resolver);
                } catch (JournalException e) {
                    // TODO: Should JournalException extend RepositoryException?
                    throw new RepositoryException(
                            "Journal initialization failed: " + journal, e);
                }
                return journal;
            }
        };
    }

    /**
     * Parses the DataSources configuration under the given parent. It has the following format:
     * <pre>
     *   &lt;DataSources&gt;
     *     &lt;DataSource name="..."&gt;
     *       &lt;param name="..." value="..."&gt;
     *       ...
     *     &lt;/DataSource&gt;
     *     &lt;DataSource name="..."&gt;
     *       &lt;param name="..." value="..."&gt;
     *       ...
     *     &lt;/DataSource&gt;
     *   &lt;/DataSources&gt;
     * </pre>
     * <p>
     * @param parent the parent of the DataSources element
     * @return a {@link DataSourceConfig} for the repository
     * @throws ConfigurationException on error
     */
    protected DataSourceConfig parseDataSourceConfig(Element parent)
            throws ConfigurationException {
        DataSourceConfig dsc = new DataSourceConfig();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && DATASOURCES_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;
                NodeList children2 = element.getChildNodes();
                // Process the DataSource entries:
                for (int j = 0; j < children2.getLength(); j++) {
                    Node child2 = children2.item(j);
                    if (child2.getNodeType() == Node.ELEMENT_NODE
                            && DATASOURCE_ELEMENT.equals(child2.getNodeName())) {
                        Element dsdef = (Element) child2;
                        String logicalName = getAttribute(dsdef, "name");
                        Properties props = parseParameters(dsdef);
                        dsc.addDataSourceDefinition(logicalName, props);
                    }
                }
            }
        }
        return dsc;
    }

    /**
     * Parses data store configuration. Data store configuration uses the following format:
     * <pre>
     *   &lt;DataStore class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/DataStore&gt;
     * </pre>
     * Its also possible to configure a multi data store. The configuration uses following format:
     * <pre>
     *   &lt;DataStore class="org.apache.jackrabbit.core.data.MultiDataStore"&gt;
     *     &lt;param name="primary" value="org.apache.jackrabbit.core.data.db.XXDataStore"&gt;
     *         &lt;param name="..." value="..."&gt;
     *         ...
     *     &lt;/param&gt;
     *     &lt;param name="archive" value="org.apache.jackrabbit.core.data.db.XXDataStore"&gt;
     *         &lt;param name="..." value="..."&gt;
     *         ...
     *     &lt;/param&gt;
     *   &lt;/DataStore&gt;
     * </pre>
     * <p>
     * <code>DataStore</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     *
     * @param parent configuration element
     * @param directory the repository directory
     * @return data store factory
     * @throws ConfigurationException if the configuration is broken
     */
    protected DataStoreFactory getDataStoreFactory(
            final Element parent, final String directory)
            throws ConfigurationException {
        return new DataStoreFactory() {
            public DataStore getDataStore() throws RepositoryException {
                NodeList children = parent.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE
                            && DATA_STORE_ELEMENT.equals(child.getNodeName())) {
                        BeanConfig bc = parseBeanConfig(parent, DATA_STORE_ELEMENT);
                        bc.setValidate(false);
                        DataStore store = bc.newInstance(DataStore.class);
                        if (store instanceof MultiDataStore) {
                            DataStore primary = null;
                            DataStore archive = null;
                            NodeList subParamNodes = child.getChildNodes();
                            for (int x = 0; x < subParamNodes.getLength(); x++) {
                                Node paramNode = subParamNodes.item(x);
                                if (paramNode.getNodeType() == Node.ELEMENT_NODE 
                                        && (PRIMARY_DATASTORE_ATTRIBUTE.equals(paramNode.getAttributes().getNamedItem("name").getNodeValue())
                                                || ARCHIVE_DATASTORE_ATTRIBUTE.equals(paramNode.getAttributes().getNamedItem("name").getNodeValue()))) {
                                    try {
                                        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                                        Element newParent = document.createElement("parent");
                                        document.appendChild(newParent);
                                        Element datastoreElement = document.createElement(DATA_STORE_ELEMENT);
                                        newParent.appendChild(datastoreElement);
                                        NodeList childNodes = paramNode.getChildNodes();
                                        for (int y = 0; childNodes.getLength() > y; y++) {
                                            datastoreElement.appendChild(document.importNode(childNodes.item(y), true));
                                        }
                                        NamedNodeMap attributes = paramNode.getAttributes();
                                        for (int z = 0; attributes.getLength() > z; z++) {
                                            Node item = attributes.item(z);
                                            datastoreElement.setAttribute(CLASS_ATTRIBUTE, item.getNodeValue());
                                        }
                                        DataStore subDataStore = getDataStoreFactory(newParent, directory).getDataStore();
                                        if (!MultiDataStoreAware.class.isAssignableFrom(subDataStore.getClass())) {
                                            throw new ConfigurationException("Only MultiDataStoreAware datastore's can be used within a MultiDataStore.");
                                        }
                                        String type = getAttribute((Element) paramNode, NAME_ATTRIBUTE);
                                        if (PRIMARY_DATASTORE_ATTRIBUTE.equals(type)) {
                                            primary = subDataStore;
                                        } else if (ARCHIVE_DATASTORE_ATTRIBUTE.equals(type)) {
                                            archive = subDataStore;
                                        }
                                    } catch (Exception e) {
                                        throw new ConfigurationException("Failed to parse the MultiDataStore element.", e);
                                    }
                                }
                            }
                            if (primary == null || archive == null) {
                                throw new ConfigurationException("A MultiDataStore must have configured a primary and archive datastore");
                            }
                            ((MultiDataStore) store).setPrimaryDataStore(primary);
                            ((MultiDataStore) store).setArchiveDataStore(archive);
                        }
                        store.init(directory);
                        return store;
                    }
                }
                return null;
            }
        };
    }

    /**
     * Parses repository lock mechanism configuration. Repository lock mechanism
     * configuration uses the following format:
     * <pre>
     *   &lt;RepositoryLockMechanism class=&quot;...&quot; &gt;
     *     &lt;param name=&quot;...&quot; value=&quot;...&quot;&gt;
     *     ...
     *   &lt;/RepositoryLockMechanism&gt;
     * </pre>
     * <p>
     * <code>RepositoryLockMechanism</code> is a
     * {@link #parseBeanConfig(Element,String) bean configuration} element.
     *
     * @param root the root configuration element
     * @return repository lock mechanism factory
     */
    protected RepositoryLockMechanismFactory getRepositoryLockMechanismFactory(final Element root) {
        return new RepositoryLockMechanismFactory() {
            public RepositoryLockMechanism getRepositoryLockMechanism() throws RepositoryException {
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE
                            && REPOSITORY_LOCK_MECHANISM_ELEMENT.equals(child.getNodeName())) {
                        BeanConfig bc =
                            parseBeanConfig(root, REPOSITORY_LOCK_MECHANISM_ELEMENT);
                        return bc.newInstance(RepositoryLockMechanism.class);
                    }
                }
                return new RepositoryLock();
            }
        };
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
     * variables and the same connection factory as this parser.
     *
     * @param variables the variables overlay
     * @return a new configuration parser instance
     */
    protected RepositoryConfigurationParser createSubParser(Properties variables) {
        // overlay the properties
        Properties props = new Properties(getVariables());
        props.putAll(variables);
        return new RepositoryConfigurationParser(props, connectionFactory);
    }

    /**
     * Creates and returns a factory object that creates {@link FileSystem}
     * instances based on the bean configuration at the named element.
     *
     * @param parent parent element
     * @param name name of the bean configuration element
     * @return file system factory
     * @throws ConfigurationException if the bean configuration is invalid
     */
    protected FileSystemFactory getFileSystemFactory(Element parent, String name)
            throws ConfigurationException {
        final BeanConfig config = parseBeanConfig(parent, name);
        return new FileSystemFactory() {
            public FileSystem getFileSystem() throws RepositoryException {
                try {
                    FileSystem fs = config.newInstance(FileSystem.class);
                    fs.init();
                    return fs;
                } catch (FileSystemException e) {
                    throw new RepositoryException(
                            "File system initialization failure.", e);
                }
            }
        };
    }


    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setConfigVisitor(BeanConfigVisitor configVisitor) {
        this.configVisitor = configVisitor;
    }
}
