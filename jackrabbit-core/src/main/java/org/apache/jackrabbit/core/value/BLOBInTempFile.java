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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.LazyFileInputStream;
import org.apache.jackrabbit.util.TransientFileFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.jcr.RepositoryException;

/**
 * Represents binary data which is stored in a temporary file.
 */
class BLOBInTempFile extends BLOBFileValue {

    /**
     * the prefix of the string representation of this value
     */
    private static final String PREFIX = "file:";

    private File file;
    private long length;
    private final boolean temp;

    /**
     * Creates a new instance from a stream.
     * The input stream is always closed by this method.
     *
     * @param in the input stream
     * @param temp
     * @throws RepositoryException
     */
    private BLOBInTempFile(InputStream in, boolean temp) throws RepositoryException {
        this.temp = temp;
        OutputStream out = null;
        try {
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            file = fileFactory.createTransientFile("bin", null, null);
            out = new FileOutputStream(file);
            length = IOUtils.copyLarge(in, out);
        } catch (IOException e) {
            throw new RepositoryException("Error creating temporary file", e);
        } finally {
            IOUtils.closeQuietly(in);
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RepositoryException("Error creating temporary file", e);
                }
            }
        }
    }

    /**
     * Creates a new instance from file.
     *
     * @param file
     * @param temp
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
     * @param temp
     */
    static BLOBFileValue getInstance(InputStream in, boolean temp) throws RepositoryException {
        if (temp) {
            return new RefCountingBLOBFileValue(new BLOBInTempFile(in, temp));
        } else {
            return new BLOBInTempFile(in, temp);
        }
    }

    /**
     * Creates a new instance from a file.
     *
     * @param file the file
     */
    static BLOBInTempFile getInstance(File file, boolean temp) {
        return new BLOBInTempFile(file, temp);
    }

    void delete(boolean pruneEmptyParentDirs) {
        file.delete();
        length = -1;
        file = null;
    }

    public void dispose() {
        if (temp) {
            delete(true);
        }
    }

    BLOBFileValue copy() throws RepositoryException {
        if (temp) {
            return BLOBInTempFile.getInstance(getStream(), temp);
        } else {
            return BLOBInTempFile.getInstance(file, temp);
        }
    }

    public long getSize() {
        return length;
    }

    public InputStream getStream() throws IllegalStateException, RepositoryException {
        try {
            return new LazyFileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            throw new RepositoryException("file backing binary value not found", fnfe);
        }
    }

    public String toString() {
        return PREFIX + file.toString();
    }

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

    public int read(byte[] b, long position) throws IOException, RepositoryException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            raf.seek(position);
            return raf.read(b);
        } finally {
            raf.close();
        }
    }
}
