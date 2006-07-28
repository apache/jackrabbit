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
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Properties;


import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
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
    
    //TODO Useful?
    private PersistenceManagerConfig pmc;
    //Tused to backup a workspace first in a file
    private File workFolder;
    private Collection allResources;
    private File file;
    private File repoConfFile;
    private String login;
    private String password;
    
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
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
    public static BackupConfig create(String myFile, String repoConfFile, String login, String password)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        
        URI uri = new File(myFile).toURI();
        InputSource is = new InputSource(uri.toString());

        BackupConfigurationParser parser = new BackupConfigurationParser(new Properties());

        BackupConfig config = parser.parseBackupConfig(is, myFile, repoConfFile, login, password);
        
        return config;
    }
    
 

    //TODO see if path is really useful?
    public BackupConfig(PersistenceManagerConfig pmc, File path, Collection allResources, String myFile, String repoConfFile, String login, String password) throws IOException {
        
        //Logic application: not in the parser: this code has to be here
        if (!(path.isDirectory() && path.canWrite())) {
            throw new IOException();
        }     
        
        this.pmc = pmc;
        this.workFolder = path;
        this.allResources = allResources;
        this.file = new File(myFile);
        this.repoConfFile = new File(repoConfFile);
        this.password = password;
        this.login = login;
    }

    public Collection getAllResources() {
        return allResources;
    }

    public File getWorkFolder() {
        return workFolder;
    }

    public PersistenceManagerConfig getPmc() {
        return pmc;
    }

    public File getFile() {
        return this.file;       
    }



    public File getRepoConfFile() {
        return repoConfFile;
    }



    public String getPassword() {
        return this.password;
    }



    public String getLogin() {
        return this.login;
    }



}
