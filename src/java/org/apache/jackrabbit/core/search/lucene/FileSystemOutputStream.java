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
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.apache.lucene.store.OutputStream;

import java.io.IOException;

/**
 * Implements an lucene store OutputStream that is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystemResource}.
 */
class FileSystemOutputStream extends OutputStream {

    private final FileSystemResource res;

    private final RandomAccessOutputStream out;

    FileSystemOutputStream(FileSystemResource res) throws IOException {
        this.res = res;
        try {
            this.out = res.getRandomAccessOutputStream();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected void flushBuffer(byte[] b, int len) throws IOException {
        out.write(b, 0, len);
        out.flush();
    }

    public long length() throws IOException {
        try {
            return res.length();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void close() throws IOException {
        super.close();
        out.close();
    }

    public void seek(long pos) throws IOException {
        super.seek(pos);
        out.seek(pos);
    }
}
