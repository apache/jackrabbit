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


import java.util.Collection;
import java.util.Iterator;
import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * This class manages the backup/restore process. It is responsible to transmit it to the BackupIOHandler and to add the repository to the 
 * BackupConfig.
 * 
 * It uses a work folder to get first all backup/restore information, zip them and send them to the handler.
 * 
 * @author ntoper
 *
 */
public class ManagerBackup extends Backup {
    
    public ManagerBackup(RepositoryImpl repo, BackupConfig conf) {
        super(repo, conf);
        
        //The repository is a special Resource: it is added here to the list of resource
        // It is not in the XML Conf file to make it more generic
        this.conf.getAllResources().add(new RepositoryBackup(repo, conf));
    }
    
    
    public static ManagerBackup create(RepositoryImpl impl, BackupConfig conf2) {
		return new ManagerBackup(impl, conf2);
	}
    /**
     * Used to backup the repository and all subclasses. Call all classes when needed.
     * This class stores the backup config file also. (simplify its fetching and logical since it's not a configurable resource)
     * 
     * TODO visibility of the conf is huge: each ResourceBackup can get and set others resources modifiers. Is it really bad?
     * 
     * @param The BackupIOHandler where the backup will be saved
     * 
     */
    public void backup(BackupIOHandler h) {
        /* This method calls alternatively each backup method of each <Resource>Backup.
         *  It is responsible to initiate and close the zipFile.
         *  Each backup method, use the BackupIOHandler to write the file directly.
         */
        
        try {
            
            h.init();
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
