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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.core.RepositoryImpl;



/**
 * This class handles backup of the namespaces of the repository.
 * 
 * This class needs to be serializable so the internal class can be serialized (does anybody know why?)
 * 
 * @author ntoper
 *
 */
public class NamespaceBackup extends Backup implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4703796138774238005L;

    /**
     * This class holds all namespaces in a serializable way. We only put the relevant information.
     * (Do not change this class or you might lose backward compatibility; instead use another version)
     * 
     */
    private class Namespaces implements Serializable {
        
        private static final long serialVersionUID = 8384076353482950602L;
        
        HashMap h;


        public Namespaces() {
            h = new HashMap();            
        }
        
        public void addNamespace(String prefix, String uri) {
            h.put(prefix, uri);          
        }

    }

   /**
     * @param repo
     * @param conf
 * @throws RepositoryException 
 * @throws LoginException 
     */
    public NamespaceBackup(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        super(repo, conf);
       
        
       
    }
    
    public NamespaceBackup() {    
        super();
    }


    /* (non-Javadoc)
     * TODO where do I find the local ns?
     * TODO use a ByteArrayOutputStream?
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException, IOException {
        
       Session s = this.getSession();
       Workspace wsp = s.getWorkspace();
       NamespaceRegistry ns = wsp.getNamespaceRegistry();
       
       Namespaces myNs = new Namespaces();
        
       String[] allPrefixes = ns.getPrefixes();
              
       for (int i = 0; i < allPrefixes.length; i++) {
           String prefix = allPrefixes[i];
           myNs.addNamespace(prefix, ns.getURI(prefix));          
       }
       
       String name = this.getClass().toString();
       
       ByteArrayOutputStream fos = new ByteArrayOutputStream();
       ObjectOutputStream oos = new ObjectOutputStream(fos);
       oos.writeObject(myNs);       
       h.write(name, fos);     
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub

    }
    
    

}
