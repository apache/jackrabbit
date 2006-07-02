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

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * This class is the abstract class of all resources to backup. If you need to add a new backuped resource
 * extend Backup and implement both the save and restore methods.
 *
 * The constructor is called when instantiating the specific backup resource class through RepositoryBackup.
 */
public abstract class Backup {

    RepositoryImpl repo;
    BackupConfig conf;
    String name;

    /**
     *
     * @param repo The repository to backup
     * @param conf The specific BackupConfig object (usually a subset of backup.xml)
     * @param name Name of the resource to backup. Unique. Useful?
     */
    public Backup(RepositoryImpl repo, BackupConfig conf) {
        this.repo = repo;
        this.conf = conf;
    }

    public void setRepo(RepositoryImpl repo) {
        this.repo = repo;
    }

    public RepositoryImpl getRepo() {
        return this.repo;
    }

    public abstract void backup(BackupIOHandler out, BackupConfig conf);
    public abstract void restore(BackupIOHandler in);

}
