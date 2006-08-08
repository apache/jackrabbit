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
import java.util.zip.ZipException;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.xml.SysViewSAXEventGenerator;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//TODO Wiki doc to update
/**
 * @author ntoper
 *
 */
public class WorkspaceBackup extends Backup {
    
    private String wspName;
    
    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public WorkspaceBackup(RepositoryImpl repo, BackupConfig conf, String name, String login, String password) throws LoginException, RepositoryException {
        super(repo, conf, login, password);
        this.wspName = name;
    }
    
    public void init(RepositoryImpl repo, BackupConfig conf, String name, String login, String password) throws LoginException, RepositoryException {
        super.init(repo, conf, login, password);
        this.wspName = name;
    }
    
    /* (non-Javadoc)
     * @see org.apache.jackrabbijcr:root/t.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
    IOException {
        SessionImpl s = (SessionImpl) this.getRepo().login(this.getCredentials(), this.wspName);
        
        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        File temp = new File(this.getConf().getWorkFolder() + "wsp.xml");
        try {
            TransformerHandler th = stf.newTransformerHandler();
            th.setResult(new StreamResult(new FileOutputStream(temp)));
            th.getTransformer().setParameter(OutputKeys.METHOD, "xml");
            th.getTransformer().setParameter(OutputKeys.ENCODING, "UTF-8");
            th.getTransformer().setParameter(OutputKeys.INDENT, "no");
            
            new SysViewSAXEventGenerator(
                    s.getRootNode(), false, false, th) {
                protected void process(Node node, int level)
                throws RepositoryException, SAXException {
                    if (!"/jcr:system".equals(node.getPath())) {
                        super.process(node, level);
                    }
                }
            }.serialize();
            h.write("export_"+ this.wspName +".xml", temp);
        } catch (TransformerException te) {
            throw new RepositoryException(te);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        } finally {
            temp.delete();
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) throws ZipException, IOException, LoginException, NoSuchWorkspaceException, RepositoryException {
        //TODO put temp and constant in object's attribute.
        
        //Restore the SysView in a temp file
        File wspXml = new File(this.getConf().getWorkFolder() + "/workspace.xml");
        File temp = new File(this.getConf().getWorkFolder() + "wsp.xml");

        try {
            FileInputStream fis = new FileInputStream(wspXml);
            InputSource xml = new InputSource(fis);
            
            //Launch & register the wsp
            //There is at least the default wsp.
            SessionImpl s1 = (SessionImpl) this.getSession();
            Workspace wsp_def = s1.getWorkspace();
            
            //Check if the workspace already exist (UC: partial restore)
            String[] allWsp = wsp_def.getAccessibleWorkspaceNames();
            boolean isCreated = false;
            
            for (int i = 0; i < allWsp.length; i++) {
                if (this.wspName.equals(allWsp[i])) {
                    isCreated = true;
                    break;
                }
            }

            if (!isCreated) {
                ((WorkspaceImpl) wsp_def).createWorkspace(this.wspName, xml);
            }

            h.read("export_"+ this.wspName +".xml", temp);

            SessionImpl s2 = (SessionImpl) this.getRepo().login(this.getCredentials(), this.wspName);

            FileInputStream iTemp = new FileInputStream(temp);
            //TODO add a parameter in the conf file to manage UUIDBehavior
            s2.importXML(s2.getRootNode().getPath(), iTemp, 3);
        }
        finally {
            wspXml.delete();
            temp.delete();
        }
    }
    
}
