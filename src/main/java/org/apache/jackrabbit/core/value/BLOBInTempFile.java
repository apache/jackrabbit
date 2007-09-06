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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.util.TransientFileFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;

/**
 * Represents binary data which is stored in a temporary file.
 */
public class BLOBInTempFile extends BLOBFileValue {
    
    /**
     * the prefix of the string representation of this value
     */    
    private static final String PREFIX = "file:";
    
    private File file;
    private long length;
    private final boolean temp;
    
    /**
     * Creates a new instance from a stream.
     *
     * @param in the input stream
     * @throws IOException 
     */    
    private BLOBInTempFile(InputStream in, boolean temp) throws IOException {
        this.temp = temp;
        OutputStream out = null;
        try {
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            file = fileFactory.createTransientFile("bin", null, null);
            out = new FileOutputStream(file);
            byte[] buffer = new byte[4 * 1024];
            while (true) {
                int len = in.read(buffer);
                if (len < 0) {
                    break;
                }
                out.write(buffer, 0, len);
                length += len;                
            }
        } finally {
            if (out != null) {
                out.close();
            }
            in.close();
        }
    }

    /**
     * Creates a new instance from file.
     *
     * @param in the input stream
     */    
    private BLOBInTempFile(File file, boolean temp) {
        this.file = file;
        this.length = file.length();
        this.temp = temp;
    }

    /**
     * Creates a new instance from a stream.
     *
     * @param in the stream
     */    
    static BLOBInTempFile getInstance(InputStream in, boolean temp) throws IOException {
        return new BLOBInTempFile(in, temp);
    }
    
    /**
     * Creates a new instance from a file.
     *
     * @param file the file
     */    
    static BLOBInTempFile getInstance(File file, boolean temp) throws IOException {
        return new BLOBInTempFile(file, temp);
    }    
    
    /**
     * {@inheritDoc}
     */
    public void delete(boolean pruneEmptyParentDirs) {
        file.delete();
        length = -1;
        file = null;
    }

    /**
     * {@inheritDoc}
     */
    public void discard() {
        if (temp) {
            delete(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws IllegalStateException, RepositoryException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            throw new RepositoryException("file backing binary value not found", fnfe);
        }        
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return PREFIX + file.toString();
    }    
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BLOBInTempFile) {
            BLOBInTempFile other = (BLOBInTempFile) obj;
            return (file == other.file) || (length == other.length && file != null && file.equals(other.file));
        }
        return false;
    }
    
    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }    
    
}
