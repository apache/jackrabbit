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

import java.io.FilterInputStream;
import java.io.IOException;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents an input stream backed by a database. It allows the stream to be used by
 * keeping the DB resources open until the stream is closed. When the stream is finished or
 * close()d, then the resources are freed.
 */
public class DbInputStream extends FilterInputStream {

    private static Logger log = LoggerFactory.getLogger(DbInputStream.class);

    protected DbResources resources;
    protected boolean streamFinished;
    protected boolean streamClosed;
    protected DbDataStore store;
    protected DataIdentifier identifier;

    /**
     * Create a database input stream for the given identifier.
     * Database access is delayed until the first byte is read from the stream.
     * 
     * @param store the database data store
     * @param identifier the data identifier
     */
    protected DbInputStream(DbDataStore store, DataIdentifier identifier) {
        super(null);
        streamFinished = false;
        streamClosed = true;
        this.store = store;
        this.identifier = identifier;
    }

    private void getStream() throws IOException {
        try {
            resources = store.getDatabaseResources(identifier);
            in = resources.getInputStream();
            streamClosed = false;
        } catch (DataStoreException e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * {@inheritDoc}
     * When the stream is consumed, the database resources held by the instance are closed.
     */
    public int read() throws IOException {
        if (streamFinished) {
            return -1;
        }
        if (in == null) {
            getStream();
        }
        int c = in.read();
        if (c == -1) {
            streamFinished = true;
            close();
        }
        return c;
    }

    /**
     * {@inheritDoc}
     * When the stream is consumed, the database resources held by the instance are closed.
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     * When the stream is consumed, the database resources held by the instance are closed.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if (streamFinished) {
            return -1;
        }
        if (in == null) {
            getStream();
        }
        int c = in.read(b, off, len);
        if (c == -1) {
            streamFinished = true;
            close();
        }
        return c;
    }

    /**
     * {@inheritDoc}
     * When the stream is consumed, the database resources held by the instance are closed.
     */
    public void close() throws IOException {
        if (!streamClosed) {
            streamClosed = true;
            // It may be null (see constructor)
            if (in != null) {
                in.close();
                super.close();
            }
            // resources may be null (if getStream() was not called)
            if (resources != null) {
                resources.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long skip(long n) throws IOException {
        if (in == null) {
            getStream();
        }
        return in.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        if (in == null) {
            getStream();
        }
        return in.available();
    }

    /**
     * {@inheritDoc}
     */
    public void mark(int readlimit) {
        if (in == null) {
            try {
                getStream();
            } catch (IOException e) {
                log.info("Error getting underlying stream: ", e);
            }
        }
        in.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    public void reset() throws IOException {
        if (in == null) {
            getStream();
        }
        in.reset();
    }

    /**
     * {@inheritDoc}
     */
    public boolean markSupported() {
        if (in == null) {
            try {
                getStream();
            } catch (IOException e) {
                log.info("Error getting underlying stream: ", e);
                return false;
            }
        }
        return in.markSupported();
    }
}
