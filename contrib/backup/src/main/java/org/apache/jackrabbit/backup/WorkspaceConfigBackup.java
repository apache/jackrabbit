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

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.backup.Backup;

/**
 * @author ntoper
 *
 */
public class WorkspaceConfigBackup extends Backup {

    private static int called = 0;
    private String wspName;
    
    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public WorkspaceConfigBackup(RepositoryImpl repo, BackupConfig conf, String name) throws LoginException, RepositoryException {
        super(repo, conf);
        this.wspName = name;
    }
    
    public void init(RepositoryImpl repo, BackupConfig conf, String name) throws LoginException, RepositoryException {
        super.init(repo, conf);
        this.wspName = name;
    }

    public WorkspaceConfigBackup() {
        super();
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
            IOException {
        Session s = repo.login(new SimpleCredentials(this.conf.getLogin(), this.conf.getPassword().toCharArray()), this.wspName);
     
        WorkspaceImpl wsp = (WorkspaceImpl) s.getWorkspace();
        WorkspaceConfig c = wsp.getConfig();
        
        String home = c.getHomeDir();
        File wspXml = new File (home + "/workspace.xml");
        h.write("WspConf" + called , wspXml);
        called += 1;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub

    }

}
