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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Question: ZipFile
 * 
 * @author ntoper
 *
 */
public class ZipFileBackupIOHandler implements BackupIOHandler {
    
    private static int BUFFER_SIZE = 1024;

    private File zip;
  //  private FileInputStream fin;
 //   private ByteBuffer buffer;
    private FileOutputStream fout;
    private ZipOutputStream zipOut;
    
    
    public ZipFileBackupIOHandler(String zipFile) throws FileNotFoundException {
        this.zip = new File(zipFile);
      //  this.buffer = ByteBuffer.allocateDirect(2048);        
    }

    public void close() throws IOException {
        zipOut.finish();
        zipOut.close();    
    }
    
  ///  private void init() {
       //Useful?
       // this.buffer.clear();
    //}

    public void initBackup() throws IOException {
        boolean a = this.zip.createNewFile(); 
        
        if (!a) {
            throw new IOException();
        }
        
        this.fout = new FileOutputStream(this.zip);
        this.zipOut = new ZipOutputStream(this.fout);
    }
    
    public void initRestore() throws FileNotFoundException {
  //      this.fin = new FileInputStream(this.zip);
      //  this.fcin = this.fin.getChannel();
        //Restore zipFile
    }

    /**
     * Create a directory per resources
     *   Backup the resource and zip it
     * @param string
     * @param content
     */
    /*private void writeFile(String string, String content) {
             File conf = new File();
                FileWriter fw = new FileWriter(cheminAbstraitSortie);
                BufferedWriter tamponEcriture = new BufferedWriter(fluxEcritureTexte);
                tamponEcriture.write(xml);
                tamponEcriture.flush();
                tamponEcriture.close();
        
            }  */
    
    public void read() {
    }
    
    
    public void write(String name, File f) throws IOException {
       zipOut.flush();
       ZipEntry e = new ZipEntry(name);
       zipOut.putNextEntry(e);
       
       Checksum crc = new CRC32();
       CheckedInputStream i = new CheckedInputStream(new FileInputStream(f), crc);
       
       byte[] buffer = new byte[BUFFER_SIZE]; 
       
       int len;  
       while ( (len = i.read(buffer, 0, BUFFER_SIZE)) != -1) {
           zipOut.write(buffer,0, len); 
        }
       
       //Checksum management
       // TODO Is crc up to date? To be checked...
       long check = crc.getValue();
       e.setCrc(check);
       zipOut.closeEntry(); 
    }

  
    /**
     * 
     * TODO: refactor this method with the one upper.
     * 
     * 
     * Used for small I/O operations (no NIO used there). Take a file and zip it.
     * 
     * Most I/O operations are operated on RAM.
     * 
     */
    public void write(String name, ByteArrayOutputStream fos) throws IOException {       
        zipOut.flush();
        ZipEntry e = new ZipEntry(name);
        zipOut.putNextEntry(e);
        
        Checksum crc = new CRC32();
        
        InputStream io = new ByteArrayInputStream(fos.toByteArray());
        
        CheckedInputStream i = new CheckedInputStream(io, crc);
        
        byte[] buffer = new byte[BUFFER_SIZE]; 
        int len;  
        while ( (len = i.read(buffer, 0, BUFFER_SIZE)) != -1) {
            zipOut.write(buffer,0, len); 
         }
    
        //Checksum management
        // TODO Is crc up to date? To be checked...
        long check = crc.getValue();
        e.setCrc(check);
        zipOut.closeEntry(); 
     }
   

}
