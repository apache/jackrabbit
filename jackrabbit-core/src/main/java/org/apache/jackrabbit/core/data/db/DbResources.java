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

import java.io.InputStream;
import java.sql.ResultSet;

import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the resources used to back a database-based input stream.
 */
public class DbResources {

    private static Logger log = LoggerFactory.getLogger(DbResources.class);

    protected final ConnectionRecoveryManager conn;
    protected final ResultSet rs;
    protected final InputStream in;
    protected final DbDataStore store;
    protected boolean closed;
    
    /**
     * Construct a db resource using the specified input stream.
     * 
     * @param in the input stream
     */
    public DbResources(InputStream in) {
        this(null, null, in, null);
    }

    /**
     * Construct a db resource using the specified connection. The connection
     * will be returned to the data store once the resource is fully read. If
     * the connection is null, then this class is just a container for the input
     * stream. This is to support other kinds of input streams as well.
     * 
     * @param conn the connection (may be null)
     * @param rs the result set (may be null)
     * @param in the input stream
     * @param store the data store
     */
    public DbResources(ConnectionRecoveryManager conn, ResultSet rs, InputStream in, DbDataStore store) {
        this.conn = conn;
        this.rs = rs;
        this.in = in;
        this.store = store;
        if (conn == null) {
            closed = true;
        }
    }

    /**
     * Get the input stream.
     * 
     * @return the input stream
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Close the stream, and return the connection to the data store.
     */
    public void close() {
        if (!closed) {
            closed = true;
            DatabaseHelper.closeSilently(rs);
            try {
                store.putBack(conn);
            } catch (Exception e) {
                log.info("Closing DbResources: ", e);
            }
        }
    }
}
