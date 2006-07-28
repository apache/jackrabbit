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

import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.xml.SysViewSAXEventGenerator;
import org.xml.sax.SAXException;

//TODO Wiki doc to update
/**
 * @author ntoper
 *
 */
public class WorkspaceBackup extends Backup {

    private static int called = 0;
    private String wspName;

    /**
     * @param repo
     * @param conf
     * @throws RepositoryException 
     * @throws LoginException 
     */
    public WorkspaceBackup(RepositoryImpl repo, BackupConfig conf, String name) throws LoginException, RepositoryException {
        super(repo, conf);
        this.wspName = name;
    }
    
    public void init(RepositoryImpl repo, BackupConfig conf, String name) throws LoginException, RepositoryException {
        super.init(repo, conf);
        this.wspName = name;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
            IOException {
       SessionImpl s = (SessionImpl) repo.login(new SimpleCredentials(this.conf.getLogin(), this.conf.getPassword().toCharArray()), this.wspName);
       
       SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
       File temp = new File(this.conf.getWorkFolder() + "wsp.xml");
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
           h.write("export"+ called +".xml", temp);
       } catch (TransformerException te) {
           throw new RepositoryException(te);
       } catch (SAXException se) {
           throw new RepositoryException(se);
       } finally {
           temp.delete();
           called += 1;
       }

    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) {
        // TODO Auto-generated method stub

    }

}
