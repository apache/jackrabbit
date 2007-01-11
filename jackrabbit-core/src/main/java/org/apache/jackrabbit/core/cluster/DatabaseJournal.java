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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

/**
 * Database-based journal implementation. Stores records inside a database.
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
 * <li><code>schema</code>: </li>
 * </ul>
 * This implementation maintains a database table, containing exactly one record with the
 * last available revision.
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
     * Schema object prefix, bean property.
     */
    protected String schemaObjectPrefix;

    /**
     * User name, bean property.
     */
    private String user;

    /**
     * Password, bean property.
     */
    private String password;

    /**
     * JDBC Connection used.
     */
    private Connection con;

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
     * Bean getter for driver.
     * @return driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Bean setter for driver.
     * @param driver driver
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Bean getter for url.
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Bean setter for url.
     * @param url url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Bean getter for schema.
     * @return schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Bean getter for schema.
     * @param schema schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Bean getter for schema object prefix.
     * @return schema object prefix
     */
    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    /**
     * Bean getter for schema object prefix.
     * @param schemaObjectPrefix schema object prefix
     */
    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        this.schemaObjectPrefix = schemaObjectPrefix.toUpperCase();
    }

    /**
     * Bean getter for user.
     * @return user
     */
    public String getUser() {
        return user;
    }

    /**
     * Bean setter for user.
     * @param user user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Bean getter for password.
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Bean setter for password.
     * @param password password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, RecordProcessor processor, NamespaceResolver resolver)
            throws JournalException {
        
        super.init(id, processor, resolver);

        if (driver == null) {
            String msg = "Driver not specified.";
            throw new JournalException(msg);
        }
        if (url == null) {
            String msg = "Connection URL not specified.";
            throw new JournalException(msg);
        }
        try {
            if (schema == null) {
                schema = getSchemaFromURL(url);
            }
            if (schemaObjectPrefix == null) {
                schemaObjectPrefix = "";
            }
        } catch (IllegalArgumentException e) {
            String msg = "Unable to derive schema from URL: " + e.getMessage();
            throw new JournalException(msg);
        }
        try {
            Class.forName(driver);
            con = DriverManager.getConnection(url, user, password);
            con.setAutoCommit(false);

            checkSchema();
            prepareStatements();
        } catch (Exception e) {
            String msg = "Unable to initialize connection.";
            throw new JournalException(msg, e);
        }
        log.info("DatabaseJournal initialized at URL: " + url);
    }

    /**
     * Derive a schema from a JDBC connection URL. This simply treats the given URL
     * as delimeted by colons and takes the 2nd field.
     *
     * @param url JDBC connection URL
     * @return schema
     * @throws IllegalArgumentException if the JDBC connection URL is invalid
     */
    private String getSchemaFromURL(String url) throws IllegalArgumentException {
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
    public void sync() throws JournalException {
        long oldRevision = getLocalRevision();
        ResultSet rs = null;

        try {
            selectRevisionsStmt.clearParameters();
            selectRevisionsStmt.clearWarnings();
            selectRevisionsStmt.setLong(1, oldRevision);
            selectRevisionsStmt.execute();

            rs = selectRevisionsStmt.getResultSet();
            while (rs.next()) {
                long revision = rs.getLong(1);
                String creator = rs.getString(2);
                if (!creator.equals(id)) {
                    DataInputStream in = new DataInputStream(rs.getBinaryStream(3));
                    try {
                        process(revision, in);
                    } catch (IllegalArgumentException e) {
                        String msg = "Error while processing revision " +
                                revision + ": " + e.getMessage();
                        throw new JournalException(msg);
                    } finally {
                        close(in);
                    }
                } else {
                    log.info("Log entry matches journal id, skipped: " + revision);
                }
                setLocalRevision(revision);
            }
        } catch (SQLException e) {
            String msg = "Unable to iterate over modified records.";
            throw new JournalException(msg, e);
        } finally {
            close(rs);
        }

        long currentRevision = getLocalRevision();
        if (oldRevision < currentRevision) {
            log.info("Sync finished, instance revision is: " + currentRevision);
        }
    }

    /**
     * Process a record.
     *
     * @param revision revision
     * @param dataIn data input
     * @throws JournalException if an error occurs
     */
    private void process(long revision, DataInputStream dataIn) throws JournalException {
        RecordInput in = new RecordInput(dataIn, resolver);

        try {
            process(revision, in);
        } finally {
            in.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected long lockRevision() throws JournalException {
        ResultSet rs = null;
        boolean succeeded = false;

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
            long globalRevision = rs.getLong(1);
            succeeded = true;
            return globalRevision;

        } catch (SQLException e) {
            String msg = "Unable to lock global revision table: " + e.getMessage();
            throw new JournalException(msg);
        } finally {
            close(rs);
            if (!succeeded) {
                rollback(con);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void unlockRevision(boolean successful) {
        if (!successful) {
            rollback(con);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void append(long revision, File record) throws JournalException {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(record));

            try {
                insertRevisionStmt.clearParameters();
                insertRevisionStmt.clearWarnings();
                insertRevisionStmt.setLong(1, revision);
                insertRevisionStmt.setString(2, id);
                insertRevisionStmt.setBinaryStream(3, in, (int) record.length());
                insertRevisionStmt.execute();

                con.commit();

                setLocalRevision(revision);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            String msg = "Unable to open journal log " + record + ": " + e.getMessage();
            throw new JournalException(msg);
        } catch (SQLException e) {
            String msg = "Unable to append revision: "  + revision + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            String msg = "Error while closing connection: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Close some input stream.
     *
     * @param in input stream, may be <code>null</code>.
     */
    private void close(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            String msg = "Error while closing input stream: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Close some statement.
     *
     * @param stmt statement, may be <code>null</code>.
     */
    private void close(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            String msg = "Error while closing statement: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Close some resultset.
     *
     * @param rs resultset, may be <code>null</code>.
     */
    private void close(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            String msg = "Error while closing result set: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Rollback a connection.
     *
     * @param con connection.
     */
    private void rollback(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            String msg = "Error while rolling back connection: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    private void checkSchema() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String tableName = schemaObjectPrefix + "JOURNAL";
        if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase();
        }

        ResultSet rs = metaData.getTables(null, null, tableName, null);
        boolean schemaExists;
        try {
            schemaExists = rs.next();
        } finally {
            rs.close();
        }

        if (!schemaExists) {
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
            Statement stmt = con.createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    // Skip comments and empty lines
                    if (!sql.startsWith("#") && sql.length() > 0) {
                        // replace prefix variable
                        sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
                // commit the changes
                con.commit();
            } finally {
                close(in);
                close(stmt);
            }
        }
    }

    /**
     * Builds and prepares the SQL statements.
     *
     * @throws SQLException if an error occurs
     */
    private void prepareStatements() throws SQLException {
        selectRevisionsStmt = con.prepareStatement(
                "select REVISION_ID, REVISION_CREATOR, REVISION_DATA " +
                "from " + schemaObjectPrefix + "JOURNAL " +
                "where REVISION_ID > ?");
        updateGlobalStmt = con.prepareStatement(
                "update " + schemaObjectPrefix + "GLOBAL_REVISION " +
                "set revision_id = revision_id + 1");
        selectGlobalStmt = con.prepareStatement(
                "select revision_id " +
                "from " + schemaObjectPrefix + "GLOBAL_REVISION");
        insertRevisionStmt = con.prepareStatement(
                "insert into " + schemaObjectPrefix + "JOURNAL" +
                "(REVISION_ID, REVISION_CREATOR, REVISION_DATA) " +
                "values (?,?,?)");
    }
}
