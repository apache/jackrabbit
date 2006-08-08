/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE backupFile distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this backupFile to You under the Apache License, Version 2.0
 * (the "License"); you may not use this backupFile except in compliance with
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
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Properties;


import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.xml.sax.InputSource;


/**
 * Backup configuration. This configuration class is used to
 * create configured backup objects.
 * <p>
 * It will send different backup object, according to the expected type
 * (BackupManager or WorkspaceBackup for instance).
 *
 */
public class BackupConfig {

    //used to backup a workspace first in a backupFile
    private final File workFolder;
    //Not final since BackupManager adds some resources
    private Collection allResources;
    private final File backupFile;
    private final File repoConfFile;


    /**
     * Parses the given repository configuration document and returns the
     * parsed and initialized repository configuration. The given repository
     * home directory workFolder will be used as the ${rep.home} parser variable.
     * <p>
     * Note that in addition to parsing the repository configuration, this
     * method also initializes the configuration (creates the configured
     * directories, etc.). The {@link RepositoryConfigurationParser} class should be
     * used directly to just parse the configuration.
     * @param repoConfFile
     *
     * @param myFile repository configuration document
     * @param repoConfFile repository file configuration
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static BackupConfig create(String myFile, String repoConfFile)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        URI uri = new File(myFile).toURI();
        InputSource is = new InputSource(uri.toString());
        BackupConfigurationParser parser = new BackupConfigurationParser(new Properties());
        BackupConfig config = parser.parseBackupConfig(is, myFile, repoConfFile);
        return config;
    }

      public BackupConfig(File path, Collection allResources, String myFile, String repoConfFile) throws IOException {

        //Logic application: not in the parser: this code has to be here
        if (!(path.isDirectory() && path.canWrite())) {
            //if path not set in the conf file then create one as the current dir
            path = new File(".");
        }

        this.workFolder = path;
        this.allResources = allResources;
        this.backupFile = new File(myFile);
        this.repoConfFile = new File(repoConfFile);
    }

    public Collection getAllResources() {
        return allResources;
    }

    public void addResource(Backup b) {
        this.allResources.add(b);
    }

    public File getWorkFolder() {
        return workFolder;
    }


    public File getFile() {
        return this.backupFile;
    }

    public File getRepoConfFile() {
        return repoConfFile;
    }
}
