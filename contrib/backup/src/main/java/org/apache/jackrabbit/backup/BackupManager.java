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
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * This class manages the backup/restore process. It is responsible to transmit it to the BackupIOHandler and to add the repository to the 
 * BackupConfig.
 * 
 * It extends Backup since it is based on the same semantics. However it is not at the same type as a ResourceBackup (indicated by different names)
 * 
 * It uses a work folder to get first all backup/restore information, zip them and send them to the handler.
 * 
 * @author ntoper
 *
 */
public class BackupManager extends Backup {
    
    public BackupManager(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        super(repo, conf);
        
        //Initiate correctly all objects in allResources
        Iterator it = this.conf.getAllResources().iterator();
        
        while(it.hasNext()) {
            Backup b = (Backup) it.next();
            b.init(repo, conf);
        }
    }
    
    
    public static BackupManager create(RepositoryImpl impl, BackupConfig conf2) throws LoginException, RepositoryException {
		return new BackupManager(impl, conf2);
	}
    /**
     * Used to backup the repository and all subclasses. Call all classes when needed.
     * This class stores the backup config file also. (simplify its fetching and logical since it's not a configurable resource)
     * 
     * TODO visibility of the conf is huge: each ResourceBackup can get and set others resources modifiers. Is it really bad?
     * 
     * @param The BackupIOHandler where the backup will be saved
     * @throws RepositoryException 
     * @throws IOException 
     * 
     */
    public void backup(BackupIOHandler h) throws RepositoryException, IOException {
        /* This method calls alternatively each backup method of each <Resource>Backup.
         *  It is responsible to initiate and close the zipFile.
         *  Each backup method, use the BackupIOHandler to write the file directly.
         */
        
        h.initBackup();
        try {
            
           
            Collection resources = this.conf.getAllResources();
            
         
            Iterator it = resources.iterator();
            
            while (it.hasNext()) {
                Backup b = (Backup) it.next();
                b.backup(h);
            }
        }
        finally  {
            h.close();
        }
    }
    
    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub
        
    }

   
 

}
