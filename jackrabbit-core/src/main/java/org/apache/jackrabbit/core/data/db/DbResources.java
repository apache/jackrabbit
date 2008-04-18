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
import java.sql.Statement;

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
    protected final Statement stmt;
    protected final InputStream in;
    protected final DbDataStore store;
    protected boolean closed;

    public DbResources(ConnectionRecoveryManager conn, ResultSet rs, Statement stmt, InputStream in, DbDataStore store) {
        this.conn = conn;
        this.rs = rs;
        this.stmt = stmt;
        this.in = in;
        this.store = store;
        this.closed = false;
    }

    public ConnectionRecoveryManager getConnection() {
        return conn;
    }

    public InputStream getInputStream() {
        return in;
    }

    public ResultSet getResultSet() {
        return rs;
    }

    public Statement getStatement() {
        return stmt;
    }

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
