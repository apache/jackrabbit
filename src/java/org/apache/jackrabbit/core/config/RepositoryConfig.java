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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

/**
 * A <code>RepositoryConfig</code> ...
 */
public class RepositoryConfig extends AbstractConfig {
    private static Logger log = Logger.getLogger(RepositoryConfig.class);

    /**
     * name of repository configuration file
     */
    public static final String CONFIG_FILE_NAME = "repository.xml";

    /**
     * public id
     */
    public static final String PUBLIC_ID = "-//The Apache Software Foundation//DTD Repository//EN";

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

    /**
     * map of variable names and their respective values
     */
    private final Properties vars;

    /**
     * map of workspace names and workspace configurations
     */
    private final HashMap wspConfigs;

    /**
     * repository home directory
     */
    private String repHomeDir;

    /**
     * virtual file system where the repository stores global state
     */
    private FileSystem repFS;

    /**
     * the name of the JAAS configuration app-entry for this repository 
     */
    private String appName;

    /**
     * workspaces config root directory (i.e. folder that contains
     * a subfolder with a workspace configuration file for every workspace
     * in the repository)
     */
    private String wspConfigRootDir;

    /**
     * name of default workspace
     */
    private String defaultWspName;

    /**
     * configuration for the access manager
     */
    private BeanConfig amConfig;

    /**
     * the versioning config
     */
    private VersioningConfig vConfig;

    /**
     * private constructor.
     *
     * @param is
     * @param repHomeDir
     * @throws RepositoryException
     */
    private RepositoryConfig(InputSource is, String repHomeDir)
            throws RepositoryException {
        super(is);
        this.repHomeDir = repHomeDir;
        wspConfigs = new HashMap();
        // initialize variables
        vars = new Properties();
        vars.put(REPOSITORY_HOME_VARIABLE, repHomeDir);
        // read config
        init(config);
    }

    /**
     * Initializes this <code>RepositoryConfig</code> object.
     *
     * @param config
     * @throws RepositoryException
     */
    protected void init(Document config) throws RepositoryException {
        try {
            Element root = config.getRootElement();
            ConfigurationParser parser = new ConfigurationParser(vars);

            // file system
            BeanConfig fsc = parser.parseBeanConfig(root, FILE_SYSTEM_ELEMENT);
            repFS = (FileSystem) fsc.newInstance();

            // security & access manager config
            Element secEleme = root.getChild(SECURITY_ELEMENT);
            appName = secEleme.getAttributeValue(APP_NAME_ATTRIB);
            amConfig = parser.parseBeanConfig(secEleme, ACCESS_MANAGER_ELEMENT);

            // workspaces
            Element wspsElem = root.getChild(WORKSPACES_ELEMENT);
            wspConfigRootDir = parser.replaceVariables(wspsElem.getAttributeValue(ROOT_PATH_ATTRIB));
            defaultWspName = parser.replaceVariables(wspsElem.getAttributeValue(DEFAULT_WORKSPACE_ATTRIB));

            // load wsp configs
            File wspRoot = new File(wspConfigRootDir);
            if (!wspRoot.exists()) {
                wspRoot.mkdir();
            }
            File[] files = wspRoot.listFiles();
            if (files == null) {
                String msg = "invalid repsitory home directory";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            for (int i = 0; i < files.length; i++) {
                // check if <subfolder>/workspace.xml exists
                File configFile = new File(files[i], "workspace.xml");
                if (configFile.isFile()) {
                    // create workspace config
                    WorkspaceConfig wspConfig =
                        ConfigurationParser.parseWorkspaceConfig(
                                configFile.getPath(), configFile.getParent());
                    String wspName = wspConfig.getName();
                    if (wspConfigs.containsKey(wspName)) {
                        String msg = "duplicate workspace name: " + wspName;
                        log.debug(msg);
                        throw new RepositoryException(msg);
                    }
                    wspConfigs.put(wspName, wspConfig);
                }
            }
            if (wspConfigs.isEmpty()) {
                // create initial default workspace
                createWorkspaceConfig(defaultWspName);
            } else {
                if (!wspConfigs.containsKey(defaultWspName)) {
                    String msg = "no configuration found for default workspace: " + defaultWspName;
                    log.debug(msg);
                    throw new RepositoryException(msg);
                }
            }

            // load versioning config
            Element vElement = config.getRootElement().getChild(VERSIONING_ELEMENT);
            vConfig = parser.parseVersioningConfig(vElement);
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
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified configuration file.
     *
     * @param configFilePath path to the configuration file
     * @param repHomeDir     repository home directory
     * @return a new <code>RepositoryConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryConfig create(String configFilePath, String repHomeDir) throws RepositoryException {
        try {
            File config = new File(configFilePath);
            InputSource is = new InputSource(new FileReader(config));
            is.setSystemId(config.toURI().toString());
            return new RepositoryConfig(is, repHomeDir);
        } catch (IOException ioe) {
            String msg = "error while reading config file " + configFilePath;
            log.debug(msg);
            throw new RepositoryException(msg, ioe);
        }
    }

    /**
     * Creates a new <code>RepositoryConfig</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param is         <code>InputSource</code> where the configuration is read from
     * @param repHomeDir repository home directory
     * @return a new <code>RepositoryConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryConfig create(InputSource is, String repHomeDir) throws RepositoryException {
        return new RepositoryConfig(is, repHomeDir);
    }

    /**
     * Returns the home directory of the repository.
     *
     * @return the home directory of the repository
     */
    public String getHomeDir() {
        return repHomeDir;
    }

    /**
     * Returns the virtual file system where the repository stores global state.
     *
     * @return the virtual file system where the repository stores global state
     */
    public FileSystem getFileSystem() {
        return repFS;
    }

    /**
     * Returns the name of the JAAS configuration app-entry for this repository.
     *
     * @return the name of the JAAS configuration app-entry for this repository
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns workspaces config root directory (i.e. the folder that contains
     * a subfolder with a workspace configuration file for every workspace
     * in the repository).
     *
     * @return the workspaces config root directory
     */
    public String getWorkspacesConfigRootDir() {
        return wspConfigRootDir;
    }

    /**
     * Returns the name of the default workspace.
     *
     * @return the name of the default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWspName;
    }

    /**
     * Returns all workspace configurations.
     *
     * @return a collection of <code>WorkspaceConfig</code> objects.
     */
    public Collection getWorkspaceConfigs() {
        return wspConfigs.values();
    }

    /**
     * Returns the configuration of the specified workspace.
     *
     * @param name workspace name
     * @return a <code>WorkspaceConfig</code> object or <code>null</code>
     *         if no such workspace exists.
     */
    public WorkspaceConfig getWorkspaceConfig(String name) {
        return (WorkspaceConfig) wspConfigs.get(name);
    }

    /**
     * Returns the configuration for the versioning
     *
     * @return a <code>VersioningConfig</code> object
     */
    public VersioningConfig getVersioningConfig() {
        return vConfig;
    }

    /**
     * Returns the access manager configuration
     *
     * @return an <code>AccessManagerConfig</code> object
     */
    public AccessManagerConfig getAccessManagerConfig() {
        return new AccessManagerConfig(amConfig);
    }

    /**
     * Creates a new workspace configuration with the specified name.
     *
     * @param name workspace name
     * @return a new <code>WorkspaceConfig</code> object.
     * @throws RepositoryException if the specified name already exists or
     *                             if an error occured during the creation.
     */
    public synchronized WorkspaceConfig createWorkspaceConfig(String name) throws RepositoryException {
        if (wspConfigs.containsKey(name)) {
            String msg = "A workspace with the specified name alreay exists";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // create the workspace folder (i.e. the workspace home directory)
        File wspFolder = new File(wspConfigRootDir, name);
        if (!wspFolder.mkdir()) {
            String msg = "Failed to create the workspace home directory: " + wspFolder.getPath();
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // clone the workspace definition template
        Element wspCongigElem = (Element) config.getRootElement().getChild(WORKSPACE_ELEMENT).clone();
        wspCongigElem.setAttribute(NAME_ATTRIB, name);

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
            log.debug(msg);
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
        WorkspaceConfig wspConfig =
            ConfigurationParser.parseWorkspaceConfig(
                    configFile.getPath(), configFile.getParent());
        wspConfigs.put(name, wspConfig);
        return wspConfig;
    }

}
