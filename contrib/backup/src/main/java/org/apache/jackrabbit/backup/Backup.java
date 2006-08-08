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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * This class is the abstract class of all resources to backup. If you need to add a new backuped resource
 * extend Backup and implement both the save and restore methods.
 *
 * The constructor is called when instantiating the specific backup resource class through BackupManager.
 */
public abstract class Backup {

    private RepositoryImpl repo;
    private BackupConfig conf;
    private Session session;;
    private SimpleCredentials credentials;

    /**
     * @param login
     * @param password
     * @param repo The repository to backup
     * @param conf The specific BackupConfig object (usually a subset of backup.xml)
     * @throws RepositoryException
     * @throws LoginException
     */
    public Backup(RepositoryImpl repo, BackupConfig conf, String login, String password) 
                                                throws LoginException, RepositoryException {
        this.repo = repo;
        this.conf = conf;
        this.credentials =  new SimpleCredentials(login, password.toCharArray());
        this.session = this.repo.login(this.credentials);
    }

    /**
     * Used only by BackupManager. No attributes are initialized.
     */
    protected Backup() {
    }
    
    /**
     * This constructor is used explicitly for restore operations
     * 
     * @param login
     * @param password
     */
    protected Backup(String login, String password) {
        this.credentials =  new SimpleCredentials(login, password.toCharArray());
    }
    /**
     * Used by BackupManager with the empty constructor.
     *
     * @param repo
     * @param conf
     * @param login
     * @param password
     * @throws LoginException
     * @throws RepositoryException
     */

    protected void init(RepositoryImpl repo, BackupConfig conf, String login, String password) throws LoginException, RepositoryException {
        this.repo = repo;
        this.conf = conf;
        this.credentials =  new SimpleCredentials(login, password.toCharArray());
        this.session = this.repo.login(this.credentials);
    }

    public RepositoryImpl getRepo() {
        return this.repo;
    }

    protected void setRepo(RepositoryImpl repo) {
        this.repo = repo;
    }

    /*
     * Each ResourceBackup is responsible to handle the backup.
     *
     * We use file when we cannot assume anything on the size of the data or we know it's big. When
     * we know the data is small we store it in RAM.
     *
     * For each resource
     *   Test maxFileSize
     * Zip the whole workingFolder
     * check the checksum
     * Send it to out
     */
   /**
    * Backup the resource designated by this class to h from the current repository
    * @param h
    * @throws FileNotFoundException
    * @throws RepositoryException
    * @throws IOException
    *
    */
    public abstract void backup(BackupIOHandler h) throws RepositoryException, IOException;

    /**
     * Restore the resource designated by this class from h to the current repository
     * @param h
     * @throws FileNotFoundException
     * @throws RepositoryException
     * @throws IOException
     */
    public abstract void restore(BackupIOHandler h) throws FileNotFoundException, RepositoryException, IOException;

    protected Session getSession() {
        return this.session;
    }

    protected BackupConfig getConf() {
        return conf;
    }

    protected void setConf(BackupConfig conf2) {
        this.conf = conf2;
    }

    protected SimpleCredentials getCredentials() {
        return credentials;
    }

    protected void setCredentials(SimpleCredentials cred) {
        this.credentials = cred;
    }

    public void finalize() {
        this.session.logout();
    }
}
