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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;


import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * @author ntoper
 *
 */
public class RepositoryBackup extends Backup {

 
    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public RepositoryBackup(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        super(repo, conf);
    }
    
    public RepositoryBackup() {
        super();
    }


    /**
     * Backup the repository config file
     * 
     * TODO Backup properties? Metadata store? Other ressources?
     * @throws IOException 
     * @throws RepositoryException 
     * 
     * 
     */
    public void backup(BackupIOHandler h) throws IOException, RepositoryException {
        
        File file = this.conf.getRepoConfFile();

        //Backup repository.xml
        h.write("repository_xml", file);
        
        //Properties
        Properties p = new Properties();
        String[] keys = repo.getDescriptorKeys();
        for (int i = 0; i < keys.length; i++) {
            p.setProperty(keys[i], repo.getDescriptor(keys[i]));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.store(bos,"");
        h.write("repository_properties", bos);
        
        // Root node ID
        NodeImpl nod = (NodeImpl) this.getSession().getRootNode();
        NodeId n = nod.getNodeId();
        
        //We persist the string as a serialized object to avoid compatibility issue
        String s = n.toString();
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(s);       
        h.write("repository_rootNode", fos);
    }

    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub
        
    }

}
