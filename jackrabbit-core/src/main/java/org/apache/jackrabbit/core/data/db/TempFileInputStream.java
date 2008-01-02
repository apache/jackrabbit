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

/**
 * An input stream from a temp file that self-destructs when fully read or closed.
 */
public class TempFileInputStream extends InputStream {
    
    private final File file;
    private final InputStream in;
    private boolean closed;
    
    /**
     * Copy the data to a file and close the input stream afterwards.
     * 
     * @param in the input stream
     * @param file the target file
     * @return the size of the file
     */
    public static long writeToFileAndClose(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        byte[] b = new byte[4096];
        while (true) {
            int n = in.read(b);
            if (n < 0) {
                break;
            }
            out.write(b, 0, n);
        }
        out.close();
        in.close();
        return file.length();
    }
    
    /**
     * Construct a new temporary file input stream.
     * The file is deleted if the input stream is closed or fully read.
     * Deleting is only attempted once.
     * 
     * @param file the temporary file
     */
    TempFileInputStream(File file) throws FileNotFoundException {
        this.file = file;
        in = new BufferedInputStream(new FileInputStream(file));
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
            file.delete();
            closed = true;
        }
    }
    
    public int available() throws IOException {
        return in.available();
    }
    
    public void mark(int readlimit) {
        in.mark(readlimit);
    }
    
    public boolean markSupported() {
        return in.markSupported();
    }
    
    public long skip(long n) throws IOException {
        return in.skip(n);
    }
    
    public void reset() throws IOException {
        in.reset();
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        return closeIfEOF(in.read(b, off, len));
    }

    public int read(byte[] b) throws IOException {
        return closeIfEOF(in.read(b));
    }

    public int read() throws IOException {
        return closeIfEOF(in.read());
    }

}
