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
package org.apache.jackrabbit.vfs.ext.ds;

import java.io.IOException;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

/**
 * This input stream delays opening the file content until the first byte is read, and
 * closes and discards the underlying stream as soon as the end of input has
 * been reached or when the stream is explicitly closed.
 */
public class LazyFileContentInputStream extends AutoCloseInputStream {

    /**
     * The file object to read from.
     */
    protected final FileObject fileObject;

    /**
     * True if the input stream was opened. It is also set to true if the stream
     * was closed without reading (to avoid opening the file content after the stream
     * was closed).
     */
    protected boolean opened;

    /**
     * Creates a new <code>LazyFileInputStream</code> for the given file. If the
     * file is unreadable, a FileSystemException is thrown.
     * The file is not opened until the first byte is read from the stream.
     *
     * @param fileObject the file
     * @throws org.apache.commons.vfs2.FileNotFoundException
     * @throws org.apache.commons.vfs2.FileSystemException
     */
    public LazyFileContentInputStream(FileObject fileObject) throws FileSystemException {
        super(null);

        if (!fileObject.isReadable()) {
            throw new FileNotFoundException(fileObject.getName().getFriendlyURI());
        }

        this.fileObject = fileObject;
    }

    /**
     * Open the stream if required.
     *
     * @throws java.io.IOException
     */
    protected void open() throws IOException {
        if (!opened) {
            opened = true;
            in = fileObject.getContent().getInputStream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        open();
        return super.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        open();
        return super.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // make sure the file is not opened afterwards
        opened = true;

        // only close the file if it was in fact opened
        if (in != null) {
            super.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void reset() throws IOException {
        open();
        super.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        try {
            open();
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }

        return super.markSupported();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void mark(int readlimit) {
        try {
            open();
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }

        super.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        open();
        return super.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b) throws IOException {
        open();
        return super.read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        open();
        return super.read(b, off, len);
    }
}
