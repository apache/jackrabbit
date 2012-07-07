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
package org.apache.jackrabbit.core.data.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

/**
 * An input stream from a temporary file. The file is deleted when the stream is
 * closed, fully read, or garbage collected.
 * <p>
 * This class does not support mark/reset. It is always to be wrapped
 * using a BufferedInputStream.
 */
public class TempFileInputStream extends AutoCloseInputStream {

    private final File file;
    private boolean closed;
    private boolean delayedResourceCleanup = true;

    /**
     * Copy the data to a file and close the input stream afterwards.
     *
     * @param in the input stream
     * @param file the target file
     * @return the size of the file
     */
    public static long writeToFileAndClose(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        IOUtils.copy(in, out);
        out.close();
        in.close();
        return file.length();
    }

    /**
     * Construct a new temporary file input stream.
     * The file is deleted if the input stream is closed or fully read and 
     * delayedResourceCleanup was set to true. Otherwise you must call {@link #deleteFile()}.
     * Deleting is only attempted once.
     *
     * @param file the temporary file
     * @param delayedResourceCleanup
     */
    public TempFileInputStream(File file, boolean delayedResourceCleanup) throws FileNotFoundException {
        super(new BufferedInputStream(new FileInputStream(file)));
        this.file = file;
        this.delayedResourceCleanup = delayedResourceCleanup;
    }

    public File getFile() {
    	return file;
    }
    
    public void deleteFile() {
	    file.delete();
	}

	private int closeIfEOF(int read) throws IOException {
        if (read < 0) {
            close();
        }
        return read;
    }

    public void close() throws IOException {
        if (!closed) {
            in.close();
            if (!delayedResourceCleanup) {
            	deleteFile();
            }
            closed = true;
        }
    }

    public int available() throws IOException {
        return in.available();
    }

    /**
     * This method does nothing.
     */
    public void mark(int readlimit) {
        // do nothing
    }

    /**
     * Check whether mark and reset are supported.
     *
     * @return false
     */
    public boolean markSupported() {
        return false;
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public void reset() throws IOException {
        in.reset();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            return -1;
        }
        return closeIfEOF(in.read(b, off, len));
    }

    public int read(byte[] b) throws IOException {
        if (closed) {
            return -1;
        }
        return closeIfEOF(in.read(b));
    }

    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        return closeIfEOF(in.read());
    }

}
