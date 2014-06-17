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
package org.apache.jackrabbit.core.query.lucene.directory;

import org.apache.lucene.store.IndexInput;

import java.io.InputStream;
import java.io.IOException;

/**
 * <code>IndexInputStream</code> implements an {@link InputStream} that wraps
 * a lucene {@link IndexInput}.
 */
public class IndexInputStream extends InputStream {

    /**
     * The underlying index input.
     */
    private final IndexInput in;

    /**
     * The length of the index input.
     */
    private final long len;

    /**
     * The position where the next read will occur.
     */
    private long pos;

    /**
     * Creates a new index input stream wrapping the given lucene index
     * <code>input</code>.
     *
     * @param input the index input to wrap.
     */
    public IndexInputStream(IndexInput input) {
        this.in = input;
        this.len = input.length();
    }

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
        byte[] buf = new byte[1];
        if (read(buf, 0, 1) == -1) {
            return -1;
        } else {
            return buf[0] & 0xff;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (pos >= this.len) {
            // EOF
            return -1;
        }
        int num = (int) Math.min(len - off, this.len - pos);
        in.readBytes(b, off, num);
        pos += num;
        return num;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Closes the underlying index input.
     */
    public void close() throws IOException {
        in.close();
    }
}
