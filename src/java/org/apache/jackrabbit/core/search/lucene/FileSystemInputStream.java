/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.lucene.store.InputStream;

import java.io.IOException;

/**
 * Implements a lucene store InputStream that is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystemResource}.
 */
class FileSystemInputStream extends InputStream {

    /** The underlying resource. */
    private final FileSystemResource res;

    /** The underlying input stream of the <code>FileSystemResource</code>. */
    private java.io.InputStream in;

    /** Current position in the stream. */
    private long position;

    /**
     * Creates a new <code>FileSystemInputStream</code> based on the
     * {@link org.apache.jackrabbit.core.fs.FileSystemResource} <code>res</code>.
     * @param res the resource this stream is based on.
     * @throws IOException if an error occurs creating the stream on the
     *   resource.
     */
    FileSystemInputStream(FileSystemResource res) throws IOException {
        this.res = res;
        try {
            this.length = res.length();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Reads <code>length</code> bytes into the array <code>b</code> starting
     * at <code>offset</code>.
     * @param b the byte array to write the date into.
     * @param offset the offset where to start writing the data.
     * @param length number of bytes to read in / write to <code>b</code>
     * @throws IOException if an error occurs reading from the stream or
     *   if the stream is unable to read <code>length</code> bytes.
     */
    protected void readInternal(byte[] b, int offset, int length) throws IOException {
        checkOpen();
        int total = 0;
        int read;
        while ((read = in.read(b, offset, length)) > 0) {
            total += read;
            offset += read;
            length -= read;
            position += read;
        }
        if (length > 0) {
            throw new IOException("readInternal: Unable to read " + (total + length)
                    + " bytes. Only read " + total);
        }
    }

    /**
     * Closes this <code>FileSystemInputStream</code> and also the underlying
     * <code>InputStream</code>.
     * @throws IOException if an error occurs while closing the underlying
     *  <code>InputStream</code>.
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    /**
     * Sets the current position to <code>pos</code>. The next read operation
     * will occur at the position <code>pos</code>.
     * @param pos the position where to seek to.
     * @throws IOException if an error occurs while seeking, or if pos &gt;
     *   {@link #getFilePointer()}.
     */
    protected void seekInternal(long pos) throws IOException {
        checkOpen();
        long skip;
        if (pos >= position) {
            skip = pos - position;
        } else {
            // seeking backwards
            in.close();
            try {
                in = res.getInputStream();
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
            skip = pos;
        }
        while (skip > 0) {
            long skipped = in.skip(skip);
            if (skipped == 0) {
                throw new IOException("seekInternal: Unable to skip " + skip + " bytes.");
            }
            skip -= skipped;
        }
        position = pos;
    }

    /**
     * Clones this <code>FileSystemInputStream</code>.
     * @return a clone of this <code>FileSystemInputStream</code>.
     */
    public Object clone() {
        FileSystemInputStream clone = (FileSystemInputStream) super.clone();
        // decouple from this
        clone.in = null;
        return clone;
    }

    //----------------------------< internal >----------------------------------

    /**
     * Opens a new <code>InputStream</code> on the underlying resource if
     * necessary.
     * @throws IOException if an error occurs creating a new
     *   <code>InputStream</code>.
     */
    private void checkOpen() throws IOException {
        if (in == null) {
            try {
                in = res.getInputStream();
                length = res.length();
                long skip = position;
                while (skip > 0) {
                    long skipped = in.skip(skip);
                    if (skipped == 0) {
                        throw new IOException("checkOpen: Unable to skip " + position + " bytes.");
                    }
                    skip -= skipped;
                }
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
}
