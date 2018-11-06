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

import static org.apache.jackrabbit.core.config.RepositoryConfigurationParser.REPOSITORY_CONF_VARIABLE;
import static org.apache.jackrabbit.core.config.RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE;

import org.apache.commons.io.IOUtils; 
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreFactory;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemFactory;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.QueryHandlerFactory;
import org.apache.jackrabbit.core.util.RepositoryLockMechanism;
import org.apache.jackrabbit.core.util.RepositoryLockMechanismFactory;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.jcr.RepositoryException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Repository configuration. This configuration class is used to
 * create configured repository objects.
 * <p>
 * The contained configuration information are: the home directory and name
 * of the repository, the access manager, file system and versioning
 * configuration, repository index configuration, the workspace directory,
 * the default workspace name, and the workspace configuration template. In
 * addition the workspace configuration object keeps track of all configured
 * workspaces.
 */
public class RepositoryConfig
        implements FileSystemFactory, DataStoreFactory, QueryHandlerFactory {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(RepositoryConfig.class);

    /** Name of the default repository configuration file. */
    private static final String REPOSITORY_XML = "repository.xml";

    /** Name of the workspace configuration file. */
    private static final String WORKSPACE_XML = "workspace.xml";

    /**
     * Returns the configuration of a repository in a given repository
     * directory. The repository configuration is read from a "repository.xml"
     * file inside the repository directory.
     * <p>
     * The directory is created if it does not exist. If the repository
     * configuration file does not exist, then it is created using the
     * default Jackrabbit configuration settings.
     *
     * @param dir repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws java.io.IOException If an error occurs.
     * @since Apache Jackrabbit 1.6
     */
    public static RepositoryConfig install(File dir)
            throws IOException, ConfigurationException {
        return install(new File(dir, REPOSITORY_XML), dir);
    }

    /**
     * Returns the configuration of a repository with the home directory,
     * configuration file, and other options as specified in the given
     * configuration parser variables. 
     * <p>
     * The directory is created if it does not exist. If the repository
     * configuration file does not exist, then it is created using the
     * default Jackrabbit configuration settings.
     *
     * @param variables parser variables
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws java.io.IOException If an error occurs.
     * @since Apache Jackrabbit 2.1
     */
    public static RepositoryConfig install(Properties variables)
            throws IOException, ConfigurationException {
        Properties copy = new Properties(variables);

        String home = copy.getProperty(REPOSITORY_HOME_VARIABLE);
        if (home == null) {
            home = copy.getProperty(
                    RepositoryFactoryImpl.REPOSITORY_HOME, "jackrabbit");
            copy.setProperty(REPOSITORY_HOME_VARIABLE, home);
        }
        File dir = new File(home);

        String conf = copy.getProperty(REPOSITORY_CONF_VARIABLE);
        if (conf == null) {
            conf = copy.getProperty(RepositoryFactoryImpl.REPOSITORY_CONF);
        }

        URL resource = RepositoryImpl.class.getResource(REPOSITORY_XML);
        if (conf == null) {
            conf = new File(dir, REPOSITORY_XML).getPath();
        } else if (conf.startsWith("classpath:")) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = RepositoryImpl.class.getClassLoader();
            }
            resource = loader.getResource(conf.substring("classpath:".length()));
            conf = new File(dir, REPOSITORY_XML).getPath();
        }

        File xml = new File(conf);
        installRepositorySkeleton(dir, xml, resource);
        return create(new InputSource(xml.toURI().toString()), copy);
    }

    public static File getRepositoryHome(Properties variables) {
        String home = variables.getProperty(REPOSITORY_HOME_VARIABLE);
        if (home == null) {
            home = variables.getProperty(
                    RepositoryFactoryImpl.REPOSITORY_HOME, "jackrabbit");
        }
        return new File(home);
    }

    /**
     * Returns the configuration of a repository with the given configuration
     * file and repository home directory.
     * <p>
     * The directory is created if it does not exist. If the repository
     * configuration file does not exist, then it is created using the
     * default Jackrabbit configuration settings.
     *
     * @param xml the configuration file.
     * @param dir repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws java.io.IOException If another error occurs.
     * @since Apache Jackrabbit 1.6
     */
    public static RepositoryConfig install(File xml, File dir)
            throws IOException, ConfigurationException {
        installRepositorySkeleton(
                dir, xml, RepositoryImpl.class.getResource(REPOSITORY_XML));
        return create(xml, dir);
    }

    private static void installRepositorySkeleton(
            File dir, File xml, URL resource)
            throws IOException, ConfigurationException {
        if (!dir.exists()) {
            log.info("Creating repository directory {}", dir);
            boolean dirCreated = dir.mkdirs();
            if (!dirCreated) {
                throw new ConfigurationException("Cannot create repository directory " + dir);
            }
        }

        if (!xml.exists()) {
            log.info("Copying configuration from {} to {}", resource, xml);
            OutputStream output = new FileOutputStream(xml);
            try {
                InputStream input = resource.openStream();
                try {
                    IOUtils.copy(input, output);
                } finally {
                   input.close();
                }
            } finally {
                output.close();
            }
        }
    }

    /**
     * Returns the configuration of a repository in a given repository
     * directory. The repository configuration is read from a "repository.xml"
     * file inside the repository directory.
     * <p>
     * An exception is thrown if the directory does not exist or if
     * the repository configuration file can not be read. 
     *
     * @since Apache Jackrabbit 1.6
     * @param dir repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static RepositoryConfig create(File dir)
            throws ConfigurationException {
        return create(new File(dir, REPOSITORY_XML), dir);
    }

    /**
     * Returns the configuration of a repository with the given configuration
     * file and repository home directory.
     * <p>
     * An exception is thrown if the directory does not exist or if
     * the repository configuration file can not be read. 
     *
     * @param xml The configuration file.
     * @param dir repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @since Apache Jackrabbit 1.6
     */
    public static RepositoryConfig create(File xml, File dir)
            throws ConfigurationException {
        if (!dir.isDirectory()) {
            throw new ConfigurationException(
                    "Repository directory " + dir + " does not exist");
        }

        if (!xml.isFile()) {
            throw new ConfigurationException(
                    "Repository configuration file " + xml + " does not exist");
        }

        return create(new InputSource(xml.toURI().toString()), dir.getPath());
    }

    /**
     * Convenience method that wraps the configuration file name into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param file repository configuration file name
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static RepositoryConfig create(String file, String home)
            throws ConfigurationException {
        URI uri = new File(file).toURI();
        return create(new InputSource(uri.toString()), home);
    }

    /**
     * Convenience method that wraps the configuration URI into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param uri repository configuration URI
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static RepositoryConfig create(URI uri, String home)
            throws ConfigurationException {
        return create(new InputSource(uri.toString()), home);
    }

    /**
     * Convenience method that wraps the configuration input stream into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param input repository configuration input stream
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static RepositoryConfig create(InputStream input, String home)
            throws ConfigurationException {
        return create(new InputSource(input), home);
    }

    /**
     * Convenience method that invokes the
     * {@link #create(InputSource, Properties)} method with the given
     * repository home home directory path set as the ${rep.home} parser
     * variable. Also all system properties are used as parser variables.
     *
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static RepositoryConfig create(InputSource xml, String home)
            throws ConfigurationException {
        Properties variables = new Properties(System.getProperties());
        variables.setProperty(REPOSITORY_HOME_VARIABLE, home);
        return create(xml, variables);
    }

    /**
     * Parses the given repository configuration document using the given
     * parser variables. Note that the ${rep.home} variable should be set
     * by the caller!
     * <p>
     * Note that in addition to parsing the repository configuration, this
     * method also initializes the configuration (creates the configured
     * directories, etc.). The {@link ConfigurationParser} class should be
     * used directly to just parse the configuration.
     *
     * @since Apache Jackrabbit 2.1
     * @param xml repository configuration document
     * @param variables parser variables
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static RepositoryConfig create(InputSource xml, Properties variables)
            throws ConfigurationException {
        RepositoryConfigurationParser parser =
            new RepositoryConfigurationParser(variables, new ConnectionFactory());

        RepositoryConfig config = parser.parseRepositoryConfig(xml);
        config.init();

        return config;
    }

    /**
     * Creates a repository configuration object based on an existing configuration. The factories
     * contained within the configuration will be newly initialized, but all other information
     * will be the same.
     *
     * @param config repository configuration to create the new instance from
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static RepositoryConfig create(RepositoryConfig config) throws ConfigurationException
    {
        RepositoryConfig copiedConfig = new RepositoryConfig(config.home, config.sec, config.fsf,
                config.workspaceDirectory, config.workspaceConfigDirectory, config.defaultWorkspace,
                config.workspaceMaxIdleTime, config.template, config.vc, config.qhf, config.cc,
                config.dsf, config.rlf, config.dsc, new ConnectionFactory(), config.parser);
        copiedConfig.init();
        return copiedConfig;
    }

    /**
     * map of workspace names and workspace configurations
     */
    private Map<String, WorkspaceConfig> workspaces;

    /**
     * Repository home directory.
     */
    private final String home;

    /**
     * The security config.
     */
    private final SecurityConfig sec;

    /**
     * Repository file system factory.
     */
    private final FileSystemFactory fsf;

    /**
     * Name of the default workspace.
     */
    private final String defaultWorkspace;

    /**
     * the default parser
     */
    private final RepositoryConfigurationParser parser;

    /**
     * Workspace physical root directory. This directory contains a subdirectory
     * for each workspace in this repository, i.e. the physical workspace home
     * directory. Each workspace is configured by a workspace configuration file
     * either contained in the workspace home directory or, optionally, located
     * in a subdirectory of {@link #workspaceConfigDirectory} within the
     * repository file system if such has been specified.
     */
    private final String workspaceDirectory;

    /**
     * Path to workspace configuration root directory within the
     * repository file system or null if none was specified.
     */
    private final String workspaceConfigDirectory;

    /**
     * Amount of time in seconds after which an idle workspace is automatically
     * shutdown.
     */
    private final int workspaceMaxIdleTime;

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
     * Query handler factory, or <code>null</code> if not configured.
     */
    private final QueryHandlerFactory qhf;

    /**
     * Optional cluster configuration.
     */
    private final ClusterConfig cc;

    /**
     * The data store factory.
     */
    private final DataStoreFactory dsf;
    
    /**
     * The repository lock mechanism factory.
     */
    private final RepositoryLockMechanismFactory rlf;

    /**
     * The configuration for the used DataSources.
     */
    private final DataSourceConfig dsc;

    /**
     * The {@link ConnectionFactory}
     */
    private final ConnectionFactory cf;

    /**
     * Creates a repository configuration object.
     *
     * @param home repository home directory
     * @param sec the security configuration
     * @param fsf file system factory
     * @param workspaceDirectory workspace root directory
     * @param workspaceConfigDirectory optional workspace configuration directory
     * @param defaultWorkspace name of the default workspace
     * @param workspaceMaxIdleTime maximum workspace idle time in seconds
     * @param template workspace configuration template
     * @param vc versioning configuration
     * @param qhf query handler factory for the system search manager
     * @param cc optional cluster configuration
     * @param dsf data store factory
     * @param rlf the RepositoryLockMechanismFactory
     * @param dsc the DataSource configuration
     * @param cf the ConnectionFactory for all DatabasAware beans
     * @param parser configuration parser
     */
    public RepositoryConfig(
            String home, SecurityConfig sec, FileSystemFactory fsf,
            String workspaceDirectory, String workspaceConfigDirectory,
            String defaultWorkspace, int workspaceMaxIdleTime,
            Element template, VersioningConfig vc, QueryHandlerFactory qhf,
            ClusterConfig cc, DataStoreFactory dsf,
            RepositoryLockMechanismFactory rlf,
            DataSourceConfig dsc,
            ConnectionFactory cf,
            RepositoryConfigurationParser parser) {
        workspaces = new HashMap<String, WorkspaceConfig>();
        this.home = home;
        this.sec = sec;
        this.fsf = fsf;
        this.workspaceDirectory = workspaceDirectory;
        this.workspaceConfigDirectory = workspaceConfigDirectory;
        this.workspaceMaxIdleTime = workspaceMaxIdleTime;
        this.defaultWorkspace = defaultWorkspace;
        this.template = template;
        this.vc = vc;
        this.qhf = qhf;
        this.cc = cc;
        this.dsf = dsf;
        this.rlf = rlf;
        this.dsc = dsc;
        this.cf = cf;
        this.parser = parser;
    }

    /**
     * Initializes the repository configuration. This method loads the
     * configurations for all available workspaces.
     *
     * @throws ConfigurationException on initialization errors
     * @throws IllegalStateException if the repository configuration has already
     *                               been initialized
     */
    public void init() throws ConfigurationException, IllegalStateException {
        
        // This needs to be done here and not by clients (e.g., RepositoryImpl ctor) because
        // fsf is used below and this might be a DatabaseAware FileSystem
        try {
            cf.registerDataSources(dsc);
        } catch (RepositoryException e) {
            throw new ConfigurationException("failed to register data sources", e);
        }

        if (!workspaces.isEmpty()) {
            throw new IllegalStateException(
                    "Repository configuration has already been initialized.");
        }

        // Get the physical workspace root directory (create it if not found)
        File directory = new File(workspaceDirectory);
        if (!directory.exists()) {
            boolean directoryCreated = directory.mkdirs();
            if (!directoryCreated) {
                throw new ConfigurationException("Cannot create workspace root directory " + directory);
            }
        }

        // Get all workspace subdirectories
        if (workspaceConfigDirectory != null) {
            // a configuration directory had been specified; search for
            // workspace configurations in virtual repository file system
            // rather than in physical workspace root directory on disk
            try {
                FileSystem fs = fsf.getFileSystem();
                try {
                    if (!fs.exists(workspaceConfigDirectory)) {
                        fs.createFolder(workspaceConfigDirectory);
                    } else {
                        String[] dirNames = fs.listFolders(workspaceConfigDirectory);
                        for (String dir : dirNames) {
                            String configDir = workspaceConfigDirectory
                            + FileSystem.SEPARATOR + dir;
                            WorkspaceConfig wc = loadWorkspaceConfig(fs, configDir);
                            if (wc != null) {
                                addWorkspaceConfig(wc);
                            }
                        }

                    }
                } finally {
                    fs.close();
                }
            } catch (Exception e) {
                throw new ConfigurationException(
                        "error while loading workspace configurations from path "
                        + workspaceConfigDirectory, e);
            }
        } else {
            // search for workspace configurations in physical workspace root
            // directory on disk
            File[] files = directory.listFiles();
            if (files == null) {
                throw new ConfigurationException(
                        "Invalid workspace root directory: " + workspaceDirectory);
            }

            for (File file: files) {
                WorkspaceConfig wc = loadWorkspaceConfig(file);
                if (wc != null) {
                    addWorkspaceConfig(wc);
                }
            }
        }
        if (!workspaces.containsKey(defaultWorkspace)) {
            if (!workspaces.isEmpty()) {
                log.warn("Potential misconfiguration. No configuration found "
                        + "for default workspace: " + defaultWorkspace);
            }
            // create initial default workspace
            createWorkspaceConfig(defaultWorkspace, (StringBuffer)null);
        }
    }

    /**
     * Attempts to load a workspace configuration from the given physical
     * workspace subdirectory. If the directory contains a valid workspace
     * configuration file, then the configuration is parsed and returned as a
     * workspace configuration object. The returned configuration object has not
     * been initialized.
     * <p>
     * This method returns <code>null</code>, if the given directory does
     * not exist or does not contain a workspace configuration file. If an
     * invalid configuration file is found, then a
     * {@link ConfigurationException ConfigurationException} is thrown.
     *
     * @param directory physical workspace configuration directory on disk
     * @return workspace configuration
     * @throws ConfigurationException if the workspace configuration is invalid
     */
    private WorkspaceConfig loadWorkspaceConfig(File directory)
            throws ConfigurationException {
        FileInputStream fin = null;
        try {
            File file = new File(directory, WORKSPACE_XML);
            fin = new FileInputStream(file);
            InputSource xml = new InputSource(fin);
            xml.setSystemId(file.toURI().toString());

            Properties variables = new Properties();
            variables.setProperty(
                    RepositoryConfigurationParser.WORKSPACE_HOME_VARIABLE,
                    directory.getPath());
            RepositoryConfigurationParser localParser =
                parser.createSubParser(variables);
            return localParser.parseWorkspaceConfig(xml);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    /**
     * Attempts to load a workspace configuration from the given workspace
     * subdirectory within the repository file system. If the directory contains
     * a valid workspace configuration file, then the configuration is parsed
     * and returned as a workspace configuration object. The returned
     * configuration object has not been initialized.
     * <p>
     * This method returns <code>null</code>, if the given directory does
     * not exist or does not contain a workspace configuration file. If an
     * invalid configuration file is found, then a
     * {@link ConfigurationException ConfigurationException} is thrown.
     *
     * @param fs virtual file system where to look for the configuration file
     * @param configDir workspace configuration directory in virtual file system
     * @return workspace configuration
     * @throws ConfigurationException if the workspace configuration is invalid
     */
    private WorkspaceConfig loadWorkspaceConfig(FileSystem fs, String configDir)
            throws ConfigurationException {
        Reader configReader = null;
        try {
            String configPath = configDir + FileSystem.SEPARATOR + WORKSPACE_XML;
            if (!fs.exists(configPath)) {
                // no configuration file in this directory
                return null;
            }

            configReader = new InputStreamReader(fs.getInputStream(configPath));
            InputSource xml = new InputSource(configReader);
            xml.setSystemId(configPath);

            // the physical workspace home directory (TODO encode name?)
            File homeDir = new File(
                    workspaceDirectory, FileSystemPathUtil.getName(configDir));
            if (!homeDir.exists()) {
                homeDir.mkdir();
            }
            Properties variables = new Properties(parser.getVariables());
            variables.setProperty(
                    RepositoryConfigurationParser.WORKSPACE_HOME_VARIABLE,
                    homeDir.getPath());
            RepositoryConfigurationParser localParser =
                parser.createSubParser(variables);
            return localParser.parseWorkspaceConfig(xml);
        } catch (FileSystemException e) {
            throw new ConfigurationException("Failed to load workspace configuration", e);
        } finally {
            IOUtils.closeQuietly(configReader);
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
     * Creates a new workspace configuration with the specified name and the
     * specified workspace <code>template</.
     * <p>
     * This method creates a workspace configuration subdirectory,
     * copies the workspace configuration template into it, and finally
     * adds the created workspace configuration to the repository.
     * The initialized workspace configuration object is returned to
     * the caller.
     *
     * @param name workspace name
     * @param template the workspace template
     * @param configContent optional stringbuffer that will have the content
     *        of workspace configuration file written in
     * @return created workspace configuration
     * @throws ConfigurationException if creating the workspace configuration
     *                                failed
     */
    private synchronized WorkspaceConfig internalCreateWorkspaceConfig(String name,
                                                                       Element template,
                                                                       StringBuffer configContent)
            throws ConfigurationException {

        // The physical workspace home directory on disk (TODO encode name?)
        File directory = new File(workspaceDirectory, name);

        // Create the physical workspace directory, fail if it exists
        // or cannot be created
        if (!directory.mkdir()) {
            if (directory.exists()) {
                throw new ConfigurationException(
                        "Workspace directory already exists: " + name);
            } else {
                throw new ConfigurationException(
                        "Failed to create workspace directory: " + name);
            }
        }

        FileSystem virtualFS;
        if (workspaceConfigDirectory != null) {
            // a configuration directoy had been specified;
            // workspace configurations are maintained in
            // virtual repository file system
            try {
                virtualFS = fsf.getFileSystem();
            } catch (RepositoryException e) {
                throw new ConfigurationException("File system configuration error", e);
            }
        } else {
            // workspace configurations are maintained on disk
            virtualFS = null;
        }
        try {
            Writer configWriter;

            // get a writer for the workspace configuration file
            if (virtualFS != null) {
                // a configuration directoy had been specified; create workspace
                // configuration in virtual repository file system rather than
                // on disk
                String configDir = workspaceConfigDirectory
                        + FileSystem.SEPARATOR + name;
                String configFile = configDir + FileSystem.SEPARATOR + WORKSPACE_XML;
                try {
                    // Create the directory
                    virtualFS.createFolder(configDir);

                    configWriter = new OutputStreamWriter(
                            virtualFS.getOutputStream(configFile));
                } catch (FileSystemException e) {
                    throw new ConfigurationException(
                            "failed to create workspace configuration at path "
                            + configFile, e);
                }
            } else {
                File file = new File(directory, WORKSPACE_XML);
                try {
                    configWriter = new FileWriter(file);
                } catch (IOException e) {
                    throw new ConfigurationException(
                            "failed to create workspace configuration at path "
                            + file.getPath(), e);
                }
            }

            // Create the workspace.xml file using the configuration template and
            // the configuration writer.
            try {
                template.setAttribute("name", name);

                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                if (configContent == null) {
                    transformer.transform(
                            new DOMSource(template), new StreamResult(configWriter));
                } else {
                    StringWriter writer = new StringWriter();
                    transformer.transform(
                            new DOMSource(template), new StreamResult(writer));

                    String s = writer.getBuffer().toString();
                    configWriter.write(s);
                    configContent.append(s);
                }
            } catch (IOException e) {
                throw new ConfigurationException(
                        "Cannot create a workspace configuration file", e);
            } catch (TransformerConfigurationException e) {
                throw new ConfigurationException(
                        "Cannot create a workspace configuration writer", e);
            } catch (TransformerException e) {
                throw new ConfigurationException(
                        "Cannot create a workspace configuration file", e);
            } finally {
                IOUtils.closeQuietly(configWriter);
            }

            // Load the created workspace configuration.
            WorkspaceConfig wc;
            if (virtualFS != null) {
                String configDir = workspaceConfigDirectory
                        + FileSystem.SEPARATOR + name;
                wc = loadWorkspaceConfig(virtualFS, configDir);
            } else {
                wc = loadWorkspaceConfig(directory);
            }
            if (wc != null) {
                addWorkspaceConfig(wc);
                return wc;
            } else {
                throw new ConfigurationException(
                        "Failed to load the created configuration for workspace "
                        + name + ".");
            }
        } finally {
            try {
                if (virtualFS != null) {
                    virtualFS.close();
                }
            } catch (FileSystemException ignore) {
            }
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
     * @param configContent optional StringBuffer that will have the content
     *        of workspace configuration file written in
     * @return created workspace configuration
     * @throws ConfigurationException if creating the workspace configuration
     *                                failed
     */
    public WorkspaceConfig createWorkspaceConfig(String name, StringBuffer configContent)
            throws ConfigurationException {
        // use workspace template from repository.xml
        return internalCreateWorkspaceConfig(name, template, configContent);
    }

    /**
     * Creates a new workspace configuration with the specified name. This
     * method uses the provided workspace <code>template</code> to create the
     * repository config instead of the template that is present in the
     * repository configuration.
     * <p>
     * This method creates a workspace configuration subdirectory,
     * copies the workspace configuration template into it, and finally
     * adds the created workspace configuration to the repository.
     * The initialized workspace configuration object is returned to
     * the caller.
     *
     * @param name workspace name
     * @param template the workspace template
     * @return created workspace configuration
     * @throws ConfigurationException if creating the workspace configuration
     *                                failed
     */
    public WorkspaceConfig createWorkspaceConfig(String name,
                                                 InputSource template)
            throws ConfigurationException {
        ConfigurationParser parser = new ConfigurationParser(new Properties());
        Element workspaceTemplate = parser.parseXML(template);
        return internalCreateWorkspaceConfig(name, workspaceTemplate, null);
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
     * Creates and returns the configured repository file system.
     *
     * @return the configured {@link FileSystem}
     * @throws RepositoryException if the file system can not be created
     */
    public FileSystem getFileSystem() throws RepositoryException {
        return fsf.getFileSystem();
    }

    /**
     * Returns the repository name. The repository name can be used for
     * JAAS app-entry configuration.
     *
     * @return repository name
     * @deprecated Use {@link SecurityConfig#getAppName()} instead.
     */
    public String getAppName() {
        return sec.getAppName();
    }

    /**
     * Returns the repository access manager configuration.
     *
     * @return access manager configuration
     * @deprecated Use {@link SecurityConfig#getAccessManagerConfig()} instead.
     */
    public AccessManagerConfig getAccessManagerConfig() {
        return sec.getAccessManagerConfig();
    }

    /**
     * Returns the repository login module configuration.
     *
     * @return login module configuration, or <code>null</code> if standard
     *         JAAS mechanism should be used.
     * @deprecated Use {@link SecurityConfig#getLoginModuleConfig()} instead.
     */
    public LoginModuleConfig getLoginModuleConfig() {
        return sec.getLoginModuleConfig();
    }

    /**
     * Returns the repository security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return sec;
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
     * Returns the amount of time in seconds after which an idle workspace is
     * automatically shutdown. If zero then idle workspaces will never be
     * automatically shutdown.
     *
     * @return maximum workspace idle time in seconds
     */
    public int getWorkspaceMaxIdleTime() {
        return workspaceMaxIdleTime;
    }

    /**
     * Returns all workspace configurations.
     *
     * @return workspace configurations
     */
    public Collection<WorkspaceConfig> getWorkspaceConfigs() {
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
        return workspaces.get(name);
    }

    /**
     * Returns the repository versioning configuration.
     *
     * @return versioning configuration
     */
    public VersioningConfig getVersioningConfig() {
        return vc;
    }

    /**
     * Checks whether search configuration is present.
     *
     * @return <code>true</code> if search is configured,
     *         <code>false</code> otherwise
     */
    public boolean isSearchEnabled() {
        return qhf != null;
    }

    /**
     * Returns the initialized query handler, or <code>null</code> if one
     * has not been configured.
     *
     * @return initialized query handler, or <code>null</code>
     */
    public QueryHandler getQueryHandler(QueryHandlerContext context)
            throws RepositoryException {
        if (qhf != null) {
            return qhf.getQueryHandler(context);
        } else {
            return null;
        }
    }

    /**
     * Returns the cluster configuration. Returns <code>null</code> if clustering
     * has not been configured.
     *
     * @return the cluster configuration or <code>null</code> if clustering
     * has not been configured.
     */
    public ClusterConfig getClusterConfig() {
        return cc;
    }

    /**
     * Returns the {@link ConnectionFactory} for this repository.
     * Please note that it must be closed explicitly.
     *
     * @return The connection factory configured for this repository.
     */
    public ConnectionFactory getConnectionFactory() {
        return cf;
    }

    /**
     * Creates and returns the configured data store. Returns
     * <code>null</code> if a data store has not been configured.
     *
     * @return the configured data store, or <code>null</code>
     * @throws RepositoryException if the data store can not be created
     */
    public DataStore getDataStore() throws RepositoryException {
        return dsf.getDataStore();
    }

    /**
     * Creates and returns the configured repository lock mechanism. This method
     * returns the default repository lock mechanism if no other mechanism is
     * configured.
     * 
     * @return the repository lock mechanism (never <code>null</code>)
     * @throws RepositoryException if the repository lock mechanism can not be created
     */
    public RepositoryLockMechanism getRepositoryLockMechanism() throws RepositoryException {
        return rlf.getRepositoryLockMechanism();
    }

}

