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

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.name.QName;

import java.io.File;
import java.io.FileInputStream;

//TODO provide options to start up and put relative path

/**
 * Third Jackrabbit example application. Imports an example XML file
 * and outputs the contents of the entire workspace.
 */
public class BackupTest {

    /** Runs the ThirdHop example. */
    public static void main(String[] args) throws Exception {
        // Set up a Jackrabbit repository with the specified
        // configuration file and repository directory
        Repository repository = new TransientRepository();

        // Login to the default workspace as a dummy user
        Session session = repository.login(
            new SimpleCredentials("username", "password".toCharArray()));
        try {
            // Use the root node as a starting point
            Node root = session.getRootNode();

            // Import the XML file unless already imported
            if (!root.hasNode("importxml")) {
                System.out.print("Importing xml... ");
                // Create an unstructured node under which to import the XML
                root.addNode("importxml", "nt:unstructured");
                // Import the file "test.xml" under the created node
                FileInputStream xml = new FileInputStream("src/test/test.xml");
                session.importXML(
                    "/importxml", xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                xml.close();
                // Save the changes to the repository
                session.save();
                System.out.println("done.");                                              
            }
            
            //Versionning
            //create versionable node
            Node n = root.addNode("childNode", "nt:unstructured");
            n.addMixin("mix:versionable");
            n.setProperty("anyProperty", "Blah");
            session.save();
            n.checkin();

            //add new version
            Node child = root.getNode("childNode");
            child.checkout();
            child.setProperty("anyProperty", "Blah2");
            session.save();
            child.checkin();
            
            //Creating a second workspace
            Workspace wsp = session.getWorkspace();
            String[] allWsp = wsp.getAccessibleWorkspaceNames();
            
            if (allWsp.length < 2) {
                ((WorkspaceImpl)wsp).createWorkspace("secondTest");
                session.logout();
                Session session2 = repository.login(new SimpleCredentials("username", "password".toCharArray()), "secondTest");
                root = session2.getRootNode();
                
                System.out.print("Importing xml in workspace secondTest... ");
                
                // Create an unstructured node under which to import the XML
                root.addNode("importxml", "nt:unstructured");
                // Import the file "test.xml" under the created node
                FileInputStream xml = new FileInputStream("src/test/test.xml");
                session2.importXML(
                    "/importxml", xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                xml.close();
                // Save the changes to the repository
                session2.save();
                System.out.println("done.");         
              
            }
            

            
            //Registering a NodeType
      /*      System.out.print("Registering a test nodeType...\r\n ");
       
            NodeTypeDef ntd = new NodeTypeDef();
            ntd.setMixin(true);
            ntd.setName(new QName("http://www.jcp.org/jcr/nt/1.0", "example"));
            registerNodeType(ntd, session);
            
            */
            
            System.out.print("Launching backup...\r\n ");
            
            /* Tested params:
             * --zip myzip.zip --size 2 --conf backup.xml backup repository.xml repository/
             */
            
            //Delete the zip file if existing
            File zip = new File("myzip.zip");
            zip.delete();
            
            String[] argsBackup ="--zip myzip.zip --login username --password password --conf src/test/backup.xml backup repository.xml repository/".split(" ");
            LaunchBackup.main(argsBackup); 
            System.out.print("Backup done. ");

          
        } finally {
            session.logout();
        }
    }
  
    private static void registerNodeType(NodeTypeDef nodeTypeDef, Session session) throws RepositoryException, InvalidNodeTypeDefException
    {
        //NodeTypeRegistry object
        Workspace wsp = session.getWorkspace();
        NodeTypeManager ntMgr = wsp.getNodeTypeManager();
        
        //non-JSR 170 - jackrabbit specific
        NodeTypeRegistry ntReg = 
                ((NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();
        
        ntReg.registerNodeType(nodeTypeDef);
    }


}