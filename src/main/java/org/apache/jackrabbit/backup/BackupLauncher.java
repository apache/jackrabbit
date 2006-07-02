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

import java.io.FileInputStream;
import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;

/**
 * BackupLauncher is a command line tool and a demo tool for the backup tool. To
 * get all available options please type BackupLauncher --help
 * Used to launch a backup while the repository is inactive.
 *
 * @author Nicolas Toper <ntoper@gmail.com>
 * Date: 23-jun-06
 */
public class BackupLauncher {

    BackupConfig conf;
    RepositoryImpl repo;

    /**
     * The command line tool.
     *
     * BackupLauncher --zip "myzip.zip" --size 2 save
     * BackupLauncher --zip "./myzip.zip" --size 2 restore
     *
     * -zip: where is the zip file (only implemented way to backup for now)
     * -size in Go
     *
     * --help for help option
     *
     */
    public static void main(String[] args) {

        for (int i = 0; i < args.length; i++) {
            if ( args[i].equals("--help")  || args.length == 0) {
                usage();
            }

            //To finish
            /* BackupIOHandler h = new ZipFileBackupIOHandler("mybackup.zip");
             h.setMaxFileSize(2);*/
            //repo.getBackupManager().save(h, config);
            //path to repository.xml
            // TODO Auto-generated method stub
        }
    }

    /**
     * Auxiliary method for main
     *
     */
    private static void usage(){
        System.out.println("options:\n --zip to specify the path of the zip file.\n --size (optional) max size of the file\n save|restore\n" +
                "example 1: java BackupLauncher --zip \"myzip.zip\" --size 2 save " +
        "example 2: java BackupLauncher --zip \"./myzip.zip\" --size 2 restore");
        System.exit(0);
    }

    /**
     * Constructor of BackupLauncher. Initiate the repository.
     *
     * @param String filename: name of the configuration file
     */
    public BackupLauncher(String filename) {
        //this.conf = new BackupConfig(filename);
        //How to launch the repository? JNDI?
    }

    public void backup(String out) throws AccessDeniedException, RepositoryException, IOException {
        //repo.getBackupRepository();
    }

    /**
     *
     * @param filename
     */
    public void restore(FileInputStream file) {
    }

}
