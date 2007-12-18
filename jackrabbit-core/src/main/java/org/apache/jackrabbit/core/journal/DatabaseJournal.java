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
package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database-based journal implementation. Stores records inside a database table named
 * <code>JOURNAL</code>, whereas the table <code>GLOBAL_REVISION</code> contains the
 * highest available revision number. These tables are located inside the schema specified
 * in <code>schemaObjectPrefix</code>.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>revision</code>: the filename where the parent cluster node's revision
 * file should be written to; this is a required property with no default value</li>
 * <li><code>driver</code>: the JDBC driver class name to use; this is a required
 * property with no default value</li>
 * <li><code>url</code>: the JDBC connection url; this is a required property with
 * no default value </li>
 * <li><code>schema</code>: the schema to be used; if not specified, this is the
 * second field inside the JDBC connection url, delimeted by colons</li>
 * <li><code>schemaObjectPrefix</code>: the schema object prefix to be used;
 * defaults to an empty string</li>
 * <li><code>user</code>: username to specify when connecting</li>
 * <li><code>password</code>: password to specify when connecting</li>
 * <li><code>reconnectDelayMs</code>: number of milliseconds to wait before
 * trying to reconnect to the database.
 * </ul>
 */
public class DatabaseJournal extends AbstractJournal {

    /**
     * Schema object prefix.
     */
    private static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    /**
     * Default DDL script name.
     */
    private static final String DEFAULT_DDL_NAME = "default.ddl";

    /**
     * Default journal table name, used to check schema completeness.
     */
    private static final String DEFAULT_JOURNAL_TABLE = "JOURNAL";

    /**
     * Default reconnect delay in milliseconds.
     */
    private static final long DEFAULT_RECONNECT_DELAY_MS = 10000;

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(DatabaseJournal.class);

    /**
     * Driver name, bean property.
     */
    private String driver;

    /**
     * Connection URL, bean property.
     */
    private String url;

    /**
     * Schema name, bean property.
     */
    private String schema;

    /**
     * User name, bean property.
     */
    private String user;

    /**
     * Password, bean property.
     */
    private String password;

    /**
     * Reconnect delay in milliseconds, bean property.
     */
    private long reconnectDelayMs;

    /**
     * JDBC Connection used.
     */
    private Connection connection;

    /**
     * Statement returning all revisions within a range.
     */
    private PreparedStatement selectRevisionsStmt;

    /**
     * Statement updating the global revision.
     */
    private PreparedStatement updateGlobalStmt;

    /**
     * Statement returning the global revision.
     */
    private PreparedStatement selectGlobalStmt;

    /**
     * Statement appending a new record.
     */
    private PreparedStatement insertRevisionStmt;
    
    /**
     * Auto commit level.
     */
    private int lockLevel;

    /**
     * Locked revision.
     */
    private long lockedRevision;

    /**
     * Next time in milliseconds to reattempt connecting to the database.
     */
    private long reconnectTimeMs;

    /**
     * SQL statement returning all revisions within a range.
     */
    protected String selectRevisionsStmtSQL;

    /**
     * SQL statement updating the global revision.
     */
    protected String updateGlobalStmtSQL;

    /**
     * SQL statement returning the global revision.
     */
    protected String selectGlobalStmtSQL;

    /**
     * SQL statement appending a new record.
     */
    protected String insertRevisionStmtSQL;

    /**
     * Schema object prefix, bean property.
     */
    protected String schemaObjectPrefix;

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver)
            throws JournalException {

        super.init(id, resolver);

        // Provide valid defaults for arguments
        if (schemaObjectPrefix == null) {
            schemaObjectPrefix = "";
        }
        if (reconnectDelayMs == 0) {
            reconnectDelayMs = DEFAULT_RECONNECT_DELAY_MS;
        }

        init();

        try {
            connection = getConnection();
            connection.setAutoCommit(true);
            checkSchema();
            buildSQLStatements();
            prepareStatements();
        } catch (Exception e) {
            String msg = "Unable to create connection.";
            throw new JournalException(msg, e);
        }
        log.info("DatabaseJournal initialized.");
    }

    /**
     * Completes initialization of this database journal. Base implementation
     * checks whether the required bean properties <code>driver</code> and
     * <code>url</code> have been specified and optionally deduces a valid
     * schema. Should be overridden by subclasses that use a different way to
     * create a connection and therefore require other arguments.
     *
     * @see #getConnection()
     * @throws JournalException if initialization fails
     */
    protected void init() throws JournalException {
        if (driver == null) {
            String msg = "Driver not specified.";
            throw new JournalException(msg);
        }
        if (url == null) {
            String msg = "Connection URL not specified.";
            throw new JournalException(msg);
        }

        if (schema == null) {
            try {
                schema = getSchemaFromURL(url);
            } catch (IllegalArgumentException e) {
                String msg = "Unable to derive schema from URL: " + e.getMessage();
                throw new JournalException(msg);
            }
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to load JDBC driver class.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Creates a new database connection. This method is called inside
     * {@link #init(String, org.apache.jackrabbit.name.NamespaceResolver)} or
     * when a connection has been dropped and must be reacquired. Base
     * implementation uses <code>java.sql.DriverManager</code> to get the
     * connection. May be overridden by subclasses.
     *
     * @see #init()
     * @return new connection
     * @throws SQLException if an error occurs
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Derive a schema from a JDBC connection URL. This simply treats the given URL
     * as delimeted by colons and takes the 2nd field.
     *
     * @param url JDBC connection URL
     * @return schema
     * @throws IllegalArgumentException if the JDBC connection URL is invalid
     */
    private static String getSchemaFromURL(String url) throws IllegalArgumentException {
        int start = url.indexOf(':');
        if (start != -1) {
            int end = url.indexOf(':', start + 1);
            if (end != -1) {
                return url.substring(start + 1, end);
            }
        }
        throw new IllegalArgumentException(url);
    }

    /**
     * {@inheritDoc}
     */
    protected RecordIterator getRecords(long startRevision)
            throws JournalException {

        try {
            checkConnection();

            selectRevisionsStmt.clearParameters();
            selectRevisionsStmt.clearWarnings();
            selectRevisionsStmt.setLong(1, startRevision);
            selectRevisionsStmt.execute();

            return new DatabaseRecordIterator(
                    selectRevisionsStmt.getResultSet(), getResolver(), getNamePathResolver());
        } catch (SQLException e) {
            close(true);

            String msg = "Unable to return record iterater.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This journal is locked by incrementing the current value in the table
     * named <code>GLOBAL_REVISION</code>, which effectively write-locks this
     * table. The updated value is then saved away and remembered in the
     * appended record, because a save may entail multiple appends (JCR-884).
     */
    protected void doLock() throws JournalException {
        ResultSet rs = null;
        boolean succeeded = false;

        try {
            checkConnection();
            if (lockLevel++ == 0) {
                setAutoCommit(connection, false);
            }
        } catch (SQLException e) {
            close(true);

            String msg = "Unable to set autocommit to false.";
            throw new JournalException(msg, e);
        }

        try {
            updateGlobalStmt.clearParameters();
            updateGlobalStmt.clearWarnings();
            updateGlobalStmt.execute();

            selectGlobalStmt.clearParameters();
            selectGlobalStmt.clearWarnings();
            selectGlobalStmt.execute();

            rs = selectGlobalStmt.getResultSet();
            if (!rs.next()) {
                 throw new JournalException("No revision available.");
            }
            lockedRevision = rs.getLong(1);
            succeeded = true;

        } catch (SQLException e) {
            close(true);

            String msg = "Unable to lock global revision table.";
            throw new JournalException(msg, e);
        } finally {
            close(rs);
            if (!succeeded) {
                doUnlock(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void doUnlock(boolean successful) {
        if (--lockLevel == 0) {
            if (successful) {
                commit(connection);
            } else {
                rollback(connection);
            }
            setAutoCommit(connection, true);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Save away the locked revision inside the newly appended record.
     */
    protected void appending(AppendRecord record) {
        record.setRevision(lockedRevision);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * We have already saved away the revision for this record.
     */
    protected void append(AppendRecord record, InputStream in, int length)
            throws JournalException {

        try {
            checkConnection();

            insertRevisionStmt.clearParameters();
            insertRevisionStmt.clearWarnings();
            insertRevisionStmt.setLong(1, record.getRevision());
            insertRevisionStmt.setString(2, getId());
            insertRevisionStmt.setString(3, record.getProducerId());
            insertRevisionStmt.setBinaryStream(4, in, length);
            insertRevisionStmt.execute();

        } catch (SQLException e) {
            close(true);

            String msg = "Unable to append revision " + lockedRevision + ".";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        close(false);
    }

    /**
     * Close database connections and statements. If closing was due to an
     * error that occurred, calculates the next time a reconnect should
     * be attempted.
     *
     * @param failure whether closing is due to a failure
     */
    private void close(boolean failure) {
        if (failure) {
            reconnectTimeMs = System.currentTimeMillis() + reconnectDelayMs;
        }

        close(selectRevisionsStmt);
        selectRevisionsStmt = null;
        close(updateGlobalStmt);
        updateGlobalStmt = null;
        close(selectGlobalStmt);
        selectGlobalStmt = null;
        close(insertRevisionStmt);
        insertRevisionStmt = null;

        close(connection);
        connection = null;
    }

    /**
     * Set the autocommit flag of a connection. Does nothing if the connection
     * passed is <code>null</code> and logs any exception as warning.
     *
     * @param connection database connection
     * @param autoCommit where to enable or disable autocommit
     */
    private static void setAutoCommit(Connection connection, boolean autoCommit) {
        if (connection != null) {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                String msg = "Unable to set autocommit flag to " + autoCommit;
                log.warn(msg, e);
            }
        }
    }

    /**
     * Commit a connection. Does nothing if the connection passed is
     * <code>null</code> and logs any exception as warning.
     *
     * @param connection connection.
     */
    private static void commit(Connection connection) {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                String msg = "Error while committing connection: " + e.getMessage();
                log.warn(msg);
            }
        }
    }
    
    /**
     * Rollback a connection. Does nothing if the connection passed is
     * <code>null</code> and logs any exception as warning.
     *
     * @param connection connection.
     */
    private static void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                String msg = "Error while rolling back connection: " + e.getMessage();
                log.warn(msg);
            }
        }
    }

    /**
     * Closes the given database connection. Does nothing if the connection
     * passed is <code>null</code> and logs any exception as warning.
     *
     * @param connection database connection
     */
    private static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                String msg = "Error while closing connection: " + e.getMessage();
                log.warn(msg);
            }
        }
    }

    /**
     * Close some input stream.  Does nothing if the input stream
     * passed is <code>null</code> and logs any exception as warning.
     *
     * @param in input stream, may be <code>null</code>.
     */
    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                String msg = "Error while closing input stream: " + e.getMessage();
                log.warn(msg);
            }
        }
    }

    /**
     * Close some statement.  Does nothing if the statement
     * passed is <code>null</code> and logs any exception as warning.
     *
     * @param stmt statement, may be <code>null</code>.
     */
    private static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                String msg = "Error while closing statement: " + e.getMessage();
                log.warn(msg);
            }
        }
    }

    /**
     * Close some resultset.  Does nothing if the result set
     * passed is <code>null</code> and logs any exception as warning.
     *
     * @param rs resultset, may be <code>null</code>.
     */
    private static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                String msg = "Error while closing result set: " + e.getMessage();
                log.warn(msg);
            }
        }
    }

    /**
     * Checks the currently established connection. If the connection no longer
     * exists, waits until at least <code>reconnectTimeMs</code> have passed
     * since the error occurred and recreates the connection.
     */
    private void checkConnection() throws SQLException {
        if (connection == null) {
            long delayMs = reconnectTimeMs - System.currentTimeMillis();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    /* ignore */
                }
            }
            connection = getConnection();
            prepareStatements();
        }
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    private void checkSchema() throws Exception {
        if (!schemaExists(connection.getMetaData())) {
            // read ddl from resources
            InputStream in = DatabaseJournal.class.getResourceAsStream(schema + ".ddl");
            if (in == null) {
                String msg = "No schema-specific DDL found: '" + schema + ".ddl" +
                        "', falling back to '" + DEFAULT_DDL_NAME + "'.";
                log.info(msg);
                in = DatabaseJournal.class.getResourceAsStream(DEFAULT_DDL_NAME);
                if (in == null) {
                    msg = "Unable to load '" + DEFAULT_DDL_NAME + "'.";
                    throw new JournalException(msg);
                }
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = connection.createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    // Skip comments and empty lines
                    if (!sql.startsWith("#") && sql.length() > 0) {
                        // replace prefix variable
                        sql = createSchemaSQL(sql);
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
            } finally {
                close(in);
                close(stmt);
            }
        }
    }

    /**
     * Checks whether the required table(s) exist in the schema. May be
     * overridden by subclasses to allow different table names.
     *
     * @param metaData database meta data
     * @return <code>true</code> if the schema exists
     * @throws SQLException if an SQL error occurs
     */
    protected boolean schemaExists(DatabaseMetaData metaData)
            throws SQLException {

        String tableName = schemaObjectPrefix + DEFAULT_JOURNAL_TABLE;
        if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase();
        }

        ResultSet rs = metaData.getTables(null, null, tableName, null);

        try {
            return rs.next();
        } finally {
            rs.close();
        }
    }

    /**
     * Creates an SQL statement for schema creation by variable substitution.
     *
     * @param sql a SQL string which may contain variables to substitute
     * @return a valid SQL string
     */
    protected String createSchemaSQL(String sql) {
        return Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
    }

    /**
     * Builds the SQL statements. May be overridden by subclasses to allow
     * different table and/or column names.
     */
    protected void buildSQLStatements() {
        selectRevisionsStmtSQL =
                "select REVISION_ID, JOURNAL_ID, PRODUCER_ID, REVISION_DATA " +
                "from " + schemaObjectPrefix + "JOURNAL " +
                "where REVISION_ID > ?";
        updateGlobalStmtSQL =
                "update " + schemaObjectPrefix + "GLOBAL_REVISION " +
                "set revision_id = revision_id + 1";
        selectGlobalStmtSQL =
                "select revision_id " +
                "from " + schemaObjectPrefix + "GLOBAL_REVISION";
        insertRevisionStmtSQL =
                "insert into " + schemaObjectPrefix + "JOURNAL" +
                "(REVISION_ID, JOURNAL_ID, PRODUCER_ID, REVISION_DATA) " +
                "values (?,?,?,?)";
    }

    /**
     * Prepares the SQL statements.
     *
     * @throws SQLException if an error occurs
     */
    private void prepareStatements() throws SQLException {
        selectRevisionsStmt = connection.prepareStatement(selectRevisionsStmtSQL);
        updateGlobalStmt = connection.prepareStatement(updateGlobalStmtSQL);
        selectGlobalStmt = connection.prepareStatement(selectGlobalStmtSQL);
        insertRevisionStmt = connection.prepareStatement(insertRevisionStmtSQL);
    }

    /**
     * Bean getters
     */
    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getSchema() {
        return schema;
    }

    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public long getReconnectDelayMs() {
        return reconnectDelayMs;
    }

    /**
     * Bean setters
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        this.schemaObjectPrefix = schemaObjectPrefix.toUpperCase();
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setReconnectDelayMs(long reconnectDelayMs) {
        this.reconnectDelayMs = reconnectDelayMs;
    }
}
