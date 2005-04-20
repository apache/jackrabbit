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
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Repository configuration. This configuration class is used to
 * create configured repository objects.
 * <p>
 * The contained configuration information are: the home directory and name
 * of the repository, the access manager, file system, and versioning
 * configurations, the workspace directory, the default workspace name, and
 * the workspace configuration template. In addition the workspace
 * configuration object keeps track of all configured workspaces.
 */
public class RepositoryConfig {

    /** Name of the workspace configuration file. */
    private static final String WORKSPACE_XML = "workspace.xml";

    /**
     * Parses the given repository configuration file and returns the parsed
     * repository configuration. The given repository home directory path
     * will be used as the ${rep.home} parser variable.
     *
     * @param file repository configuration file
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static RepositoryConfig create(String file, String home)
            throws ConfigurationException {
        try {
            File config = new File(file);

            InputSource xml = new InputSource(new FileReader(config));
            xml.setSystemId(config.toURI().toString());

            return create(xml, home);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(
                    "The repository configuration file " + file
                    + " could not be found.", e);
        }
    }

    /**
     * Parses the given repository configuration document and returns the
     * parsed repository configuration. The given repository home directory
     * path will be used as the ${rep.home} parser variable.
     *
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
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

    /**
     * map of workspace names and workspace configurations
     */
    private Map workspaces;

    /**
     * Repository home directory.
     */
    private final String home;

    /**
     * Repository name for a JAAS app-entry configuration.
     */
    private final String name;

    /**
     * Repository access manager configuration;
     */
    private final AccessManagerConfig amc;

    /**
     * Repository login module configuration. Optional, can be null
     */
    private final LoginModuleConfig lmc;

    /**
     * Repository file system configuration.
     */
    private final FileSystemConfig fsc;

    /**
     * Name of the default workspace.
     */
    private final String defaultWorkspace;

    /**
     * Workspace root directory. This directory contains a subdirectory for
     * each workspace in this repository. Each workspace is configured by
     * a workspace configuration file contained in the workspace subdirectory.
     */
    private final String workspaceDirectory;

    /**
     * The workspace configuration template. Used in creating new workspace
     * configuration files.
     */
    private final Element template;

    /**
     * Repository versioning configuration.
     */
    private final VersioningConfig vc;

    /**
     * Creates a repository configuration object.
     *
     * @param template workspace configuration template
     * @param home repository home directory
     * @param name repository name for a JAAS app-entry configuration
     * @param amc access manager configuration
     * @param lmc login module configuration (can be <code>null</code>)
     * @param fsc file system configuration
     * @param workspaceDirectory workspace root directory
     * @param defaultWorkspace name of the default workspace
     * @param vc versioning configuration
     */
    RepositoryConfig(String home, String name,
            AccessManagerConfig amc, LoginModuleConfig lmc, FileSystemConfig fsc,
            String workspaceDirectory, String defaultWorkspace,
            Element template, VersioningConfig vc) {
        this.workspaces = new HashMap();
        this.home = home;
        this.name = name;
        this.amc = amc;
        this.lmc = lmc;
        this.fsc = fsc;
        this.workspaceDirectory = workspaceDirectory;
        this.defaultWorkspace = defaultWorkspace;
        this.template = template;
        this.vc = vc;
    }

    /**
     * Initializes the repository configuration. This method first initializes
     * the repository file system and versioning configurations and then
     * loads and initializes the configurations for all available workspaces.
     *
     * @throws ConfigurationException on initialization errors
     */
    private void init() throws ConfigurationException {
        fsc.init();
        vc.init();

        // Get the workspace root directory (create it if not found)
        File directory = new File(workspaceDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Get all workspace subdirectories
        File[] files = directory.listFiles();
        if (files == null) {
            throw new ConfigurationException(
                    "Invalid workspace root directory: " + workspaceDirectory);
        }

        for (int i = 0; i < files.length; i++) {
            WorkspaceConfig wc = loadWorkspaceConfig(files[i]);
            if (wc != null) {
                wc.init();
                addWorkspaceConfig(wc);
            }
        }

        if (workspaces.isEmpty()) {
            // create initial default workspace
            createWorkspaceConfig(defaultWorkspace);
        } else if (!workspaces.containsKey(defaultWorkspace)) {
            throw new ConfigurationException(
                    "no configuration found for default workspace: "
                    + defaultWorkspace);
        }
    }

    /**
     * Attempts to load a workspace configuration from the given workspace
     * subdirectory. If the directory contains a valid workspace configuration
     * file, then the configuration is parsed and returned as a workspace
     * configuration object. The returned configuration object has not been
     * initialized.
     * <p>
     * This method returns <code>null</code>, if the given directory does
     * not exist or does not contain a workspace configuration file. If an
     * invalid configuration file is found, then a
     * {@link ConfigurationException ConfigurationException} is thrown.
     *
     * @param directory workspace configuration directory
     * @return workspace configuration
     * @throws ConfigurationException if the workspace configuration is invalid
     */
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

            return parser.parseWorkspaceConfig(xml);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * Adds the given workspace configuration to the repository.
     *
     * @param wc workspace configuration
     * @throws ConfigurationException if a workspace with the same name
     *                                already exists
     */
    private void addWorkspaceConfig(WorkspaceConfig wc)
            throws ConfigurationException {
        String name = wc.getName();
        if (!workspaces.containsKey(name)) {
            workspaces.put(name, wc);
        } else {
            throw new ConfigurationException(
                    "Duplicate workspace configuration: " + name);
        }
    }

    /**
     * Creates a new workspace configuration with the specified name.
     * This method creates a workspace configuration subdirectory,
     * copies the workspace configuration template into it, and finally
     * adds the created workspace configuration to the repository.
     * The initialized workspace configuration object is returned to
     * the caller.
     *
     * @param name workspace name
     * @return created workspace configuration
     * @throws ConfigurationException if creating the workspace configuration
     *                                failed
     */
    public synchronized WorkspaceConfig createWorkspaceConfig(String name)
            throws ConfigurationException {
        // The workspace directory (TODO encode name?)
        File directory = new File(workspaceDirectory, name);

        // Create the directory, fail if it exists or cannot be created
        if (!directory.mkdir()) {
            throw new ConfigurationException(
                    "Failed to create configuration directory for workspace "
                    + name + ".");
        }

        // Create the workspace.xml file using the configuration template.
        try {
            template.setAttribute("name", name);
            File xml = new File(directory, WORKSPACE_XML);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(
                    new DOMSource(template), new StreamResult(xml));
        } catch (TransformerConfigurationException e) {
            throw new ConfigurationException(
                    "Cannot create a workspace configuration writer", e);
        } catch (TransformerException e) {
            throw new ConfigurationException(
                    "Cannot create a workspace configuration file", e);
        }

        // Load the created workspace configuration.
        WorkspaceConfig wc = loadWorkspaceConfig(directory);
        if (wc != null) {
            wc.init();
            addWorkspaceConfig(wc);
            return wc;
        } else {
            throw new ConfigurationException(
                    "Failed to load the created configuration for workspace "
                    + name + ".");
        }
    }

    /**
     * Returns the repository home directory.
     *
     * @return repository home directory
     */
    public String getHomeDir() {
        return home;
    }

    /**
     * Returns the repository file system implementation.
     *
     * @return file system implementation
     */
    public FileSystem getFileSystem() {
        return fsc.getFileSystem();
    }

    /**
     * Returns the repository name. The repository name can be used for
     * JAAS app-entry configuration.
     *
     * @return repository name
     */
    public String getAppName() {
        return name;
    }

    /**
     * Returns the repository access manager configuration.
     *
     * @return access manager configuration
     */
    public AccessManagerConfig getAccessManagerConfig() {
        return amc;
    }

    /**
     * Returns the repository login module configuration.
     * 
     * @return login module configuration, or <code>null</code> if standard
     *         JAAS mechanism should be used.
     */
    public LoginModuleConfig getLoginModuleConfig() {
        return lmc;
    }

    /**
     * Returns the workspace root directory.
     *
     * @return workspace root directory
     */
    public String getWorkspacesConfigRootDir() {
        return workspaceDirectory;
    }

    /**
     * Returns the name of the default workspace.
     *
     * @return name of the default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspace;
    }

    /**
     * Returns all workspace configurations.
     *
     * @return workspace configurations
     */
    public Collection getWorkspaceConfigs() {
        return workspaces.values();
    }

    /**
     * Returns the configuration of the specified workspace.
     *
     * @param name workspace name
     * @return workspace configuration, or <code>null</code> if the named
     *         workspace does not exist
     */
    public WorkspaceConfig getWorkspaceConfig(String name) {
        return (WorkspaceConfig) workspaces.get(name);
    }

    /**
     * Returns the repository versioning configuration.
     *
     * @return versioning configuration
     */
    public VersioningConfig getVersioningConfig() {
        return vc;
    }

}
