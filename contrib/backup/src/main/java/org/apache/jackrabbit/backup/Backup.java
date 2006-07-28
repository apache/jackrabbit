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

    RepositoryImpl repo;
    BackupConfig conf;
    Session session;

    /**
     *
     * @param repo The repository to backup
     * @param conf The specific BackupConfig object (usually a subset of backup.xml)
     * @param name Name of the resource to backup. Unique. Useful?
     * @throws RepositoryException 
     * @throws LoginException 
     */
    //TODO Useful?
    public Backup(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        this.repo = repo;
        this.conf = conf;
        this.session = this.repo.login(
                new SimpleCredentials(this.conf.getLogin(), this.conf.getPassword().toCharArray()));
        
    }
    
    public Backup() {
        
    }
    
    public void init(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        this.repo = repo;
        this.conf = conf;
        this.session = this.repo.login(
                new SimpleCredentials(this.conf.getLogin(), this.conf.getPassword().toCharArray()));
    }

    public RepositoryImpl getRepo() {
        return this.repo;
    }
      
    /*
     * Each ResourceBackup is responsible to handle the backup.
     * 
     * We use file when we cannot assume anything on the size of the data or we know it's big. When
     * we know the data is small we store it in RAM.
     * 
     *  
     * 
     * For each resource
     *   Test maxFileSize
     * Zip the whole workingFolder
     * check the checksum
     * Send it to out      
     */
    public abstract void backup(BackupIOHandler h) throws RepositoryException, IOException;
    public abstract void restore(BackupIOHandler h);

    public Session getSession() {
        return this.session;
    }

    //TODO call sesssion.logout or useless?
    

}
