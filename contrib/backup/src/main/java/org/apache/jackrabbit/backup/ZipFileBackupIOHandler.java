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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Question: ZipFile
 * 
 * @author ntoper
 *
 */
public class ZipFileBackupIOHandler implements BackupIOHandler {

    int maxFileSize;
    File zip;
    FileInputStream fin;
    FileChannel fc;
    private ByteBuffer buffer;
	private FileOutputStream fout;

	
	public ZipFileBackupIOHandler(String zipFile) {
		this.zip = new File(zipFile);
        this.buffer = ByteBuffer.allocateDirect(2048);        
	}

	public void setMaxFileSize(int i) {
		this.maxFileSize = i;
    }

    public int getMaxFileSize() {
        return this.maxFileSize;
    }

    public void close() {
        // TODO Auto-generated method stub
        
    }
    
    public void init() {
       //Useful?
        this.buffer.clear();
    }

    public void initBackup() throws FileNotFoundException {
        this.fout = new FileOutputStream(this.zip);
        this.fc = this.fin.getChannel();      
    }
    
    public void initRestore() {
        try {
			this.fin = new FileInputStream(this.zip);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.fc = this.fin.getChannel();
        
    }


    
    
    /**
     * Create a directory per resources
     *   Backup the resource and zip it
     * @param string
     * @param content
     */
    /*  private void writeFile(String string, String content) {
             File conf = new File();
                FileWriter fw = new FileWriter(cheminAbstraitSortie);
                BufferedWriter tamponEcriture = new BufferedWriter(fluxEcritureTexte);
                tamponEcriture.write(xml);
                tamponEcriture.flush();
                tamponEcriture.close();
        
            }  */

}
