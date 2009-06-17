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

import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Derby data store closes the database when the data store is closed
 * (embedded databases only).
 */
public class DerbyDataStore extends DbDataStore {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DerbyDataStore.class);

    public synchronized void close() throws DataStoreException {
        super.close();

        // check for embedded driver
        if (!DerbyPersistenceManager.DERBY_EMBEDDED_DRIVER.equals(getDriver())) {
            return;
        }

        try {

            // prepare connection url for issuing shutdown command
            ConnectionRecoveryManager connectionManager = getConnection();

            String url = connectionManager.getConnection().getMetaData().getURL();
            int pos = url.lastIndexOf(';');
            if (pos != -1) {
                // strip any attributes from connection url
                url = url.substring(0, pos);
            }
            url += ";shutdown=true";

            // we have to reset the connection to 'autoCommit=true' before closing it;
            // otherwise Derby would mysteriously complain about some pending uncommitted
            // changes which can't possibly be true.
            // @todo further investigate
            connectionManager.getConnection().setAutoCommit(true);

            // need to call it again because we just opened a connection,
            // and super.close() closes it.
            super.close();

            // now it's safe to shutdown the embedded Derby database
            try {
                DriverManager.getConnection(url);
            } catch (SQLException e) {
                // a shutdown command always raises a SQLException
                log.info(e.getMessage());
            }
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
    }
}
