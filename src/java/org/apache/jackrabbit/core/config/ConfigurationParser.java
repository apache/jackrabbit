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
package org.apache.jackrabbit.core.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * TODO
 */
public class ConfigurationParser {

    private static Logger log = Logger.getLogger(ConfigurationParser.class);

    private static final String SECURITY_ELEMENT = "Security";
    private static final String APP_NAME_ATTRIB = "appName";
    private static final String ACCESS_MANAGER_ELEMENT = "AccessManager";

    private static final String WORKSPACES_ELEMENT = "Workspaces";
    private static final String ROOT_PATH_ATTRIB = "rootPath";
    private static final String DEFAULT_WORKSPACE_ATTRIB = "defaultWorkspace";

    private static final String WORKSPACE_ELEMENT = "Workspace";

    private static final String VERSIONING_ELEMENT = "Versioning";

    /**
     * wellknown variables (will be replaced with their respective values
     * whereever they occur within the configuration)
     */
    public static final String REPOSITORY_HOME_VARIABLE = "rep.home";

    protected static final String FILE_SYSTEM_ELEMENT = "FileSystem";
    private static final String PERSISTENCE_MANAGER_ELEMENT = "PersistenceManager";
    private static final String SEARCH_INDEX_ELEMENT = "SearchIndex";

    private static final String CLASS_ATTRIBUTE = "class";

    private static final String PARAM_ELEMENT = "param";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";

    /** attribute name of home dir */
    private static final String ROOTPATH_ATTRIBUTE = "rootPath";

    /**
     * wellknown variables (will be replaced with their respective values
     * whereever they occur within the configuration)
     */
    public static final String WORKSPACE_HOME_VARIABLE = "wsp.home";
    public static final String WORKSPACE_NAME_VARIABLE = "wsp.name";

    /** FQN of the default query handler implementation */
    private static final String DEFAULT_QUERY_HANDLER
            = "org.apache.jackrabbit.core.search.lucene.SearchIndex";

    private Properties variables;

    public ConfigurationParser(Properties variables) {
        this.variables = variables;
    }

    public Document parse(InputSource xml) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(new ConfigurationEntityResolver());
        return builder.build(xml);
    }
    
    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified configuration file.
     *
     * @param configFilePath path to the configuration file
     * @param repHomeDir     repository home directory
     * @return a new <code>RepositoryConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public RepositoryConfig parseRepositoryConfig(
            String configFilePath, String repHomeDir) throws RepositoryException {
        try {
            File config = new File(configFilePath);
            InputSource is = new InputSource(new FileReader(config));
            is.setSystemId(config.toURI().toString());
            return parseRepositoryConfig(is, repHomeDir);
        } catch (IOException ioe) {
            String msg = "error while reading config file " + configFilePath;
            throw new RepositoryException(msg, ioe);
        }
    }

    /**
     * private constructor.
     *
     * @param is
     * @param repHomeDir
     * @throws RepositoryException
     */
    public RepositoryConfig parseRepositoryConfig(
            InputSource xml, String repHomeDir)
            throws RepositoryException {
        Properties newVariables = new Properties(variables);
        newVariables.setProperty(REPOSITORY_HOME_VARIABLE, repHomeDir);
        ConfigurationParser parser = new ConfigurationParser(newVariables);
        return parser.parseRepositoryConfig(xml);
    }

    public RepositoryConfig parseRepositoryConfig(InputSource xml)
            throws RepositoryException {
        try {
            Document config = parse(xml);
            Element root = config.getRootElement();

            String home = variables.getProperty(REPOSITORY_HOME_VARIABLE);

            // file system
            BeanConfig fsc = parseBeanConfig(root, FILE_SYSTEM_ELEMENT);

            // security & access manager config
            Element secEleme = root.getChild(SECURITY_ELEMENT);
            String appName = secEleme.getAttributeValue(APP_NAME_ATTRIB);
            BeanConfig amc = parseBeanConfig(secEleme, ACCESS_MANAGER_ELEMENT);

            // workspaces
            Element wspsElem = root.getChild(WORKSPACES_ELEMENT);
            String wspConfigRootDir = replaceVariables(wspsElem.getAttributeValue(ROOT_PATH_ATTRIB));
            String defaultWspName = replaceVariables(wspsElem.getAttributeValue(DEFAULT_WORKSPACE_ATTRIB));

            // load wsp configs
            Map wspConfigs = new HashMap();
            File wspRoot = new File(wspConfigRootDir);
            if (!wspRoot.exists()) {
                wspRoot.mkdir();
            }
            File[] files = wspRoot.listFiles();
            if (files == null) {
                String msg = "invalid repsitory home directory";
                throw new RepositoryException(msg);
            }
            for (int i = 0; i < files.length; i++) {
                // check if <subfolder>/workspace.xml exists
                File configFile = new File(files[i], "workspace.xml");
                if (configFile.isFile()) {
                    // create workspace config
                    Properties newVariables = new Properties(variables);
                    newVariables.setProperty(
                            WORKSPACE_HOME_VARIABLE, configFile.getParent());
                    ConfigurationParser parser =
                        new ConfigurationParser(newVariables);

                    InputSource wsxml =
                        new InputSource(new FileReader(configFile));
                    wsxml.setSystemId(configFile.toURI().toString());
                    WorkspaceConfig wspConfig =
                        parser.parseWorkspaceConfig(wsxml);
                    String wspName = wspConfig.getName();
                    if (wspConfigs.containsKey(wspName)) {
                        String msg = "duplicate workspace name: " + wspName;
                        throw new RepositoryException(msg);
                    }
                    wspConfigs.put(wspName, wspConfig);
                }
            }
            if (wspConfigs.isEmpty()) {
                // create initial default workspace
                wspConfigs.put(defaultWspName, createWorkspaceConfig(
                        config, wspConfigRootDir, defaultWspName));
            } else {
                if (!wspConfigs.containsKey(defaultWspName)) {
                    String msg = "no configuration found for default workspace: " + defaultWspName;
                    throw new RepositoryException(msg);
                }
            }

            // load versioning config
            Element vElement = config.getRootElement().getChild(VERSIONING_ELEMENT);
            VersioningConfig vc = parseVersioningConfig(vElement);

            return new RepositoryConfig(
                    config, this, home, appName, wspConfigs,
                    createFileSystem(fsc), wspConfigRootDir,
                    defaultWspName, amc, vc);
        } catch (JDOMException ex) {
            throw new RepositoryException(ex);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Creates a new workspace configuration with the specified name.
     *
     * @param name workspace name
     * @return a new <code>WorkspaceConfig</code> object.
     * @throws RepositoryException if the specified name already exists or
     *                             if an error occured during the creation.
     */
    public WorkspaceConfig createWorkspaceConfig(
            Document config, String root, String name)
            throws RepositoryException {
        // create the workspace folder (i.e. the workspace home directory)
        File wspFolder = new File(root, name);
        if (!wspFolder.mkdir()) {
            String msg = "Failed to create the workspace home directory: " + wspFolder.getPath();
            throw new RepositoryException(msg);
        }
        // clone the workspace definition template
        Element wspCongigElem =
            (Element) config.getRootElement().getChild("Workspace").clone();
        wspCongigElem.setAttribute("name", name);

        // create workspace.xml file
/*
        DocType docType = new DocType(WORKSPACE_ELEMENT, null, WorkspaceConfig.PUBLIC_ID);
        Document doc = new Document(wspCongigElem, docType);
*/
        Document doc = new Document(wspCongigElem);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        File configFile = new File(wspFolder, "workspace.xml");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(configFile);
            out.output(doc, fos);
        } catch (IOException ioe) {
            String msg = "Failed to create workspace configuration file: " + configFile.getPath();
            throw new RepositoryException(msg, ioe);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        try {
            // create workspace config object
            Properties newVariables = new Properties(variables);
            newVariables.setProperty(
                    WORKSPACE_HOME_VARIABLE, configFile.getParent());
            ConfigurationParser parser = new ConfigurationParser(newVariables);

            InputSource xml = new InputSource(new FileReader(configFile));
            xml.setSystemId(configFile.toURI().toString());

            return parser.parseWorkspaceConfig(xml);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("TODO", e);
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
     *   &lt;/Search&gt;
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
     * that is given as the ${wsp.home} parser variable.
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
     *
     * @param xml workspace configuration document
     * @return workspace configuration
     * @throws ConfigurationException if the configuration is broken
     * @see #parseBeanConfig(Element, String)
     * @see #parseSearchConfig(Element)
     */
    public WorkspaceConfig parseWorkspaceConfig(InputSource xml)
            throws ConfigurationException {
        try {
            Document document = parse(xml);
            Element element = document.getRootElement();

            // Workspace home directory
            String home = variables.getProperty(WORKSPACE_HOME_VARIABLE);

            // Workspace name
            String name = element.getAttributeValue(NAME_ATTRIBUTE);
            if (name != null) {
                name = replaceVariables(name);
            } else {
                name = new File(home).getName();
            }

            // Create a temporary parser that contains the ${wsp.name} variable
            Properties newVariables = new Properties(variables);
            newVariables.put(WORKSPACE_NAME_VARIABLE, name);
            ConfigurationParser parser = new ConfigurationParser(newVariables);

            // File system implementation
            BeanConfig fsc =
                parser.parseBeanConfig(element, FILE_SYSTEM_ELEMENT);

            // Persistence manager implementation
            BeanConfig pmc =
                parser.parseBeanConfig(element, PERSISTENCE_MANAGER_ELEMENT);

            // Search implementation (optional)
            SearchConfig sc = null;
            Element search = element.getChild(SEARCH_INDEX_ELEMENT);
            if (search != null) {
                sc = parser.parseSearchConfig(search);
            }

            return new WorkspaceConfig(home, name, createFileSystem(fsc), pmc, sc);
        } catch (JDOMException e) {
            throw new ConfigurationException(
                    "Workspace configuration syntax error.", e);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Workspace configuration could not be read.", e);
        }
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
     *
     * @param element search configuration element
     * @return search configuration
     * @throws ConfigurationException if the configuration is broken
     */
    private SearchConfig parseSearchConfig(Element element)
            throws ConfigurationException {
        // Search implementation class
        String className = element.getAttributeValue(CLASS_ATTRIBUTE);
        if (className == null) {
            className = DEFAULT_QUERY_HANDLER;
        }

        // Search parameters
        Properties parameters = parseParameters(element);

        // File system implementation
        BeanConfig fsc = parseBeanConfig(element, FILE_SYSTEM_ELEMENT);

        return new SearchConfig(className, parameters, createFileSystem(fsc));
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
     * @param element versioning configuration element
     * @return versioning configuration
     * @throws ConfigurationException if the configuration is broken
     */
    private VersioningConfig parseVersioningConfig(Element element)
            throws ConfigurationException {
        // Versioning home directory
        String home = element.getAttributeValue(ROOTPATH_ATTRIBUTE);
        if (home == null) {
            throw new ConfigurationException("Versioning root path not set.");
        }

        // File system implementation
        BeanConfig fsc = parseBeanConfig(element, FILE_SYSTEM_ELEMENT);

        // Persistence manager implementation
        BeanConfig pmc = parseBeanConfig(element, PERSISTENCE_MANAGER_ELEMENT);

        return new VersioningConfig(
                replaceVariables(home), createFileSystem(fsc), pmc);
    }

    /**
     * Instantiates and initializes the file system implementation class
     * configured by the given bean configuration object.
     *
     * @param config file system configuration
     * @return initialized file system implementation
     * @throws ConfigurationException if the file system could not be created
     */
    private FileSystem createFileSystem(BeanConfig config)
            throws ConfigurationException {
        try {
            FileSystem filesystem = (FileSystem) config.newInstance();
            filesystem.init();
            return filesystem;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "File system implementation class not found.", e);
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "File system implementation can not be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "File system implementation class is protected.", e);
        } catch (ClassCastException e) {
            throw new ConfigurationException(
                    "Invalid file system implementation class.", e);
        } catch (FileSystemException e) {
            throw new ConfigurationException(
                    "File system initialization failure.", e);
        }
    }

    /**
     * Parses a named bean configuration from the given element.
     * Bean configuration uses the following format:
     * <pre>
     *   &lt;BeanName class="..."&gt;
     *     &lt;param name="..." value="..."/&gt;
     *     ...
     *   &lt;/BeanName&gt;
     * </pre>
     * <p>
     * The returned bean configuration object contains the configured
     * class name and configuration parameters. Variable replacement
     * is performed on the parameter values.
     *
     * @param parent parent element
     * @param name name of the bean configuration element
     * @return bean configuration,
     * @throws ConfigurationException if the configuration element does not
     *                                exist or is broken
     */
    private BeanConfig parseBeanConfig(Element parent, String name)
            throws ConfigurationException {
        // Bean configuration element
        Element element = parent.getChild(name);
        if (element == null) {
            throw new ConfigurationException(
                    "Configuration element not found: " + name);
        }

        // Bean implementation class
        String className = element.getAttributeValue(CLASS_ATTRIBUTE);
        if (className == null) {
            throw new ConfigurationException(
                    "Class attribute not set: " + name);
        }

        // Bean properties
        Properties properties = parseParameters(element);

        return new BeanConfig(className, properties);
    }

    /**
     * Parses the configuration parameters of the given element.
     * Parameters are stored as
     * <code>&lt;param name="..." value="..."/&gt;</code>
     * child elements. This method parses all param elements,
     * performs {@link #replaceVariables(String) variable replacement}
     * on parameter values, and returns the resulting name-value pairs.
     *
     * @param element configuration element
     * @return configuration parameters
     * @throws ConfigurationException if a <code>param</code> element does
     *                                not contain the <code>name</code> and
     *                                <code>value</code> attributes
     */
    private Properties parseParameters(Element element)
            throws ConfigurationException {
        Properties parameters = new Properties();

        Iterator iterator = element.getChildren(PARAM_ELEMENT).iterator();
        while (iterator.hasNext()) {
            Element parameter = (Element) iterator.next();
            String name = parameter.getAttributeValue(NAME_ATTRIBUTE);
            if (name == null) {
                throw new ConfigurationException("Parameter name not set.");
            }
            String value = parameter.getAttributeValue(VALUE_ATTRIBUTE);
            if (value == null) {
                throw new ConfigurationException("Parameter value not set.");
            }
            parameters.put(name, replaceVariables(value));
        }

        return parameters;
    }

    /**
     * Performs variable replacement on the given string value.
     * Each <code>${...}</code> sequence within the given value is replaced
     * with the value of the named parser variable. The replacement is not
     * done if the named variable does not exist.
     *
     * @param value original value
     * @return value after variable replacements
     * @throws ConfigurationException if the replacement of a referenced
     *                                variable is not found
     */
    private String replaceVariables(String value)
            throws ConfigurationException {
        StringBuffer result = new StringBuffer();

        // Value:
        // +--+-+--------+-+-----------------+
        // |  |p|-->     |q|-->              |
        // +--+-+--------+-+-----------------+
        int p = 0, q = value.indexOf("${");              // Find first ${
        while (q != -1) {
            result.append(value, p, q);                  // Text before ${
            p = q;
            q = value.indexOf("}", q + 2);               // Find }
            if (q != -1) {
                String variable = value.substring(p + 2, q);
                String replacement = variables.getProperty(variable);
                if (replacement == null) {
                    throw new ConfigurationException(
                            "Variable replacement not found: " + variable);
                }
                result.append(replacement);
                p = q + 1;
                q = value.indexOf("${", p);              // Find next ${
            }
        }
        result.append(value, p, value.length());         // Trailing text

        return result.toString();
    }

}
