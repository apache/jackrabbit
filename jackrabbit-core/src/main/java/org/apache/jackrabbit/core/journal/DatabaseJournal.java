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

import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Database-based journal implementation. Stores records inside a database table named
 * <code>JOURNAL</code>, whereas the table <code>GLOBAL_REVISION</code> contains the
 * highest available revision number. These tables are located inside the schema specified
 * in <code>schemaObjectPrefix</code>.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the JDBC driver class name to use; this is a required
 * property with no default value</li>
 * <li><code>url</code>: the JDBC connection url; this is a required property with
 * no default value </li>
 * <li><code>databaseType</code>: the database type to be used; if not specified, this is the
 * second field inside the JDBC connection url, delimited by colons</li>
 * <li><code>schemaObjectPrefix</code>: the schema object prefix to be used;
 * defaults to an empty string</li>
 * <li><code>user</code>: username to specify when connecting</li>
 * <li><code>password</code>: password to specify when connecting</li>
 * <li><code>reconnectDelayMs</code>: number of milliseconds to wait before
 * trying to reconnect to the database.</li>
 * <li><code>janitorEnabled</code>: specifies whether the clean-up thread for the
 * journal table is enabled (default = <code>false</code>)</li>
 * <li><code>janitorSleep</code>: specifies the sleep time of the clean-up thread
 * in seconds (only useful when the clean-up thread is enabled, default = 24 * 60 * 60,
 * which equals 24 hours)</li>
 * <li><code>janitorFirstRunHourOfDay</code>: specifies the hour at which the clean-up
 * thread initiates its first run (default = <code>3</code> which means 3:00 at night)</li>
 * <li><code>schemaCheckEnabled</code>:  whether the schema check during initialization is enabled
 * (default = <code>true</code>)</li>
 * <p>
 * JNDI can be used to get the connection. In this case, use the javax.naming.InitialContext as the driver,
 * and the JNDI name as the URL. If the user and password are configured in the JNDI resource,
 * they should not be configured here. Example JNDI settings:
 * <pre>
 * &lt;param name="driver" value="javax.naming.InitialContext" />
 * &lt;param name="url" value="java:comp/env/jdbc/Test" />
 * </pre> *
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
     * Local revisions table name, used to check schema completeness.
     */
    private static final String LOCAL_REVISIONS_TABLE = "LOCAL_REVISIONS";

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
     * Database type, bean property.
     */
    private String databaseType;

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
     * Statement returning the minimum of the local revisions.
     */
    private PreparedStatement selectMinLocalRevisionStmt;

    /**
     * Statement removing a set of revisions with from the journal table.
     */
    private PreparedStatement cleanRevisionStmt;

    /**
     * Statement returning the local revision of this cluster node.
     */
    private PreparedStatement getLocalRevisionStmt;
    
    /**
     * Statement for inserting the local revision of this cluster node. 
     */
    private PreparedStatement insertLocalRevisionStmt;
    
    /**
     * Statement for updating the local revision of this cluster node. 
     */
    private PreparedStatement updateLocalRevisionStmt;

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
     * Whether the revision table janitor thread is enabled.
     */
    private boolean janitorEnabled = false;

    /**
     * The sleep time of the revision table janitor in seconds, 1 day default.
     */
    private int janitorSleep = 60 * 60 * 24;

    /**
     * Indicates when the next run of the janitor is scheduled.
     * The first run is scheduled by default at 03:00 hours.
     */
    private Calendar janitorNextRun = Calendar.getInstance();
    {
        if (janitorNextRun.get(Calendar.HOUR_OF_DAY) >= 3) {
            janitorNextRun.add(Calendar.DAY_OF_MONTH, 1);
        }
        janitorNextRun.set(Calendar.HOUR_OF_DAY, 3);
        janitorNextRun.set(Calendar.MINUTE, 0);
        janitorNextRun.set(Calendar.SECOND, 0);
        janitorNextRun.set(Calendar.MILLISECOND, 0);
    }

    private Thread janitorThread;

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    /**
     * The instance that manages the local revision.
     */
    private DatabaseRevision databaseRevision;
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
     * SQL statement returning the minimum of the local revisions.
     */
    protected String selectMinLocalRevisionStmtSQL;

    /**
     * SQL statement removing a set of revisions with from the journal table.
     */
    protected String cleanRevisionStmtSQL;
    
    /**
     * SQL statement returning the local revision of this cluster node.
     */
    protected String getLocalRevisionStmtSQL;
    
    /**
     * SQL statement for inserting the local revision of this cluster node. 
     */
    protected String insertLocalRevisionStmtSQL;

    /**
     * SQL statement for updating the local revision of this cluster node. 
     */
    protected String updateLocalRevisionStmtSQL;

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
            setAutoCommit(connection, true);
            if (isSchemaCheckEnabled()) {
                checkSchema();
            }
            // Make sure that the LOCAL_REVISIONS table exists (see JCR-1087)
            if (isSchemaCheckEnabled()) {
                checkLocalRevisionSchema();
            }

            buildSQLStatements();
            prepareStatements();
            initInstanceRevisionAndJanitor();
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
     * database type. Should be overridden by subclasses that use a different way to
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

        if (databaseType == null) {
            try {
                databaseType = getDatabaseTypeFromURL(url);
            } catch (IllegalArgumentException e) {
                String msg = "Unable to derive database type from URL: " + e.getMessage();
                throw new JournalException(msg);
            }
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to load driver class.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Initialize the instance revision manager and the janitor thread.
     *
     * @throws JournalException on error
     */
    protected void initInstanceRevisionAndJanitor() throws Exception {
        databaseRevision = new DatabaseRevision();

        // Get the local file revision from disk (upgrade; see JCR-1087)
        long localFileRevision = 0L;
        if (getRevision() != null) {
            InstanceRevision currentFileRevision = new FileRevision(new File(getRevision()));
            localFileRevision = currentFileRevision.get();
            currentFileRevision.close();
        }

        // Now write the localFileRevision (or 0 if it does not exist) to the LOCAL_REVISIONS
        // table, but only if the LOCAL_REVISIONS table has no entry yet for this cluster node
        long localRevision = databaseRevision.init(localFileRevision);
        log.info("Initialized local revision to " + localRevision);

        // Start the clean-up thread if necessary.
        if (janitorEnabled) {
            janitorThread = new Thread(new RevisionTableJanitor(), "Jackrabbit-ClusterRevisionJanitor");
            janitorThread.setDaemon(true);
            janitorThread.start();
            log.info("Cluster revision janitor thread started; first run scheduled at " + janitorNextRun.getTime());
        } else {
            log.info("Cluster revision janitor thread not started");
        }
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.core.journal.Journal#getInstanceRevision()
     */
    public InstanceRevision getInstanceRevision() throws JournalException {
        return databaseRevision;
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
     * @throws JournalException if the driver could not be loaded
     * @throws SQLException if the connection could not be established
     */
    protected Connection getConnection() throws SQLException, JournalException {
        try {
            return ConnectionFactory.getConnection(driver, url, user, password);
        } catch (RepositoryException e) {
            String msg = "Unable to load driver class.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Derive a database type from a JDBC connection URL. This simply treats the given URL
     * as delimeted by colons and takes the 2nd field.
     *
     * @param url JDBC connection URL
     * @return the database type
     * @throws IllegalArgumentException if the JDBC connection URL is invalid
     */
    private static String getDatabaseTypeFromURL(String url) throws IllegalArgumentException {
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
    public RecordIterator getRecords(long startRevision)
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

            String msg = "Unable to return record iterator.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords() throws JournalException {
        try {
            checkConnection();

            selectRevisionsStmt.clearParameters();
            selectRevisionsStmt.clearWarnings();
            selectRevisionsStmt.setLong(1, Long.MIN_VALUE);
            selectRevisionsStmt.execute();

            return new DatabaseRecordIterator(
                    selectRevisionsStmt.getResultSet(), getResolver(), getNamePathResolver());
        } catch (SQLException e) {
            close(true);

            String msg = "Unable to return record iterator.";
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
        if (janitorThread != null) {
            janitorThread.interrupt();
        }
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
        close(selectMinLocalRevisionStmt);
        selectMinLocalRevisionStmt = null;
        close(cleanRevisionStmt);
        cleanRevisionStmt = null;
        close(getLocalRevisionStmt);
        getLocalRevisionStmt = null;
        close(insertLocalRevisionStmt);
        insertLocalRevisionStmt = null;
        close(updateLocalRevisionStmt);
        updateLocalRevisionStmt = null;
        
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
                // JCR-1013: Setter may fail on a managed connection
                if (connection.getAutoCommit() != autoCommit) {
                    connection.setAutoCommit(autoCommit);
                }
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
    private void checkConnection() throws SQLException, JournalException {
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
        if (!tableExists(connection.getMetaData(), schemaObjectPrefix + DEFAULT_JOURNAL_TABLE)) {            // read ddl from resources
            InputStream in = DatabaseJournal.class.getResourceAsStream(databaseType + ".ddl");
            if (in == null) {
                String msg = "No database-specific DDL found: '" + databaseType + ".ddl"
                    + "', falling back to '" + DEFAULT_DDL_NAME + "'.";
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
     * Checks if the local revision schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    private void checkLocalRevisionSchema() throws Exception {
        if (!tableExists(connection.getMetaData(), schemaObjectPrefix + LOCAL_REVISIONS_TABLE)) {
            log.info("Creating " + schemaObjectPrefix + LOCAL_REVISIONS_TABLE + " table");
            // read ddl from resources
            InputStream in = DatabaseJournal.class.getResourceAsStream(databaseType + ".ddl");
            if (in == null) {
                String msg = "No database-specific DDL found: '" + databaseType + ".ddl" +
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
                    // Skip comments and empty lines, and select only the statement
                    // to create the LOCAL_REVISIONS table.
                    if (!sql.startsWith("#") && sql.length() > 0
                            && sql.indexOf(LOCAL_REVISIONS_TABLE) != -1) {
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
    protected boolean tableExists(DatabaseMetaData metaData, String tableName)
        throws SQLException {

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
            "select REVISION_ID, JOURNAL_ID, PRODUCER_ID, REVISION_DATA from "
            + schemaObjectPrefix + "JOURNAL where REVISION_ID > ? order by REVISION_ID";
        updateGlobalStmtSQL =
            "update " + schemaObjectPrefix + "GLOBAL_REVISION"
            + " set REVISION_ID = REVISION_ID + 1";
        selectGlobalStmtSQL =
            "select REVISION_ID from "
            + schemaObjectPrefix + "GLOBAL_REVISION";
        insertRevisionStmtSQL =
            "insert into " + schemaObjectPrefix + "JOURNAL"
            + " (REVISION_ID, JOURNAL_ID, PRODUCER_ID, REVISION_DATA) "
            + "values (?,?,?,?)";
        selectMinLocalRevisionStmtSQL =
            "select MIN(REVISION_ID) from " + schemaObjectPrefix + "LOCAL_REVISIONS";
        cleanRevisionStmtSQL =
            "delete from " + schemaObjectPrefix + "JOURNAL " + "where REVISION_ID < ?";
        getLocalRevisionStmtSQL =
            "select REVISION_ID from " + schemaObjectPrefix + "LOCAL_REVISIONS "
            + "where JOURNAL_ID = ?";
        insertLocalRevisionStmtSQL =
            "insert into " + schemaObjectPrefix + "LOCAL_REVISIONS "
            + "(REVISION_ID, JOURNAL_ID) values (?,?)";
        updateLocalRevisionStmtSQL =
            "update " + schemaObjectPrefix + "LOCAL_REVISIONS "
            + "set REVISION_ID = ? where JOURNAL_ID = ?";
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
        selectMinLocalRevisionStmt = connection.prepareStatement(selectMinLocalRevisionStmtSQL);
        cleanRevisionStmt = connection.prepareStatement(cleanRevisionStmtSQL);
        getLocalRevisionStmt = connection.prepareStatement(getLocalRevisionStmtSQL);
        insertLocalRevisionStmt = connection.prepareStatement(insertLocalRevisionStmtSQL);
        updateLocalRevisionStmt = connection.prepareStatement(updateLocalRevisionStmtSQL);
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

    /**
     * Get the database type.
     * 
     * @return the database type
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Get the database type.
     * @deprecated
     * This method is deprecated; {@link getDatabaseType} should be used instead.
     * 
     * @return the database type
     */
    public String getSchema() {
        return databaseType;
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

    public boolean getJanitorEnabled() {
        return janitorEnabled;
    }

    public int getJanitorSleep() {
        return janitorSleep;
    }

    public int getJanitorFirstRunHourOfDay() {
        return janitorNextRun.get(Calendar.HOUR_OF_DAY);
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

    /**
     * Set the database type.
     * 
     * @param databaseType the database type
     */
    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * Set the database type.
    * @deprecated
    * This method is deprecated; {@link getDatabaseType} should be used instead.
     * 
     * @param databaseType the database type
     */
    public void setSchema(String databaseType) {
        this.databaseType = databaseType;
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

    public void setJanitorEnabled(boolean enabled) {
        this.janitorEnabled = enabled;
    }

    public void setJanitorSleep(int sleep) {
        this.janitorSleep = sleep;
    }

    public void setJanitorFirstRunHourOfDay(int hourOfDay) {
        janitorNextRun = Calendar.getInstance();
        if (janitorNextRun.get(Calendar.HOUR_OF_DAY) >= hourOfDay) {
            janitorNextRun.add(Calendar.DAY_OF_MONTH, 1);
        }
        janitorNextRun.set(Calendar.HOUR_OF_DAY, hourOfDay);
        janitorNextRun.set(Calendar.MINUTE, 0);
        janitorNextRun.set(Calendar.SECOND, 0);
        janitorNextRun.set(Calendar.MILLISECOND, 0);
    }
   
    /**
     * @return whether the schema check is enabled
     */
    public final boolean isSchemaCheckEnabled() {
        return schemaCheckEnabled;
    }

    /**
     * @param enabled set whether the schema check is enabled
     */
    public final void setSchemaCheckEnabled(boolean enabled) {
        schemaCheckEnabled = enabled;
    }

    /**
     * This class manages the local revision of the cluster node. It
     * persists the local revision in the LOCAL_REVISIONS table in the
     * clustering database.
     */
    public class DatabaseRevision implements InstanceRevision {

        /**
         * The cached local revision of this cluster node.
         */
        private long localRevision;

        /**
         * Indicates whether the init method has been called. 
         */
        private boolean initialized = false;

        /**
         * Checks whether there's a local revision value in the database for this
         * cluster node. If not, it writes the given default revision to the database.
         *
         * @param revision the default value for the local revision counter
         * @return the local revision
         * @throws JournalException on error
         */
        protected synchronized long init(long revision) throws JournalException {
            try {
                // Check whether the connection is available
                checkConnection();

                // Check whether there is an entry in the database.
                getLocalRevisionStmt.clearParameters();
                getLocalRevisionStmt.clearWarnings();
                getLocalRevisionStmt.setString(1, getId());
                getLocalRevisionStmt.execute();
                ResultSet rs = getLocalRevisionStmt.getResultSet();
                boolean exists = rs.next();
                if (exists) {
                    revision = rs.getLong(1);
                }
                rs.close();

                // Insert the given revision in the database
                if (!exists) {
                    insertLocalRevisionStmt.clearParameters();
                    insertLocalRevisionStmt.clearWarnings();
                    insertLocalRevisionStmt.setLong(1, revision);
                    insertLocalRevisionStmt.setString(2, getId());
                    insertLocalRevisionStmt.execute();
                }

                // Set the cached local revision and return
                localRevision = revision;
                initialized = true;
                return revision;

            } catch (SQLException e) {
                log.warn("Failed to initialize local revision.", e);
                DatabaseJournal.this.close(true);
                throw new JournalException("Failed to initialize local revision", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public synchronized long get() {
            if (!initialized) {
                throw new IllegalStateException("instance has not yet been initialized");
            }
            return localRevision;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void set(long localRevision) throws JournalException {

            if (!initialized) {
                throw new IllegalStateException("instance has not yet been initialized");
            }

            // Update the cached value and the table with local revisions.
            try {
                // Check whether the connection is available
                checkConnection();
                updateLocalRevisionStmt.clearParameters();
                updateLocalRevisionStmt.clearWarnings();
                updateLocalRevisionStmt.setLong(1, localRevision);
                updateLocalRevisionStmt.setString(2, getId());
                updateLocalRevisionStmt.execute();
                this.localRevision = localRevision;
            } catch (SQLException e) {
                log.warn("Failed to update local revision.", e);
                DatabaseJournal.this.close(true);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public synchronized void close() {
            // Do nothing: The statements are closed in DatabaseJournal.close()
        }
    }

    /**
     * Class for maintaining the revision table. This is only useful if all
     * JR information except the search index is in the database (i.e., node types
     * etc). In that case, revision data can safely be thrown away from the JOURNAL table.
     */
    public class RevisionTableJanitor implements Runnable {

        /**
         * {@inheritDoc}
         */
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("Next clean-up run scheduled at " + janitorNextRun.getTime());
                    long sleepTime = janitorNextRun.getTimeInMillis() - System.currentTimeMillis();
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    cleanUpOldRevisions();
                    janitorNextRun.add(Calendar.SECOND, janitorSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Interrupted: stopping clean-up task.");
        }
        
        /**
         * Cleans old revisions from the clustering table.
         */
        protected void cleanUpOldRevisions() {
            try {
                long minRevision = 0;

                // Check whether the connection is available
                checkConnection();

                // Find the minimal local revision
                selectMinLocalRevisionStmt.clearParameters();
                selectMinLocalRevisionStmt.clearWarnings();
                selectMinLocalRevisionStmt.execute();
                ResultSet rs = selectMinLocalRevisionStmt.getResultSet();
                boolean cleanUp = rs.next();
                if (cleanUp) {
                    minRevision = rs.getLong(1);
                }
                rs.close();

                // Clean up if necessary:
                if (cleanUp) {
                    cleanRevisionStmt.clearParameters();
                    cleanRevisionStmt.clearWarnings();
                    cleanRevisionStmt.setLong(1, minRevision);
                    cleanRevisionStmt.execute();
                    log.info("Cleaned old revisions up to revision " + minRevision + ".");
                }

            } catch (Exception e) {
                log.warn("Failed to clean up old revisions.", e);
                close(true);
            }
        }
    }
}
