/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.lucene.store.InputStream;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.FileSystemException;

import java.io.IOException;

/**
 * Implements a lucene store InputStream that is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystemResource}.
 */
class FileSystemInputStream extends InputStream {

    private final FileSystemResource res;

    private java.io.InputStream in;

    private long position;

    FileSystemInputStream(FileSystemResource res) throws IOException {
        this.res = res;
        try {
            this.length = res.length();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected void readInternal(byte[] b, int offset, int length) throws IOException {
        checkOpen();
        position += in.read(b, offset, length);
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }

    protected void seekInternal(long pos) throws IOException {
        checkOpen();
        if (pos >= position) {
            in.skip(pos - position);
            position = pos;
        } else {
            // seeking backwards
            in.close();
            try {
                in = res.getInputStream();
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
            in.skip(pos);
            position = pos;
        }
    }

    public Object clone() {
        FileSystemInputStream clone = (FileSystemInputStream)super.clone();
        // decouple from this
        clone.in = null;
        clone.position = 0;
        return clone;
    }

    //----------------------------< internal >----------------------------------

    private void checkOpen() throws IOException {
        if (in == null) {
            try {
                in = res.getInputStream();
                length = res.length();
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
}
