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
package org.apache.jackrabbit.backup;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.ConfigurationParser;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


/**
 * Backup configuration. This configuration class is used to
 * create configured backup objects.
 * <p>
 * The contained configuration information are: the home directory and name
 * of the repository, the access manager, file system and versioning
 * configuration, repository index configuration, the workspace directory,
 * the default workspace name, and the workspace configuration template. In
 * addition the workspace configuration object keeps track of all configured
 * workspaces.
 */
public class BackupConfig {
	
	/** the default logger */
    private static Logger log = LoggerFactory.getLogger(BackupConfig.class);
    
    /**
     * Convenience method that wraps the configuration file name into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param file repository configuration file name
     * @param home repository home directory
     * @return backup configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(String file, String home)
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
     * @return backup configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(URI uri, String home)
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
     * @return backup configuration
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(InputStream input, String home)
            throws ConfigurationException {
        return create(new InputSource(input), home);
    }

    /**
     * Parses the given repository configuration document and returns the
     * parsed and initialized repository configuration. The given repository
     * home directory path will be used as the ${rep.home} parser variable.
     * <p>
     * Note that in addition to parsing the repository configuration, this
     * method also initializes the configuration (creates the configured
     * directories, etc.). The {@link ConfigurationParser} class should be
     * used directly to just parse the configuration.
     *
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static BackupConfig create(InputSource xml, String home)
            throws ConfigurationException {
        Properties variables = new Properties();
        variables.setProperty(
                ConfigurationParser.REPOSITORY_HOME_VARIABLE, home);
        ConfigurationParser parser = new ConfigurationParser(variables);

        // TODO: Fix this
        // BackupConfig config = parser.parseBackupConfig(xml);
        // config.init();
        // return config;
        return null;
    }


    public BackupConfig() {
        // TODO Auto-generated constructor stub
    }

    public Backup getBackup() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setRepo(RepositoryImpl impl) {
        // TODO Auto-generated method stub
    }

}
