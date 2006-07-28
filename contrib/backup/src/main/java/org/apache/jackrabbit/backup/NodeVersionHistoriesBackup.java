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
import java.io.FileOutputStream;
import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * @author ntoper
 *
 */
public class NodeVersionHistoriesBackup extends Backup {

    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public NodeVersionHistoriesBackup(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        super(repo, conf);
        // TODO Auto-generated constructor stub
    }
    
    public NodeVersionHistoriesBackup() {
        super();
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
            IOException {
        Session s = this.getSession();
        
        File temp = new File(this.conf.getWorkFolder() + "history.xml");
        
        try {
            FileOutputStream out = new FileOutputStream(temp);
            s.exportSystemView("/jcr:system/jcr:versionStorage", out, false, false);
            h.write("history.xml", temp);
        }
        finally {
            temp.delete();
        }
   }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) {
        //TODO find a way to put /jcr:system/jcr:versionStorage probably by instanciating as a repo/wsp the versioning pm
      

    }

}
