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

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.jcr.RepositoryException;

/**
 * This class represents a binary value which is
 * backed by a resource or byte[]. Unlike <code>BinaryValue</code> it has no
 * state, i.e. the <code>getStream()</code> method always returns a fresh
 * <code>InputStream</code> instance.
 * <p/>
 */
public class BLOBValue extends BLOBFileValue {

    /**
     * The default logger
     */
    private static Logger log = LoggerFactory.getLogger(BLOBValue.class);

    /**
     * empty array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * max size for keeping tmp data in memory
     */
    private static final int MAX_BUFFER_SIZE = 0x10000;

    /**
     * underlying file
     */
    private final File file;

    /**
     * flag indicating if this instance represents a <i>temporary</i> value
     * whose dynamically allocated resources can be explicitly freed on
     * {@link #discard()}.
     */
    private final boolean temp;

    /**
     * buffer for small-sized data
     */
    private byte[] buffer = EMPTY_BYTE_ARRAY;

    /**
     * underlying file system resource
     */
    private final FileSystemResource fsResource;

    /**
     * Creates a new <code>BLOBFileValue</code> instance from an
     * <code>InputStream</code>. The contents of the stream is spooled
     * to a temporary file or to a byte buffer if its size is smaller than
     * {@link #MAX_BUFFER_SIZE}.
     * <p/>
     * The <code>temp</code> parameter governs whether dynamically allocated
     * resources will be freed explicitly on {@link #discard()}. Note that any
     * dynamically allocated resources (temp file/buffer) will be freed
     * implicitly once this instance has been gc'ed.
     *
     * @param in stream to be represented as a <code>BLOBFileValue</code> instance
     * @param temp flag indicating whether this instance represents a
     *             <i>temporary</i> value whose resources can be explicitly freed
     *             on {@link #discard()}.
     * @throws IOException if an error occurs while reading from the stream or
     *                     writing to the temporary file
     */
    BLOBValue(InputStream in, boolean temp) throws IOException {
        byte[] spoolBuffer = new byte[0x2000];
        int read;
        int len = 0;
        OutputStream out = null;
        File spoolFile = null;
        try {
            while ((read = in.read(spoolBuffer)) > 0) {
                if (out != null) {
                    // spool to temp file
                    out.write(spoolBuffer, 0, read);
                    len += read;
                } else if (len + read > MAX_BUFFER_SIZE) {
                    // threshold for keeping data in memory exceeded;
                    // create temp file and spool buffer contents
                    TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                    spoolFile = fileFactory.createTransientFile("bin", null, null);
                    out = new FileOutputStream(spoolFile);
                    out.write(buffer, 0, len);
                    out.write(spoolBuffer, 0, read);
                    buffer = null;
                    len += read;
                } else {
                    // reallocate new buffer and spool old buffer contents
                    byte[] newBuffer = new byte[len + read];
                    System.arraycopy(buffer, 0, newBuffer, 0, len);
                    System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                    buffer = newBuffer;
                    len += read;
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        // init vars
        file = spoolFile;
        fsResource = null;
        this.temp = temp;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a
     * <code>byte[]</code> array.
     *
     * @param bytes byte array to be represented as a <code>BLOBFileValue</code>
     *              instance
     */
    BLOBValue(byte[] bytes) {
        buffer = bytes;
        file = null;
        fsResource = null;
        // this instance is not backed by a temporarily allocated buffer
        temp = false;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a <code>File</code>.
     *
     * @param file file to be represented as a <code>BLOBFileValue</code> instance
     * @throws IOException if the file can not be read
     */
    BLOBValue(File file) throws IOException {
        String path = file.getCanonicalPath();
        if (!file.isFile()) {
            throw new IOException(path + ": the specified file does not exist");
        }
        if (!file.canRead()) {
            throw new IOException(path + ": the specified file can not be read");
        }
        this.file = file;
        // this instance is backed by a 'real' file; set virtual fs resource to null
        fsResource = null;
        // this instance is not backed by temporarily allocated resource/buffer
        temp = false;
    }

    /**
     * Creates a new <code>BLOBFileValue</code> instance from a resource in the
     * virtual file system.
     *
     * @param fsResource resource in virtual file system
     * @throws IOException if the resource can not be read
     */
    BLOBValue(FileSystemResource fsResource) throws IOException {
        try {
            if (!fsResource.exists()) {
                throw new IOException(fsResource.getPath()
                        + ": the specified resource does not exist");
            }
        } catch (FileSystemException fse) {
            IOException e2 = new IOException(fsResource.getPath()
                    + ": Error while creating value: " + fse.toString());
            e2.initCause(fse);
            throw e2;
        }
        // this instance is backed by a resource in the virtual file system
        this.fsResource = fsResource;
        // set 'real' file to null
        file = null;
        // this instance is not backed by temporarily allocated resource/buffer
        temp = false;
    }

    /**
     * Returns the length of this <code>BLOBFileValue</code>.
     *
     * @return The length, in bytes, of this <code>BLOBFileValue</code>,
     *         or -1L if the length can't be determined.
     */
    public long getLength() {
        if (file != null) {
            // this instance is backed by a 'real' file
            if (file.exists()) {
                return file.length();
            } else {
                return -1;
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                return fsResource.length();
            } catch (FileSystemException fse) {
                return -1;
            }
        } else {
            // this instance is backed by an in-memory buffer
            return buffer.length;
        }
    }

    /**
     * Frees temporarily allocated resources such as temporary file, buffer, etc.
     * If this <code>BLOBFileValue</code> is backed by a persistent resource
     * calling this method will have no effect.
     *
     * @see #delete(boolean)
     */
    public void discard() {
        if (!temp) {
            // do nothing if this instance is not backed by temporarily
            // allocated resource/buffer
            return;
        }
        if (file != null) {
            // this instance is backed by a temp file
            file.delete();
        } else if (buffer != null) {
            // this instance is backed by an in-memory buffer
            buffer = EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Deletes the persistent resource backing this <code>BLOBFileValue</code>.
     *
     * @param pruneEmptyParentDirs if <code>true</code>, empty parent directories
     *                             will automatically be deleted
     */
    public void delete(boolean pruneEmptyParentDirs) {
        if (file != null) {
            // this instance is backed by a 'real' file
            file.delete();
            if (pruneEmptyParentDirs) {
                // prune empty parent directories
                File parent = file.getParentFile();
                while (parent != null && parent.delete()) {
                    parent = parent.getParentFile();
                }
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                fsResource.delete(pruneEmptyParentDirs);
            } catch (FileSystemException fse) {
                // ignore
                log.warn("Error while deleting BLOBFileValue: " + fse.getMessage());
            }
        } else {
            // this instance is backed by an in-memory buffer
            buffer = EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isImmutable() {
        // delete will modify the state
        return false;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Returns a string representation of this <code>BLOBFileValue</code>
     * instance. The string representation of a resource backed value is
     * the path of the underlying resource. If this instance is backed by an
     * in-memory buffer the generic object string representation of the byte
     * array will be used instead.
     *
     * @return A string representation of this <code>BLOBFileValue</code> instance.
     */
    public String toString() {
        if (file != null) {
            // this instance is backed by a 'real' file
            return file.toString();
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            return fsResource.toString();
        } else {
            // this instance is backed by an in-memory buffer
            return buffer.toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BLOBValue) {
            BLOBValue other = (BLOBValue) obj;
            return ((file == null ? other.file == null : file.equals(other.file))
                    && (fsResource == null ? other.fsResource == null : fsResource.equals(other.fsResource))
                    && Arrays.equals(buffer, other.buffer));
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

    /**
     * {@inheritDoc}
     */
    public InputStream getStream()
            throws IllegalStateException, RepositoryException {
        // always return a 'fresh' stream
        if (file != null) {
            // this instance is backed by a 'real' file
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                throw new RepositoryException("file backing binary value not found",
                        fnfe);
            }
        } else if (fsResource != null) {
            // this instance is backed by a resource in the virtual file system
            try {
                return fsResource.getInputStream();
            } catch (FileSystemException fse) {
                throw new RepositoryException(fsResource.getPath()
                        + ": the specified resource does not exist", fse);
            }
        } else {
            return new ByteArrayInputStream(buffer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSmall() {
        return false;
    }

}
