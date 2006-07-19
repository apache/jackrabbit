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
import java.io.InputStream;
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
 * (ManagerBackup or WorkspaceBackup for instance).
 *
 */
public class BackupConfig {
    
    private PersistenceManagerConfig pmc;
    private File workFolder;
    private Collection allResources;
    private String xml;
    
    /**
     * Convenience method that wraps the configuration file name into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param file repository configuration file name
     * @param home repository home directory
     * @return backup configuration
     * @throws ConfigurationException on configuration errors
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SizeException 
     * @throws IOException 
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(String file)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException, IOException {
        URI uri = new File(file).toURI();
        return create(new InputSource(uri.toString()));
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
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SizeException 
     * @throws IOException 
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(URI uri)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException, IOException {
        return create(new InputSource(uri.toString()));
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
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SizeException 
     * @throws IOException 
     * @see #create(InputSource, String)
     */
    public static BackupConfig create(InputStream input, String home)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException, IOException {
        return create(new InputSource(input));
    }

    /**
     * Parses the given repository configuration document and returns the
     * parsed and initialized repository configuration. The given repository
     * home directory workFolder will be used as the ${rep.home} parser variable.
     * <p>
     * Note that in addition to parsing the repository configuration, this
     * method also initializes the configuration (creates the configured
     * directories, etc.). The {@link RepositoryConfigurationParser} class should be
     * used directly to just parse the configuration.
     *
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SizeException 
     * @throws IOException 
     */
    public static BackupConfig create(InputSource xml)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException, IOException {
        BackupConfigurationParser parser = new BackupConfigurationParser(new Properties());

        BackupConfig config = parser.parseBackupConfig(xml);
        
        return config;
    }
    
 

    //TODO see if path is really useful?
    public BackupConfig(PersistenceManagerConfig pmc, File path, Collection allResources) throws IOException {
        
        //Logic application: not in the parser: this code has to be here
        if (!(path.isDirectory() && path.canWrite())) {
            throw new IOException();
        }     
        
        this.pmc = pmc;
        this.workFolder = path;
        this.allResources = allResources;
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

    /*
     * Useful?
     */
    public Backup getBackup() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getXml() {
        return xml;
    }


}
