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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.util.Text;
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
    public static final String REPOSITORY_HOME_VARIABLE = "${rep.home}";

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
    public static final String WORKSPACE_HOME_VARIABLE = "${wsp.home}";
    public static final String WORKSPACE_NAME_VARIABLE = "${wsp.name}";

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
            FileSystem repFS = (FileSystem) fsc.newInstance();
            repFS.init();

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
                    WorkspaceConfig wspConfig = parseWorkspaceConfig(
                                configFile.getPath(), configFile.getParent());
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

            return new RepositoryConfig(config, this, home, appName, wspConfigs, repFS, wspConfigRootDir, defaultWspName, amc, vc);
        } catch (FileSystemException ex) {
            throw new RepositoryException(ex);
        } catch (JDOMException ex) {
            throw new RepositoryException(ex);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
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

        // create workspace config object
        return parseWorkspaceConfig(configFile.getPath(), configFile.getParent());
    }

    /**
     * Initializes this <code>RepositoryConfig</code> object.
     *
     * @param config
     * @throws RepositoryException
     */
    protected void init(Document config) throws RepositoryException {
    }

    /**
     * Creates a new <code>WorkspaceConfig</code> instance. The configuration
     * is read from the specified configuration file.
     *
     * @param configFilePath path to the configuration file
     * @param wspHomeDir     workspace home directory
     * @return a new <code>WorkspaceConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public WorkspaceConfig parseWorkspaceConfig(
            String configFilePath, String wspHomeDir)
            throws RepositoryException {
        try {
            File config = new File(configFilePath);
            InputSource is = new InputSource(new FileReader(config));
            is.setSystemId(config.toURI().toString());
            return parseWorkspaceConfig(is, wspHomeDir);
        } catch (IOException ioe) {
            String msg = "error while reading config file " + configFilePath;
            throw new RepositoryException(msg, ioe);
        }
    }

    /**
     * Creates a new <code>WorkspaceConfig</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param is         <code>InputSource</code> where the configuration is read from
     * @param wspHomeDir workspace home directory
     * @return a new <code>WorkspaceConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public WorkspaceConfig parseWorkspaceConfig(
            InputSource xml, String home)
            throws RepositoryException {
        try {
            Document config = parse(xml);

            Properties newVariables = new Properties(variables);
            newVariables.setProperty(WORKSPACE_HOME_VARIABLE, home);
            ConfigurationParser parser = new ConfigurationParser(newVariables);
            return parser.parseWorkspaceConfig(config);
        } catch (JDOMException ex) {
            throw new RepositoryException(ex);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Initializes this <code>WorkspaceConfig</code> object.
     *
     * @param config
     * @throws RepositoryException
     */
    public WorkspaceConfig parseWorkspaceConfig(Document config)
            throws RepositoryException {
        try {
            String wspHomeDir = variables.getProperty(WORKSPACE_HOME_VARIABLE);

            Element wspElem = config.getRootElement();
            // name
            String wspName = wspElem.getAttributeValue(NAME_ATTRIBUTE);
            if (wspName == null) {
                // init with wsp home dirname
                wspName = new File(wspHomeDir).getName();
            } else {
                wspName = replaceVariables(wspName);
            }

            // set name variable
            Properties newVariables = new Properties(variables);
            newVariables.put(WORKSPACE_NAME_VARIABLE, wspName);
            ConfigurationParser parser = new ConfigurationParser(newVariables);

            // file system
            BeanConfig fsc = parser.parseBeanConfig(wspElem, FILE_SYSTEM_ELEMENT);
            FileSystem wspFS = (FileSystem) fsc.newInstance();
            wspFS.init();

            // persistence manager config
            BeanConfig pmc =
                parser.parseBeanConfig(wspElem, PERSISTENCE_MANAGER_ELEMENT);

            // search config (optional)
            Element searchElem = wspElem.getChild(SEARCH_INDEX_ELEMENT);
            SearchConfig sc = null;
            if (searchElem != null) {
                sc = parser.parseSearchConfig(searchElem);
            }

            return new WorkspaceConfig(wspHomeDir, wspName, wspFS, pmc, sc);
        } catch (FileSystemException ex) {
            throw new RepositoryException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Creates a new <code>SearchConfig</code>.
     * @param config the config root element for this <code>SearchConfig</code>.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public SearchConfig parseSearchConfig(Element config) throws RepositoryException {
        try {
            // create FileSystem
            BeanConfig fsc = parseBeanConfig(config, FILE_SYSTEM_ELEMENT);
            FileSystem fs = (FileSystem) fsc.newInstance();
            fs.init();

            // handler class name
            String handlerClassName = config.getAttributeValue(CLASS_ATTRIBUTE,
                    DEFAULT_QUERY_HANDLER);

            // gather params
            Properties params = parseParameters(config);

            return new SearchConfig(fs, handlerClassName, params);
        } catch (FileSystemException ex) {
            throw new RepositoryException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Creates a new <code>VersioningConfig</code>.
     * @param config the config root element for this <code>VersioningConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public VersioningConfig parseVersioningConfig(Element config) throws RepositoryException {
        try {
            // home dir
            File homeDir = new File(replaceVariables(config.getAttributeValue(ROOTPATH_ATTRIBUTE)));

            // create FileSystem
            BeanConfig fsc = parseBeanConfig(config, FILE_SYSTEM_ELEMENT);
            FileSystem fs = (FileSystem) fsc.newInstance();
            fs.init();

            // persistence manager config
            BeanConfig pmc = parseBeanConfig(config, PERSISTENCE_MANAGER_ELEMENT);

            return new VersioningConfig(homeDir, fs, pmc);
        } catch (FileSystemException ex) {
            throw new RepositoryException(ex);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    public BeanConfig parseBeanConfig(Element parent, String name)
            throws RepositoryException {
        Element element = parent.getChild(name);
        if (element != null) {
            String className = element.getAttributeValue(CLASS_ATTRIBUTE);
            Properties properties = parseParameters(element);
            return new BeanConfig(className, properties);
        } else {
            throw new IllegalArgumentException(name);
            // return null;
        }
    }

    public Properties parseParameters(Element element) {
        Properties parameters = new Properties();

        Iterator iterator = element.getChildren(PARAM_ELEMENT).iterator();
        while (iterator.hasNext()) {
            Element parameter = (Element) iterator.next();
            String name = parameter.getAttributeValue(NAME_ATTRIBUTE);
            String value = parameter.getAttributeValue(VALUE_ATTRIBUTE);
            // replace variables in param value
            parameters.put(name, replaceVariables(value));
        }

        return parameters;
    }

    /**
     * Helper method that replaces in the given string any occurences of the keys
     * in the specified map with their associated values.
     *
     * @param s
     * @param vars
     * @return
     */
    public String replaceVariables(String value) {
        String ovalue = value;
        Enumeration e = variables.propertyNames();
        while (e.hasMoreElements()) {
            String varName = (String) e.nextElement();
            String varValue = variables.getProperty(varName);
            value = Text.replace(value, varName, varValue);
        }
        return value;
    }

}
