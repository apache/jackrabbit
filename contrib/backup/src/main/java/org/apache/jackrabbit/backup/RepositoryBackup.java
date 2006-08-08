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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipException;

import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;


import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDefStore;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

/**
 * @author ntoper
 *
 */
public class RepositoryBackup extends Backup {

    private String repoConfFile;
    private String home;

    /**
     * @param repo
     * @param conf
     * @throws RepositoryException
     * @throws LoginException
     */
    public RepositoryBackup(RepositoryImpl repo, BackupConfig conf, String login, String password) throws LoginException, RepositoryException {
        super(repo, conf, login, password);
    }

    public RepositoryBackup() {
        super();
    }

    public RepositoryBackup(String repoConfFile, String home) {
        super();
        this.repoConfFile = repoConfFile;
        this.home = home;
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

        File file = this.getConf().getRepoConfFile();

        //Backup repository.xml
        h.write("repository_xml", file);
    }

    public void restore(BackupIOHandler h) throws ZipException, IOException, RepositoryException {

       //Restore repository.xml
       File f = new File(this.repoConfFile);
       h.read("repository_xml", f);

       // Launch the repository and launch it.
       RepositoryConfig repoConf = RepositoryConfig.create(this.repoConfFile, this.home);
       this.setRepo(RepositoryImpl.create(repoConf));
       
       
//       this.getRepo().setNodeTypeRegistry(createNodeTypeRegistry(nsReg, new BasedFileSystem(this.getRepo().getStore()), "/nodetypes"));
          /*
           * 1. Create a NodeTypeRegistry specific for the restore (redefines only the load built in types path)
           * 2. Update the NodeTypeRegistry in repo
           */
  
       
    }


}