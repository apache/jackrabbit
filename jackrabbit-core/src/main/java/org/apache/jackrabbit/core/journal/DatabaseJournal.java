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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.core.util.db.StreamWrapper;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Database-based journal implementation. Stores records inside a database table named
 * <code>JOURNAL</code>, whereas the table <code>GLOBAL_REVISION</code> contains the
 * highest available revision number. These tables are located inside the schema specified
 * in <code>schemaObjectPrefix</code>.
 * <p>
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
 * </ul>
 * <p>
 * JNDI can be used to get the connection. In this case, use the javax.naming.InitialContext as the driver,
 * and the JNDI name as the URL. If the user and password are configured in the JNDI resource,
 * they should not be configured here. Example JNDI settings:
 * <pre>
 * &lt;param name="driver" value="javax.naming.InitialContext" /&gt;
 * &lt;param name="url" value="java:comp/env/jdbc/Test" /&gt;
 * </pre>
 */
public class DatabaseJournal extends AbstractJournal implements DatabaseAware {

    /**
     * Default journal table name, used to check schema completeness.
     */
    private static final String DEFAULT_JOURNAL_TABLE = "JOURNAL";

    /**
     * Local revisions table name, used to check schema completeness.
     */
    private static final String LOCAL_REVISIONS_TABLE = "LOCAL_REVISIONS";

    /**
     * Logger.
     */
    static Logger log = LoggerFactory.getLogger(DatabaseJournal.class);

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
     * DataSource logical name, bean property.
     */
    private String dataSourceName;

    /**
     * The connection helper
     */
    ConnectionHelper conHelper;

    /**
     * Auto commit level.
     */
    private int lockLevel;

    /**
     * Locked revision.
     */
    private long lockedRevision;

    /**
     * Whether the revision table janitor thread is enabled.
     */
    private boolean janitorEnabled = false;

    /**
     * The sleep time of the revision table janitor in seconds, 1 day default.
     */
    int janitorSleep = 60 * 60 * 24;

    /**
     * Indicates when the next run of the janitor is scheduled.
     * The first run is scheduled by default at 03:00 hours.
     */
    Calendar janitorNextRun = Calendar.getInstance();

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
     * The repositories {@link ConnectionFactory}.
     */
    private ConnectionFactory connectionFactory;

    public DatabaseJournal() {
        databaseType = "default";
        schemaObjectPrefix = "";
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectionFactory(ConnectionFactory connnectionFactory) {
        this.connectionFactory = connnectionFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver)
            throws JournalException {

        super.init(id, resolver);

        init();

        try {
            conHelper = createConnectionHelper(getDataSource());

            // make sure schemaObjectPrefix consists of legal name characters only
            schemaObjectPrefix = conHelper.prepareDbIdentifier(schemaObjectPrefix);

            // check if schema objects exist and create them if necessary
            if (isSchemaCheckEnabled()) {
                createCheckSchemaOperation().run();
            }

            // Make sure that the LOCAL_REVISIONS table exists (see JCR-1087)
            if (isSchemaCheckEnabled()) {
                checkLocalRevisionSchema();
            }

            buildSQLStatements();
            initInstanceRevisionAndJanitor();
        } catch (Exception e) {
            String msg = "Unable to create connection.";
            throw new JournalException(msg, e);
        }
        log.info("DatabaseJournal initialized.");
    }

    private DataSource getDataSource() throws Exception {
        if (getDataSourceName() == null || "".equals(getDataSourceName())) {
            return connectionFactory.getDataSource(getDriver(), getUrl(), getUser(), getPassword());
        } else {
            return connectionFactory.getDataSource(dataSourceName);
        }
    }

    /**
     * This method is called from the {@link #init(String, NamespaceResolver)} method of this class and
     * returns a {@link ConnectionHelper} instance which is assigned to the {@code conHelper} field.
     * Subclasses may override it to return a specialized connection helper.
     *
     * @param dataSrc the {@link DataSource} of this persistence manager
     * @return a {@link ConnectionHelper}
     * @throws Exception on error
     */
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        return new ConnectionHelper(dataSrc, false);
    }

    /**
     * This method is called from {@link #init(String, NamespaceResolver)} after the
     * {@link #createConnectionHelper(DataSource)} method, and returns a default {@link CheckSchemaOperation}.
     * Subclasses can override this implementation to get a customized implementation.
     *
     * @return a new {@link CheckSchemaOperation} instance
     */
    protected CheckSchemaOperation createCheckSchemaOperation() {
        InputStream in = DatabaseJournal.class.getResourceAsStream(databaseType + ".ddl");
        return new CheckSchemaOperation(conHelper, in, schemaObjectPrefix + DEFAULT_JOURNAL_TABLE).addVariableReplacement(
            CheckSchemaOperation.SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
    }

    /**
     * Completes initialization of this database journal. Base implementation
     * checks whether the required bean properties <code>driver</code> and
     * <code>url</code> have been specified and optionally deduces a valid
     * database type. Should be overridden by subclasses that use a different way to
     * create a connection and therefore require other arguments.
     *
     * @throws JournalException if initialization fails
     */
    protected void init() throws JournalException {
        if (driver == null && dataSourceName == null) {
            String msg = "Driver not specified.";
            throw new JournalException(msg);
        }
        if (url == null && dataSourceName == null) {
            String msg = "Connection URL not specified.";
            throw new JournalException(msg);
        }
        if (dataSourceName != null) {
            try {
                String configuredDatabaseType = connectionFactory.getDataBaseType(dataSourceName);
                if (DatabaseJournal.class.getResourceAsStream(configuredDatabaseType + ".ddl") != null) {
                    setDatabaseType(configuredDatabaseType);
                }
            } catch (RepositoryException e) {
                throw new JournalException("failed to get database type", e);
            }
        }
        if (databaseType == null) {
            try {
                databaseType = getDatabaseTypeFromURL(url);
            } catch (IllegalArgumentException e) {
                String msg = "Unable to derive database type from URL: " + e.getMessage();
                throw new JournalException(msg);
            }
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
            InstanceRevision currentFileRevision = new FileRevision(new File(getRevision()), true);
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
    public RecordIterator getRecords(long startRevision) throws JournalException {
        try {
            return new DatabaseRecordIterator(conHelper.exec(selectRevisionsStmtSQL, new Object[]{new Long(
                    startRevision)}, false, 0), getResolver(), getNamePathResolver());
        } catch (SQLException e) {
            throw new JournalException("Unable to return record iterator.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords() throws JournalException {
        try {
            return new DatabaseRecordIterator(conHelper.exec(selectRevisionsStmtSQL, new Object[]{new Long(
                    Long.MIN_VALUE)}, false, 0), getResolver(), getNamePathResolver());
        } catch (SQLException e) {
            throw new JournalException("Unable to return record iterator.", e);
        }
    }

    /**
     * Synchronize contents from journal. May be overridden by subclasses.
     * Do the initial sync in batchMode, since some databases (PSQL) when
     * not in transactional mode, load all results in memory which causes
     * out of memory. See JCR-2832
     *
     * @param startRevision start point (exclusive)
     * @param startup indicates if the cluster node is syncing on startup 
     *        or does a normal sync.
     * @throws JournalException if an error occurs
     */
    @Override
    protected void doSync(long startRevision, boolean startup) throws JournalException {
        if (!startup) {
            // if the cluster node is not starting do a normal sync
            doSync(startRevision);
        } else {
            try {
                startBatch();
                try {
                    doSync(startRevision);
                } finally {
                    endBatch(true);
                }
            } catch (SQLException e) {
                throw new JournalException("Couldn't sync the cluster node", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This journal is locked by incrementing the current value in the table
     * named <code>GLOBAL_REVISION</code>, which effectively write-locks this
     * table. The updated value is then saved away and remembered in the
     * appended record, because a save may entail multiple appends (JCR-884).
     */
    protected void doLock() throws JournalException {
        ResultSet rs = null;
        boolean succeeded = false;

        try {
            startBatch();
        } catch (SQLException e) {
            throw new JournalException("Unable to set autocommit to false.", e);
        }

        try {
            conHelper.exec(updateGlobalStmtSQL);
            rs = conHelper.exec(selectGlobalStmtSQL, null, false, 0);
            if (!rs.next()) {
                 throw new JournalException("No revision available.");
            }
            lockedRevision = rs.getLong(1);
            succeeded = true;
        } catch (SQLException e) {
            throw new JournalException("Unable to lock global revision table.", e);
        } finally {
            DbUtility.close(rs);
            if (!succeeded) {
                log.debug("doLock.doUnlock(false)");
                doUnlock(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void doUnlock(boolean successful) {
        endBatch(successful);
    }

    private void startBatch() throws SQLException {
        if (lockLevel++ == 0) {
            conHelper.startBatch();
        }
    }

    private void endBatch(boolean successful) {
        if (--lockLevel == 0) {
            try {
                conHelper.endBatch(successful);
            } catch (SQLException e) {
                log.error("failed to end batch", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Save away the locked revision inside the newly appended record.
     */
    protected void appending(AppendRecord record) {
        record.setRevision(lockedRevision);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We have already saved away the revision for this record.
     */
    protected void append(AppendRecord record, InputStream in, int length)
            throws JournalException {

        try {
            conHelper.exec(insertRevisionStmtSQL, record.getRevision(), getId(), record.getProducerId(),
                new StreamWrapper(in, length));

        } catch (SQLException e) {
            String msg = "Unable to append revision " + lockedRevision + ".";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        if (janitorThread != null) {
            janitorThread.interrupt();
        }
    }

    /**
     * Checks if the local revision schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    private void checkLocalRevisionSchema() throws Exception {
        InputStream localRevisionDDLStream = null;
        InputStream in = DatabaseJournal.class.getResourceAsStream(databaseType + ".ddl");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String sql = reader.readLine();
            while (sql != null) {
                // Skip comments and empty lines, and select only the statement to create the LOCAL_REVISIONS
                // table.
                if (!sql.startsWith("#") && sql.length() > 0 && sql.indexOf(LOCAL_REVISIONS_TABLE) != -1) {
                    localRevisionDDLStream = new ByteArrayInputStream(sql.getBytes());
                    break;
                }
                // read next sql stmt
                sql = reader.readLine();
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
        // Run the schema check for the single table
        new CheckSchemaOperation(conHelper, localRevisionDDLStream, schemaObjectPrefix
                + LOCAL_REVISIONS_TABLE).addVariableReplacement(
            CheckSchemaOperation.SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix).run();
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
     * This method is deprecated; {@link #getDatabaseType} should be used instead.
     *
     * @return the database type
     */
    @Deprecated
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
    * This method is deprecated; {@link #getDatabaseType} should be used instead.
     *
     * @param databaseType the database type
     */
    @Deprecated
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

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
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
            ResultSet rs = null;
            try {
                // Check whether there is an entry in the database.
                rs = conHelper.exec(getLocalRevisionStmtSQL, new Object[]{getId()}, false, 0);
                boolean exists = rs.next();
                if (exists) {
                    revision = rs.getLong(1);
                }

                // Insert the given revision in the database
                if (!exists) {
                    conHelper.exec(insertLocalRevisionStmtSQL, revision, getId());
                }

                // Set the cached local revision and return
                localRevision = revision;
                initialized = true;
                return revision;

            } catch (SQLException e) {
                log.warn("Failed to initialize local revision.", e);
                throw new JournalException("Failed to initialize local revision", e);
            } finally {
                DbUtility.close(rs);
            }
        }

        public synchronized long get() {
            if (!initialized) {
                throw new IllegalStateException("instance has not yet been initialized");
            }
            return localRevision;
        }

        public synchronized void set(long localRevision) throws JournalException {

            if (!initialized) {
                throw new IllegalStateException("instance has not yet been initialized");
            }

            // Update the cached value and the table with local revisions.
            try {
                conHelper.exec(updateLocalRevisionStmtSQL, localRevision, getId());
                this.localRevision = localRevision;
            } catch (SQLException e) {
                log.warn("Failed to update local revision.", e);
                throw new JournalException("Failed to update local revision.", e);
            }
        }

        public void close() {
            // nothing to do
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
            ResultSet rs = null;
            try {
                long minRevision = 0;
                rs = conHelper.exec(selectMinLocalRevisionStmtSQL, null, false, 0);
                boolean cleanUp = rs.next();
                if (cleanUp) {
                    minRevision = rs.getLong(1);
                }

                // Clean up if necessary:
                if (cleanUp) {
                    conHelper.exec(cleanRevisionStmtSQL, minRevision);
                    log.info("Cleaned old revisions up to revision " + minRevision + ".");
                }

            } catch (Exception e) {
                log.warn("Failed to clean up old revisions.", e);
            } finally {
                DbUtility.close(rs);
            }
        }
    }
}
