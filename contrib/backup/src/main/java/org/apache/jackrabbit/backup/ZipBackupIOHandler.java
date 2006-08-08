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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Question: ZipFile
 *
 * @author ntoper
 *
 */
public class ZipBackupIOHandler implements BackupIOHandler {
    
    private static int BUFFER_SIZE = 1024;
    
    private File zip;
    private FileOutputStream fout;
    private ZipOutputStream zipOut;

    private FileInputStream fin;
    private ZipInputStream zipIn;

    private boolean isBackup = false;


    public ZipBackupIOHandler(String zipFile, boolean isBackup) throws IOException {
        this.zip = new File(zipFile);
        this.isBackup = isBackup;

        if (isBackup) {
            this.fout = new FileOutputStream(this.zip);
            this.zipOut = new ZipOutputStream(this.fout);
        }
        else {
            this.fin = new FileInputStream(this.zip);
            this.zipIn = new ZipInputStream(this.fin);
        }
    }

    public void close() throws IOException {
        if (isBackup) {
            zipOut.finish();
            zipOut.close();
        }
        else {
            zipIn.close();
        }
    }

    /**
     * Create a directory per resources
     *   Backup the resource and zip it
     * @param string
     * @param content
     */
    public void write(String name, File f) throws IOException {
        zipOut.flush();
        ZipEntry e = new ZipEntry(name);
        zipOut.putNextEntry(e);

        Checksum crc = new CRC32();
        CheckedInputStream i = new CheckedInputStream(new FileInputStream(f), crc);

        byte[] buffer = new byte[BUFFER_SIZE];
        int len;       while ( (len = i.read(buffer, 0, BUFFER_SIZE)) != -1) {
            zipOut.write(buffer, 0, len);
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
            zipOut.write(buffer, 0, len);
        }
        
        //Checksum management
        // TODO Is crc up to date? To be checked...
        long check = crc.getValue();
        e.setCrc(check);
        zipOut.closeEntry();
    }
    
    public byte[] read(String zipEntry) throws ZipException, IOException {
        ZipFile zf = new ZipFile(this.zip);
        ZipEntry ze = new ZipEntry(zipEntry);
        long crc1 = ze.getCrc();
        
        Checksum chkCrc2 = new CRC32();
        InputStream is = zf.getInputStream(ze);
        CheckedInputStream cis = new CheckedInputStream(is, chkCrc2);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ( (len = cis.read(buffer, 0, BUFFER_SIZE)) != -1) {
            os.write(buffer, 0, len);
        }
        //TODO check CRC
       /* if (crc1 == chkCrc2.getValue()) {*/
            return os.toByteArray();
/*        }
        else {
            throw new ZipException();
        }*/
    }
    
    //TODO Refactor: the two upper are the same!! + quite similar to Backup
    public void read(String zipEntry, File myFile) throws ZipException, IOException {
        ZipFile zf = new ZipFile(this.zip);
        ZipEntry ze = new ZipEntry(zipEntry);
        //TODO check CRC

        Checksum chkCrc2 = new CRC32();
        InputStream is = zf.getInputStream(ze);
        CheckedInputStream cis = new CheckedInputStream(is, chkCrc2);
        
        OutputStream os = new FileOutputStream(myFile);

        
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ( (len = cis.read(buffer, 0, BUFFER_SIZE)) != -1) {
            os.write(buffer, 0, len);
        }
        
     /*   if (!(crc1 == chkCrc2.getValue())) {
            System.out.println("crc1:" + crc1 + "a crc2:"+ chkCrc2.getValue()  );
            throw new ZipException();
        }*/
    }

    public Enumeration getEntries() throws ZipException, IOException {
        ZipFile zf = new ZipFile(this.zip);
        return zf.entries();
    }
}
