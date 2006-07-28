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

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeWriter;
import org.apache.jackrabbit.name.QName;

/**
 * @author ntoper
 *
 */
public class NodeTypeBackup extends Backup {

    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public NodeTypeBackup(RepositoryImpl repo, BackupConfig conf) throws LoginException, RepositoryException {
        super(repo, conf);
     }
    
    public NodeTypeBackup() {
        super();
     }


    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws IOException, RepositoryException {
        //Can we assume the default wsp always exist?
        Session s = this.getSession();
        Workspace wsp = s.getWorkspace();
        
        NodeTypeManagerImpl ntm = (NodeTypeManagerImpl) wsp.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntm.getNodeTypeRegistry();
        NamespaceRegistry ns = wsp.getNamespaceRegistry();
        NodeTypeDef[] ntd = getRegisteredNodesTypesDefs(ntreg);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NodeTypeWriter.write(out, ntd, ns);
        h.write("NodeType", out);     
    }
    
    
    /**
     * Returns the nodes types definitions of all registered node types.
     *
     * @return the node type definition of all registered node types.
     * @throws NoSuchNodeTypeException 
     */
    private static NodeTypeDef[] getRegisteredNodesTypesDefs(NodeTypeRegistry ntreg) throws NoSuchNodeTypeException {
    QName[] qn = ntreg.getRegisteredNodeTypes();
    NodeTypeDef[] ntd = new NodeTypeDef[qn.length];
    
    for (int i=0; i < qn.length; i++) {
        ntd[i] = ntreg.getNodeTypeDef(qn[i]);
    }
    return ntd;
    }
   

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub

    }

}
