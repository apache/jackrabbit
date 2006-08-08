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

import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * LaunchBackup is a command line tool and a demo tool for the backup tool. To
 * get all available options please type LaunchBackup --help
 * Used to launch a backup while the repository is inactive.
 *
 * @author Nicolas Toper <ntoper@gmail.com>
 * Date: 23-jun-06
 */
public class LaunchBackup {
    /**
     * TODO where can I find the generic repository.xml?
     * Path to the generic repository.xml
     */

    private static final String REPOSITORY_XML = "/home/ntoper/workspace/backup/";

    /**
     * The backupIOHandler used to handle IO
     */
    private static BackupIOHandler h;

    /**
     * Used to get a reference to the repository.
     */
    private RepositoryImpl repo;

    /**
     *  The backupconfig object.
     */
    private BackupConfig conf;
 
    /**
     * The repositoryConfig object.
     */
    private RepositoryConfig repoConf;

    /**
     * The backupManager is used to launch all backup/restore operations.
     */
    private BackupManager backup;


    /**
     * The command line tool.
     *
     * LaunchBackup --zip myzip.zip --conf backup.xml --login nico --password mlypass backup repository.xml repository/
     * LaunchBackup --zip ./myzip.zip --login nico --password p repository.xml  restore 
     * LaunchBackup --zip ./myzip.zip -- conf restore.xml --login nico --password p restore repository.xml repository/
     * 
     * If backup.xml for restore, no repository + backupConfig restore Only partial restore
     *
     * --zip: where is the zip file (only implemented way to backup for now)
     * --conf: path to the config file for the backup tool
     *
     *  backup/restore: whether you want a backup or a restore
     *
     *  repository.xml: path to the config file of the repository
     *
     * repository/ is the name of the repository
     *
     *
     * --help for help option
     *
     * @throws RepositoryException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws AccessDeniedException
     * @throws IOException
     */
    public static void main(String[] args) throws RepositoryException, AccessDeniedException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
       // I have to declare all var here so they are not resetted out of the for.
        String zipFile = null;
        String confFile = null;
        String home = null;
        String repoConfFile = null;
        String login = null;
        String password = null;

        //2 booleans in case the user specified nothing
        boolean isBackup = false;
        boolean isRestore = false;

        //Parse the command line.
        for (int i = 0; i < args.length; i++) {

            if ( args[i].equals("--help")  || args.length == 0) {
                usage();
            }

            if (args[i].equals("--zip")) {
                zipFile = args[i + 1];
            }

            if (args[i].equals("--conf")) {
                confFile = args[i + 1];
            }

            if (args[i].equals("--login")) {
                login = args[i + 1];
            }

            if (args[i].equals("--password")) {
                password = args[i + 1];
            }

            if (args[i].equals("backup") && !isRestore) {
                isBackup = true;
                repoConfFile = args[i + 1];
                home = args[i + 2];
            }

            if (args[i].equals("restore") && !isBackup ) {
                isRestore = true;
                repoConfFile = args[i + 1];
                home = args[i + 2];
            }
        }

        //Check if login and password are provided otherwise weird thing will happen
        if (login == null || password == null) {
            throw new LoginException();
        }

        LaunchBackup launch = null;

        //We need to shutdown properly the repository whatever happens
        try {
            //Launch backup
            if (isBackup) {
                launch = new LaunchBackup(repoConfFile, home, confFile, login, password);
                LaunchBackup.h = new ZipBackupIOHandler(zipFile, true);
                launch.backup(h);
            }
            //Launch restore
            else if (isRestore && confFile == null) {
                    LaunchBackup.h = new ZipBackupIOHandler(zipFile, false);
                    launch = new LaunchBackup(repoConfFile, home, login, password);
                    launch.restore(h);
            }
            else if (isRestore) {
                    LaunchBackup.h = new ZipBackupIOHandler(zipFile, false);
                    launch = new LaunchBackup(repoConfFile, home, confFile, login, password);
                    launch.restore(h);
            }
            //Launch nothing (if nothing specified
            else {
                usage();
            }
        }
        finally {
            if (launch != null) {
                launch.shutdown();
            }
        }
    }



    /**
     * Auxiliary method for main
     *
     */
    private static void usage() {
        System.out.println("todo: cut and paste of the comment when the project is over");
        System.exit(0);
    }

    /**
     * Constructor of LaunchBackup. Initiate the repository.
     *
     * @param String repoConfFile: name of the configuration file
     * @throws RepositoryException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws IOException
     *
     */
    public LaunchBackup(String repoConfFile, String home, String backupConfFile, String login, String password) throws RepositoryException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {

        //Launch first the repository
        this.repoConf = RepositoryConfig.create(repoConfFile, home);
        this.repo = RepositoryImpl.create(this.repoConf);

        //Create the backupConfig object
        this.conf = BackupConfig.create(backupConfFile, repoConfFile);
        this.backup =  BackupManager.create(this.repo, this.conf, login, password);
    }

    /**
     * Used for restore operations only.
     *
     * This constructor restores the repository! I don't see any other options since to restore we
     * need the repository and what is inside.
     *
     *
     * @param password
     * @param login
     * @param home
     * @throws RepositoryException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws
     *
     */
    public LaunchBackup(String repoConfFile, String home, String login, String password) throws RepositoryException, InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
        /*
         * There is a dissymetry there (due to design constraint: we wanted to be close of JR's way of working).
         * We need to restore BackupConfiguration and the Repository and we need each other to create them.
         */

        //Extract BackupConfig
        BackupConfigurationBackup b = new BackupConfigurationBackup();
        b.restore(h);
        //RepoConfFile isn't the right one. We know it
        BackupConfig bc;
        try {
            //We know we have restored it to backup.xml
            //There is no other way, except to break the abstract class and create
            //another restore methods. This seems fine and this way is unique/
            // If we have the issue again, we will evolve the design.
            bc = BackupConfig.create("backup.xml", repoConfFile);
            } catch (ConfigurationException e) {
             throw new RepositoryException();
            } catch (ClassNotFoundException e) {
                throw new RepositoryException();
            } catch (InstantiationException e) {
                throw new RepositoryException();
            }
         finally {
             //We need to delete it anyway
             File f = new File("backup.xml");
             f.delete();
         }

        //Restore repository
        RepositoryBackup br = new RepositoryBackup(repoConfFile, home);;
        br.setConf(bc);
        br.restore(h);
        RepositoryImpl repo = br.getRepo();

        this.backup = BackupManager.create(repo, bc, login, password);
        this.repo = this.backup.getRepo();
        this.conf = this.backup.getConf();
    }

    /**
    * Backup a repository
    *
    * @param BackupIOHandler h a reference where to backup
    */
    public void backup(BackupIOHandler h) throws AccessDeniedException, RepositoryException, IOException {
         this.backup.backup(h);
    }

    /**
     *Restore a repository
     *
     * @param BackupIOHandler h a reference to the backup to restore
     * @throws IOException 
     * @throws RepositoryException 
     * @throws IllegalAccessException 
     */
    public void restore(BackupIOHandler h) throws RepositoryException, IOException, IllegalAccessException {
        this.backup.restore(h);
    }

    private void shutdown() {
        this.repo.shutdown();
    }

}
