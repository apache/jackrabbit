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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * This class allows the backup and restore of all the worskspaces.
 *
 */
public class AllWorkspacesBackup extends Backup {

    /**
     * @param repo the repository
     * @param conf the BackupConfig object holding all informations about the backup/restore operations
     * @param login
     * @param password
     * @throws RepositoryException
     * @throws LoginException
     */
    public AllWorkspacesBackup(RepositoryImpl repo, BackupConfig conf, String login, String password) 
                                                            throws LoginException, RepositoryException {
        super(repo, conf, login, password);
    }

    /**
     * Constructor used by BackupManager.
     *
     */
    protected AllWorkspacesBackup() {
      super();
    }


    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
            IOException {
       Session s = this.getSession();
       Workspace wsp = s.getWorkspace();
       String[] allWsp = wsp.getAccessibleWorkspaceNames();
       String login = this.getCredentials().getUserID();
       String password = this.getCredentials().getPassword().toString();

       for (int i = 0; i < allWsp.length; i++) {
           WorkspaceBackup wspb = new WorkspaceBackup(this.getRepo(), this.getConf(), allWsp[i], login, password);
           wspb.backup(h);
           WorkspaceConfigBackup wspConfb = new WorkspaceConfigBackup(this.getRepo(), this.getConf(), allWsp[i], login, password);
           wspConfb.backup(h);
       }
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) throws ZipException, IOException, LoginException, RepositoryException {
        //Get All workspaces name in the zip
        Enumeration entries = h.getEntries();
        while (entries.hasMoreElements()) {
            String s = ((ZipEntry) entries.nextElement()).getName();
            if (s.indexOf("export_") != -1 && s.endsWith(".xml")) {
                int begin = "export_".length();
                //Allow to manage is we backup 110 workspaces for instance
                int end = s.length() - ".xml".length();
                String name = s.substring(begin, end);
                String login = this.getCredentials().getUserID();
                String password = this.getCredentials().getPassword().toString();

                //No need to check if the config file is there: if not, we will throw an exception later.
                //Restore the config
                WorkspaceConfigBackup wspConfb = new WorkspaceConfigBackup(this.getRepo(), this.getConf(), name, login, password);
                wspConfb.restore(h);

                //Restore the content
                WorkspaceBackup wsb = new WorkspaceBackup(this.getRepo(), this.getConf(), name, login, password);
                wsb.restore(h);
            }
        }
    }

}
