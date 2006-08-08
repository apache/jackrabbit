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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.SessionImporter;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

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
    public NodeVersionHistoriesBackup(RepositoryImpl repo, BackupConfig conf, String login, String password) throws LoginException, RepositoryException {
        super(repo, conf, login, password);
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
        
        File temp = new File(this.getConf().getWorkFolder() + "history.xml");
        
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
    /*  public void restore(BackupIOHandler h) throws ZipException, IOException, RepositoryException {
        SessionImpl s = (SessionImpl) this.getSession();
        VersionManagerImpl versionMgr = (VersionManager) s.getVersionManager();
        File temp = new File(this.getConf().getWorkFolder() + "history.xml");
        try {
        h.read("history.xml", temp);
        FileInputStream in = new FileInputStream(temp);
        
     //   this.getRepo().importXML("/jcr:system/jcr:versionStorage", in, 0 );
        }
        finally {
            temp.delete();
        }

        
        
       public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws RepositoryException, IOException {
            
            Path p = VersionManagerImpl.getVersionStoragePath();
            SessionImpl s = (SessionImpl) this.getSystemSession("default");
          
            //this.getItem()
             ItemImpl item = s.getItemManager().getItem(p);
             NodeImpl parent = (NodeImpl) item;
            SessionImporter importer = new SessionImporter(parent, s, 3);
            ImportHandler handler = new ImportHandler(importer, s.getNamespaceResolver(), this.getNamespaceRegistry());
            
            try {
            XMLReader parser =
            XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
            // being paranoid...
             parser.setFeature("http://xml.org/sax/features/namespaces", true);
             parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
             false);
             
             parser.parse(new InputSource(in));
             } catch (SAXException se) {
             // check for wrapped repository exception
              Exception e = se.getException();
              if (e != null && e instanceof RepositoryException) {
              throw (RepositoryException) e;
              } else {
              String msg = "failed to parse XML stream";
              throw new InvalidSerializedDataException(msg, se);
              }
              } 
            
        }
    }
        
      SessionImpl s = (SessionImpl) this.getSession();
         Path p;
         try {
         p = PathFormat.parse("/jcr:system/jcr:versionStorage", s.getNamespaceResolver()).getNormalizedPath();
         } catch (MalformedPathException e) {
         //Shouldn't happen or bug in the source code
          throw new RepositoryException();
          }
          ItemImpl item = s.getItemManager().getItem(p);
          NodeImpl parent = (NodeImpl) item;
          
          //TODO Add a parameter to specify the UUIDBehavior?
           SessionImporter importer = new SessionImporter(parent, s, 3);
           ImportHandler handler = new ImportHandler(importer, s.getNamespaceResolver(), this.getRepo().getNamespaceRegistry());
           File temp = new File(this.getConf().getWorkFolder() + "history.xml");
           h.read("history.xml", temp);
           FileInputStream in = new FileInputStream(temp);
           
           try {
           XMLReader parser =
           XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
           parser.setContentHandler(handler);
           parser.setErrorHandler(handler);
           // being paranoid...
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
            false);
            
            parser.parse(new InputSource(in));
            } catch (SAXException se) {
            // check for wrapped repository exception
             Exception e = se.getException();
             if (e != null && e instanceof RepositoryException) {
             throw (RepositoryException) e;
             } else {
             String msg = "failed to parse XML stream";
             throw new InvalidSerializedDataException(msg, se);
             }
             } finally {
             temp.delete();
             }
        
        //TODO find a way to put /jcr:system/jcr:versionStorage probably by instanciating as a repo/wsp the versioning pm
        File temp = new File(this.getConf().getWorkFolder() + "history.xml");
        SessionImpl s =  (SessionImpl) this.getSession();
        
        
        Path p = null;
        try {
            p = PathFormat.parse("/jcr:system/jcr:versionStorage", s.getNamespaceResolver()).getNormalizedPath();
        } catch (MalformedPathException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //Unprotect the tree...
        
        ItemImpl item = s.getItemManager().getItem(p);
      
        NodeImpl parent = (NodeImpl) item;
        unprotect(parent);
        
       
        

        try {
            
            h.read("history.xml", temp);
            FileInputStream iTemp = new FileInputStream(temp);
            s.importXML("/", iTemp, 0);
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ItemExistsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConstraintViolationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (VersionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidSerializedDataException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LockException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            
        }
        finally {
            temp.delete();
        }
        
        
        
        
    }*/
    public void restore(BackupIOHandler h) throws RepositoryException, IOException {
        File temp = new File(this.getConf().getWorkFolder() + "history.xml");
        SessionImpl s =  (SessionImpl) this.getSession();

        Path p = null;
        try {
            p = PathFormat.parse("/jcr:system/jcr:versionStorage", s.getNamespaceResolver()).getNormalizedPath();
        } catch (MalformedPathException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //Unprotect the tree...
        
        ItemImpl item = s.getItemManager().getItem(p);
      
        NodeImpl parent = (NodeImpl) item;
        unprotect(parent);

        try {  
            h.read("history.xml", temp);
            FileInputStream iTemp = new FileInputStream(temp);
            s.importXML("/", iTemp, 0);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            
        }
        finally {
            temp.delete();
        }
    }
    
    
    private static void unprotect(NodeImpl parent) throws RepositoryException {

        NodeDefinitionImpl def = (NodeDefinitionImpl) parent.getDefinition();
        NodeDefImpl nd = (NodeDefImpl) def.unwrap();
        //TODO After restore should we W protect the node?
        nd.setProtected(false);
        if (!def.isProtected())
            System.out.println(def.getName());
     
        NodeIterator it = parent.getNodes();
        
        while (it.hasNext()) {
           unprotect((NodeImpl) it.nextNode()); 
        } 
        
    }
}
