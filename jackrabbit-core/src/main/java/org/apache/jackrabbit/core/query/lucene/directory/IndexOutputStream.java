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

import java.io.OutputStream;
import java.io.IOException;

import org.apache.lucene.store.IndexOutput;

/**
 * <code>IndexOutputStream</code> wraps an {@link IndexOutput} and exposes it
 * as a regular {@link OutputStream}.
 */
public class IndexOutputStream extends OutputStream {

    /**
     * The underlying index output.
     */
    private final IndexOutput out;

    /**
     * Creates a new index output stream and wraps the given
     * <code>output</code>. Bytes will always be written at the end of the
     * <code>output</code>.
     *
     * @param output the lucene index output.
     * @throws IOException if an error occurs while seeking to the end of the
     *          index <code>output</code>.
     */
    public IndexOutputStream(IndexOutput output)
            throws IOException {
        this.out = output;
        this.out.seek(output.length());
    }

    /**
     * {@inheritDoc}
     */
    public void write(int b) throws IOException {
        byte[] buf = new byte[]{(byte) (b & 0xff)};
        write(buf, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte b[], int off, int len) throws IOException {
        out.writeBytes(b, off, len);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Flushes the underlying index output.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Closes the underlying index output.
     */
    public void close() throws IOException {
        out.close();
    }
}
