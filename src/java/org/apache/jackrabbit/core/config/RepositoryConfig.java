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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * A <code>RepositoryConfig</code> ...
 */
public class RepositoryConfig {

    /** Name of the workspace configuration file. */
    private static final String WORKSPACE_XML = "workspace.xml";

    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified configuration file.
     *
     * @param file path to the configuration file
     * @param home repository home directory
     * @return a new <code>RepositoryConfig</code> instance
     * @throws ConfigurationException if an error occurs
     */
    public static RepositoryConfig create(String file, String home)
            throws ConfigurationException {
        try {
            File config = new File(file);

            InputSource xml = new InputSource(new FileReader(config));
            xml.setSystemId(config.toURI().toString());

            return create(xml, home);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("TODO", e);
        }
    }

    public static RepositoryConfig create(InputSource xml, String home)
            throws ConfigurationException {
        Properties variables = new Properties();
        variables.setProperty(
                ConfigurationParser.REPOSITORY_HOME_VARIABLE, home);
        ConfigurationParser parser = new ConfigurationParser(variables);

        RepositoryConfig config = parser.parseRepositoryConfig(xml);
        config.init();

        return config;
    }

    private Element config;
    private ConfigurationParser parser;

    /**
     * map of workspace names and workspace configurations
     */
    private Map wspConfigs;

    /**
     * repository home directory
     */
    private String repHomeDir;

    /**
     * virtual file system where the repository stores global state
     */
    private FileSystemConfig fsc;

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
    private AccessManagerConfig amConfig;

    /**
     * the versioning config
     */
    private VersioningConfig vConfig;

    public RepositoryConfig(
            Element config, ConfigurationParser parser,
            String home, String name, FileSystemConfig fsc,
            String root, String defaultWspName, AccessManagerConfig amc,
            VersioningConfig vc) {
        this.config = config;
        this.parser = parser;
        this.repHomeDir = home;
        this.appName = name;
        this.wspConfigs = new HashMap();
        this.fsc = fsc;
        this.wspConfigRootDir = root;
        this.defaultWspName = defaultWspName;
        this.amConfig = amc;
        this.vConfig = vc;
    }

    private void init() throws ConfigurationException {
        fsc.init();
        vConfig.init();

        File root = new File(wspConfigRootDir);
        if (!root.exists()) {
            root.mkdirs();
        }

        File[] files = root.listFiles();
        if (files == null) {
            throw new ConfigurationException(
                    "Invalid workspace root directory: " + wspConfigRootDir);
        }

        for (int i = 0; i < files.length; i++) {
            WorkspaceConfig config = loadWorkspaceConfig(files[i]);
            if (config != null) {
                config.init();
                addWorkspaceConfig(config);
            }
        }
    }

    private WorkspaceConfig loadWorkspaceConfig(File directory)
            throws ConfigurationException {
        try {
            File file = new File(directory, WORKSPACE_XML);
            InputSource xml = new InputSource(new FileReader(file));
            xml.setSystemId(file.toURI().toString());

            Properties variables = new Properties();
            variables.setProperty(
                    ConfigurationParser.WORKSPACE_HOME_VARIABLE,
                    directory.getPath());
            ConfigurationParser parser = new ConfigurationParser(variables);

            WorkspaceConfig config = parser.parseWorkspaceConfig(xml);
            return config;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private void addWorkspaceConfig(WorkspaceConfig config)
            throws ConfigurationException {
        String name = config.getName();
        if (!wspConfigs.containsKey(name)) {
            wspConfigs.put(name, config);
        } else {
            throw new ConfigurationException(
                    "Duplicate workspace configuration: " + name);
        }
    }

    /**
     * Creates a new workspace configuration with the specified name.
     *
     * @param name workspace name
     * @return a new <code>WorkspaceConfig</code> object.
     * @throws ConfigurationException if the specified name already exists or
     *                             if an error occured during the creation.
     */
    public synchronized WorkspaceConfig createWorkspaceConfig(String name)
            throws ConfigurationException {
        // create the workspace folder (i.e. the workspace home directory)
        File wspFolder = new File(wspConfigRootDir, name);
        if (!wspFolder.mkdir()) {
            String msg = "Failed to create the workspace home directory: " + wspFolder.getPath();
            throw new ConfigurationException(msg);
        }
        // clone the workspace definition template
        Element wspCongigElem = (Element) config.getChild("Workspace").clone();
        wspCongigElem.setAttribute("name", name);

        // create workspace.xml file
/*
        DocType docType = new DocType(WORKSPACE_ELEMENT, null, WorkspaceConfig.PUBLIC_ID);
        Document doc = new Document(wspCongigElem, docType);
*/
        Document doc = new Document(wspCongigElem);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        File configFile = new File(wspFolder, WORKSPACE_XML);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(configFile);
            out.output(doc, fos);
        } catch (IOException ioe) {
            String msg = "Failed to create workspace configuration file: " + configFile.getPath();
            throw new ConfigurationException(msg, ioe);
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
            Properties newVariables = new Properties();
            newVariables.setProperty(
                    ConfigurationParser.WORKSPACE_HOME_VARIABLE,
                    configFile.getParent());
            ConfigurationParser parser = new ConfigurationParser(newVariables);

            InputSource xml = new InputSource(new FileReader(configFile));
            xml.setSystemId(configFile.toURI().toString());

            return parser.parseWorkspaceConfig(xml);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("TODO", e);
        }
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
        return fsc.getFileSystem();
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
        return amConfig;
    }

}
