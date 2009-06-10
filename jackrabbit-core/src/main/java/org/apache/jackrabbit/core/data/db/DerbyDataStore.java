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
