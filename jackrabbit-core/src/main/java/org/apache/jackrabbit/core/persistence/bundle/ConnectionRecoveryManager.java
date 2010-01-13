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
package org.apache.jackrabbit.core.persistence.bundle;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to get a database connection and to execute SQL statements.
 * It also contains reconnection logic. If the connection has been closed with the
 * {@link #close()} method, then  a call to any public method except for
 * {@link #setAutoReconnect(boolean)} will try to reestablish the connection, but
 * only if the <code>autoReconnect</code> equals <code>true</code>.
 * <p />
 * The reconnection attempt
 * can either be blocking or non-blocking, which is configured during construction.
 * In the latter case a fixed number of reconnection attempts is made. When the
 * reconnection failed an SQLException is thrown.
 * <p />
 * The methods of this class that execute SQL statements automatically call
 * {@link #close()} when they encounter an SQLException.
 *
 */
public class ConnectionRecoveryManager {

    /**
     * The default logger.
     */
    private static Logger log = LoggerFactory.getLogger(ConnectionRecoveryManager.class);

    /**
     * The database driver.
     */
    private final String driver;

    /**
     * The database URL.
     */
    private final String url;

    /**
     * The database user.
     */
    private final String user;

    /**
     * The database password.
     */
    private final String password;

    /**
     * The database connection that is managed by this {@link ConnectionRecoveryManager}.
     */
    private Connection connection;

    /**
     * An internal flag governing whether an automatic reconnect should be
     * attempted after a SQLException had been encountered in
     * {@link #executeStmt(String, Object[])}.
     */
    private boolean autoReconnect = true;

    /**
     * Indicates whether the reconnection function should block
     * until the connection is up again.
     */
    private final boolean block;

    /**
     * Time to sleep in ms before a reconnect is attempted.
     */
    private static final int SLEEP_BEFORE_RECONNECT = 500;

    /**
     * Number of reconnection attempts per method call. Only
     * used if <code>block == false</code>.
     */
    public static final int TRIALS = 20;

    /**
     * The map of prepared statements
     */
    private HashMap<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

    /**
     * Indicates whether the managed connection is open or closed.
     */
    private boolean isClosed;

    /**
     * Creates a {@link ConnectionRecoveryManager} and establishes
     * a database Connection using the driver, user, password and url
     * arguments.
     * <p />
     * By default, the connection is in auto-commit mode, and this
     * manager tries to reconnect if the connection is lost.
     *
     * @param block whether this class should block until the connection can be recovered
     * @param driver the driver to use for the connection
     * @param url the url to use for the connection
     * @param user the user to use for the connection
     * @param password the password to use for the connection
     * @throws RepositoryException if the database driver could not be loaded
     */
    public ConnectionRecoveryManager(
            boolean block, String driver, String url, String user, String password)
            throws RepositoryException {
        this.block = block;
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
        try {
            setupConnection();
            isClosed = false;
        } catch (SQLException e) {
            logException("could not setup connection", e);
            close();
        }
    }

    /**
     * Gets the database connection that is managed. If the
     * connection has been closed, and autoReconnect==true
     * then an attempt is made to reestablish the connection.
     *
     * @return the database connection that is managed
     * @throws SQLException on error
     * @throws RepositoryException if the database driver could not be loaded
     */
    public synchronized Connection getConnection() throws SQLException, RepositoryException {
        if (isClosed) {
            if (autoReconnect) {
                reestablishConnection();
            } else {
                throw new SQLException("connection has been closed and autoReconnect == false");
            }
        }
        return connection;
    }

    /**
     * Starts a transaction. I.e., the auto-commit is set to false,
     * and the manager does not try to reconnect if the connection
     * is lost. This method call should be followed by a call to
     * <code>endTransaction</code>.
     *
     * @throws SQLException on error
     */
    public synchronized void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    /**
     * Executes the given SQL query. Retries once or blocks (when the
     * <code>block</code> parameter has been set to true on construction)
     * if this fails and autoReconnect is enabled.
     *
     * @param sql the SQL query to execute
     * @return the executed ResultSet
     * @throws SQLException on error
     * @throws RepositoryException if the database driver could not be loaded
     */
    public synchronized ResultSet executeQuery(String sql) throws SQLException, RepositoryException {
        int trials = 2;
        SQLException lastException  = null;
        do {
            trials--;
            try {
                return executeQueryInternal(sql);
            } catch (SQLException e) {
                lastException = e;
            }
        } while(autoReconnect && (block || trials > 0));
        throw lastException;
    }

    /**
     * Executes the given SQL query.
     *
     * @param sql query to execute
     * @return a <code>ResultSet</code> object
     * @throws SQLException if an error occurs
     * @throws RepositoryException if the database driver could not be loaded
     */
    private ResultSet executeQueryInternal(String sql) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = preparedStatements.get(sql);
            if (stmt == null) {
                stmt = getConnection().prepareStatement(sql);
                preparedStatements.put(sql, stmt);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            logException("could not execute statement", e);
            close();
            throw e;
        } finally {
            resetStatement(stmt);
        }
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql statement to execute
     * @param params parameters to set
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException if an error occurs
     * @throws RepositoryException if the database driver could not be loaded
     */
    public PreparedStatement executeStmt(String sql, Object[] params) throws SQLException, RepositoryException {
        return executeStmt(sql, params, false, 0);
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql statement to execute
     * @param params parameters to set
     * @param returnGeneratedKeys if the statement should return auto generated keys
     * @param maxRows the maximum number of rows to return (0 for all rows)
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException if an error occurs
     * @throws RepositoryException if the database driver could not be loaded
     */
    public synchronized PreparedStatement executeStmt(
            String sql, Object[] params, boolean returnGeneratedKeys, int maxRows)
            throws SQLException, RepositoryException {
        int trials = 2;
        SQLException lastException  = null;
        do {
            trials--;
            try {
                return executeStmtInternal(sql, params, returnGeneratedKeys, maxRows);
            } catch (SQLException e) {
                lastException = e;
            }
        } while(autoReconnect && (block || trials > 0));
        throw lastException;
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     *
     * @param sql statement to execute
     * @param params parameters to set
     * @param returnGeneratedKeys if the statement should return auto generated keys
     * @param maxRows the maximum number of rows to return (0 for all rows)
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException if an error occurs
     * @throws RepositoryException if the database driver could not be loaded
     */
    private PreparedStatement executeStmtInternal(
            String sql, Object[] params, boolean returnGeneratedKeys, int maxRows)
            throws SQLException, RepositoryException {
        try {
            String key = sql;
            if (returnGeneratedKeys) {
                key += " RETURN_GENERATED_KEYS";
            }
            PreparedStatement stmt = preparedStatements.get(key);
            if (stmt == null) {
                if (returnGeneratedKeys) {
                    stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                } else {
                    stmt = getConnection().prepareStatement(sql);
                }
                preparedStatements.put(key, stmt);
            }
            stmt.setMaxRows(maxRows);
            return executeStmtInternal(params, stmt);
        } catch (SQLException e) {
            logException("could not execute statement", e);
            close();
            throw e;
        }
    }

    /**
     * Closes all resources held by this {@link ConnectionRecoveryManager}.
     * An ongoing transaction is discarded.
     */
    public synchronized void close() {
        preparedStatements.clear();
        try {
            if (connection != null) {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                }
                connection.close();
            }
        } catch (SQLException e) {
            logException("failed to close connection", e);
        }
        connection = null;
        isClosed = true;
    }

    /**
     * Creates the database connection.
     *
     * @throws SQLException on error
     * @throws RepositoryException if the database driver could not be loaded
     */
    private void setupConnection() throws SQLException, RepositoryException {
        try {
            connection = ConnectionFactory.getConnection(driver, url, user, password);
        } catch (SQLException e) {
            log.warn("Could not connect; driver: " + driver + " url: " + url + " user: " + user + " error: " + e.toString(), e);
            throw e;
        }
        // JCR-1013: Setter may fail unnecessarily on a managed connection
        if (!connection.getAutoCommit()) {
            connection.setAutoCommit(true);
        }
        try {
            DatabaseMetaData meta = connection.getMetaData();
            log.info("Database: " + meta.getDatabaseProductName() + " / " + meta.getDatabaseProductVersion());
            log.info("Driver: " + meta.getDriverName() + " / " + meta.getDriverVersion());
        } catch (SQLException e) {
            log.warn("Can not retrieve database and driver name / version", e);
        }
    }

    /**
     * @param params the parameters for the <code>stmt</code> parameter
     * @param stmt the statement to execute
     * @return the executed Statement
     * @throws SQLException on error
     */
    private PreparedStatement executeStmtInternal(Object[] params, PreparedStatement stmt) throws SQLException {
        for (int i = 0; params != null && i < params.length; i++) {
            Object p = params[i];
            if (p instanceof StreamWrapper) {
                StreamWrapper wrapper = (StreamWrapper) p;
                stmt.setBinaryStream(i + 1, wrapper.stream, (int) wrapper.size);
            } else if (p instanceof InputStream) {
                InputStream stream = (InputStream) p;
                stmt.setBinaryStream(i + 1, stream, -1);
            } else {
                stmt.setObject(i + 1, p);
            }
        }
        stmt.execute();
        resetStatement(stmt);
        return stmt;
    }

    /**
     * Re-establishes the database connection.
     *
     * @throws SQLException if reconnecting failed
     * @throws RepositoryException
     */
    private void reestablishConnection() throws SQLException, RepositoryException {

        long trials = TRIALS;
        SQLException exception = null;

        // Close the connection (might already have been done)
        close();

        if (block) {
            log.warn("blocking until database connection is up again...");
        }

        // Try to reconnect
        while (trials-- >= 0 || block) {

            // Reset the last caught exception
            exception = null;

            // Sleep for a while to give database a chance
            // to restart before a reconnect is attempted.
            try {
                Thread.sleep(SLEEP_BEFORE_RECONNECT);
            } catch (InterruptedException ignore) {
            }

            // now try to re-establish connection
            try {
                setupConnection();
                isClosed = false;
                break;
            } catch (SQLException e) {
                exception = e;
                close();
            }
        }

        // Rethrow last caught exception (if this is not null, then
        // we know that reconnecting failed and close has been called.
        if (exception != null) {
            throw exception;
        } else if (block) {
            log.warn("database connection is up again!");
        }
    }

    /**
     * Resets the given <code>PreparedStatement</code> by clearing the
     * parameters and warnings contained.
     *
     * @param stmt The <code>PreparedStatement</code> to reset. If
     *             <code>null</code> this method does nothing.
     */
    private void resetStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.clearParameters();
                stmt.clearWarnings();
            } catch (SQLException se) {
                logException("Failed resetting PreparedStatement", se);
            }
        }
    }

    /**
     * Logs an sql exception.
     *
     * @param message the message
     * @param se the exception
     */
    private void logException(String message, SQLException se) {
        message = message == null ? "" : message;
        log.error(message + ", reason: " + se.getMessage() + ", state/code: "
                + se.getSQLState() + "/" + se.getErrorCode());
        log.debug("   dump:", se);
    }

    /**
     * A wrapper for a binary stream that includes the
     * size of the stream.
     *
     */
    public static class StreamWrapper {

        private final InputStream stream;
        private final long size;

        /**
         * Creates a wrapper for the given InputStream that can
         * safely be passed as a parameter to the <code>executeStmt</code>
         * methods in the {@link ConnectionRecoveryManager} class.
         *
         * @param in the InputStream to wrap
         * @param size the size of the input stream
         */
        public StreamWrapper(InputStream in, long size) {
            this.stream = in;
            this.size = size;
        }
    }

}
