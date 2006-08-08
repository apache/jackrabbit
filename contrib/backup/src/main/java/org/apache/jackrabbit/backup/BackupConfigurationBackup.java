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
import java.io.IOException;
import java.util.zip.ZipException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * Backup/Restore the XML file used to configure this backup.
 *
 * @author ntoper
 *
 */
public class BackupConfigurationBackup extends Backup {

    /**
     * @param repo
     * @param conf
     * @param login
     * @param password
     * @throws RepositoryException
     * @throws LoginException
     */
    public BackupConfigurationBackup(RepositoryImpl repo, BackupConfig conf, String login, String password) 
                                                                throws LoginException, RepositoryException {
        super(repo, conf, login, password);

    }

    protected BackupConfigurationBackup() {
        super();
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.backup.Backup#backup(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void backup(BackupIOHandler h) throws RepositoryException,
            IOException {
        File file = this.getConf().getFile();
        h.write("backup.xml", file);
    }

    /* (non-Javadoc)
     * This method is quite special. It is used to restore content from scratch. To break cyclic reference, we restore the file
     * in the current directory (we don't have yet the temporary one).
     *
     * @see org.apache.jackrabbit.backup.Backup#restore(org.apache.jackrabbit.backup.BackupIOHandler)
     */
    public void restore(BackupIOHandler h) throws ZipException, IOException {
        File conf = new File("backup.xml");
        h.read("backup.xml", conf);
    }
}
