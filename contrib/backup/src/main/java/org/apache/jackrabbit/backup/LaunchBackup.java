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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.xml.sax.InputSource;

/**
 * LaunchBackup is a command line tool and a demo tool for the backup tool. To
 * get all available options please type LaunchBackup --help
 * Used to launch a backup while the repository is inactive.
 *
 * @author Nicolas Toper <ntoper@gmail.com>
 * Date: 23-jun-06
 */
public class LaunchBackup {

	static BackupIOHandler h; 
	RepositoryImpl repo;
    BackupConfig conf;
    RepositoryConfig repoConf;
    RepositoryBackup backup;
  
    

    /**
     * The command line tool.
     *
     * LaunchBackup --zip myzip.zip --size 2 --conf backup.xml backup repository.xml repository/
     * LaunchBackup --zip ./myzip.zip --size 2 --conf backup.xml restore repository.xml repository/
     *
     * --zip: where is the zip file (only implemented way to backup for now)
     * --size in Go
     * 
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
     * @throws RepositoryException 
     * @throws IOException 
     * @throws IOException 
     *
     */
    public static void main(String[] args) throws RepositoryException, AccessDeniedException, IOException {
       // I have to declare all var here so they are not resetted out of the for.
    	String zipFile = null;
        String confFile = null;
        String home = null;
        String repoConfFile = null;

        //2 booleans in case the user specified nothing
        boolean isBackup = false;
        boolean isRestore = false;
        
        //Parse the command line.
    	for (int i = 0; i < args.length; i++) {
    		
            if ( args[i].equals("--help")  || args.length == 0) {
                usage();
            }
            
            if (args[i].equals("--zip")){
            	zipFile = args[i + 1];
            	//We put it here because later we might offer other possibilities than only zip
            	h = new ZipFileBackupIOHandler(zipFile);
            }
            

            if (args[i].equals("--size") && !(h != null)){
            	
            	Integer max = (new Integer(args[i+ 1]));
                h.setMaxFileSize(max.intValue());        	
            }
            
            if (args[i].equals("--size") && !(h != null)){
            	
            	Integer max = (new Integer(args[i+ 1]));
                h.setMaxFileSize(max.intValue());        	
            }
            

            if (args[i].equals("--conf") && !(h != null)){
            	
            	confFile = args[i + 1];
            	
            }
            
			if (args[i].equals("backup") && isRestore == false ){
            	isBackup = true;
            	repoConfFile = args[i + 1];
            	home = args[i + 2];
            	
            }
            
            if (args[i].equals("restore") && isBackup == false ){
            	isRestore = true;
            	repoConfFile = args[i + 1];
            	home = args[i + 2];
            } 
        }
		   		
    	LaunchBackup launch = new LaunchBackup(repoConfFile, home, confFile);
		
    	//We need to shutdown properly the repository whatever happens
		try {	
	    	//Launch backup
	    	if (isBackup) {
					launch.backup(h);    		   		
	    	}  	
	    	//Launch restore
	    	else if (isRestore) {
	    			launch.restore(h);
	    	}
	    	//Launch nothing (if nothing specified
	    	else {
	    		usage();
	    	}
		}
		finally
		{
			launch.shutdown();
		}
    }

 

	/**
     * Auxiliary method for main
     *
     */
    private static void usage(){
        System.out.println("todo: cut and paste of the comment when the project is over");
        System.exit(0);
    }

    /**
     * Constructor of LaunchBackup. Initiate the repository.
     *
     * @param String filename: name of the configuration file
     * @throws RepositoryException 
     * @throws FileNotFoundException 
     */
    public LaunchBackup(String repoConfFile, String home, String backupConfFile) throws RepositoryException, FileNotFoundException {
    	//Launch first the repository
		this.repoConf = RepositoryConfig.create(repoConfFile, home);
		this.repo = RepositoryImpl.create(this.repoConf);

		//Create the backupConfig object
		FileReader fr = new FileReader(backupConfFile);
		InputSource xml = new InputSource(fr);
		this.conf = BackupConfig.create(xml, home);
		this.backup =  RepositoryBackup.create(this.conf);
    }

    /**
    * Backup a repository
    *
    * @param BackupIOHandler h a reference wher to backup
    */
    public void backup(BackupIOHandler h) throws AccessDeniedException, RepositoryException, IOException {
        this.backup.backup(h);
    }

    /**
     *Restore a repository
     *
     * @param BackupIOHandler h a reference to the backup to restore
     */
    public void restore(BackupIOHandler h) {
    	this.backup.restore(h);
    }
    
    private void shutdown() {
		this.repo.shutdown();		
	}
    
}
