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
package org.apache.jackrabbit.core.persistence.db;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract base class for database persistence managers. This class
 * contains common functionality for database persistence manager subclasses
 * that normally differ only in the way the database connection is acquired.
 * Subclasses should override the {@link #getConnection()} method to return
 * the configured database connection.
 * <p>
 * See the {@link SimpleDbPersistenceManager} for a detailed description
 * of the available configuration options and database behaviour.
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public abstract class DatabasePersistenceManager extends AbstractPersistenceManager {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DatabasePersistenceManager.class);

    protected static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    protected boolean initialized;

    protected String schema;
    protected String schemaObjectPrefix;

    protected boolean externalBLOBs;

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    // initial size of buffer used to serialize objects
    protected static final int INITIAL_BUFFER_SIZE = 1024;

    // jdbc connection
    protected Connection con;

    // internal flag governing whether an automatic reconnect should be
    // attempted after a SQLException had been encountered
    protected boolean autoReconnect = true;
    // time to sleep in ms before a reconnect is attempted
    protected static final int SLEEP_BEFORE_RECONNECT = 10000;

    // the map of prepared statements (key: sql stmt, value: prepared stmt)
    private Map<String, PreparedStatement> preparedStatements = new HashMap<String, PreparedStatement>();

    // SQL statements for NodeState management
    protected String nodeStateInsertSQL;
    protected String nodeStateUpdateSQL;
    protected String nodeStateSelectSQL;
    protected String nodeStateSelectExistSQL;
    protected String nodeStateDeleteSQL;

    // SQL statements for PropertyState management
    protected String propertyStateInsertSQL;
    protected String propertyStateUpdateSQL;
    protected String propertyStateSelectSQL;
    protected String propertyStateSelectExistSQL;
    protected String propertyStateDeleteSQL;

    // SQL statements for NodeReference management
    protected String nodeReferenceInsertSQL;
    protected String nodeReferenceUpdateSQL;
    protected String nodeReferenceSelectSQL;
    protected String nodeReferenceSelectExistSQL;
    protected String nodeReferenceDeleteSQL;

    // SQL statements for BLOB management
    // (if <code>externalBLOBs==false</code>)
    protected String blobInsertSQL;
    protected String blobUpdateSQL;
    protected String blobSelectSQL;
    protected String blobSelectExistSQL;
    protected String blobDeleteSQL;



    /**
     * file system where BLOB data is stored
     * (if <code>externalBLOBs==true</code>)
     */
    protected FileSystem blobFS;
    /**
     * BLOBStore that manages BLOB data in the file system
     * (if <code>externalBLOBs==true</code>)
     */
    protected BLOBStore blobStore;

    /**
     * Creates a new <code>DatabasePersistenceManager</code> instance.
     */
    public DatabasePersistenceManager() {
        schema = "default";
        schemaObjectPrefix = "";
        externalBLOBs = true;
        initialized = false;
    }

    //----------------------------------------------------< setters & getters >

    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        // make sure prefix is all uppercase
        this.schemaObjectPrefix = schemaObjectPrefix.toUpperCase();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isExternalBLOBs() {
        return externalBLOBs;
    }

    public void setExternalBLOBs(boolean externalBLOBs) {
        this.externalBLOBs = externalBLOBs;
    }

    public void setExternalBLOBs(String externalBLOBs) {
        this.externalBLOBs = Boolean.valueOf(externalBLOBs).booleanValue();
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

    //---------------------------------------------------< PersistenceManager >
    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        // setup jdbc connection
        initConnection();

        DatabaseMetaData meta = con.getMetaData();
        try {
            log.info("Database: " + meta.getDatabaseProductName() + " / " + meta.getDatabaseProductVersion());
            log.info("Driver: " + meta.getDriverName() + " / " + meta.getDriverVersion());
        } catch (SQLException e) {
            log.warn("Can not retrieve database and driver name / version", e);
        }

        // make sure schemaObjectPrefix consists of legal name characters only
        prepareSchemaObjectPrefix();

        // check if schema objects exist and create them if necessary
        if (isSchemaCheckEnabled()) {
            checkSchema();
        }

        // build sql statements
        buildSQLStatements();

        // prepare statements
        initPreparedStatements();

        if (externalBLOBs) {
            /**
             * store BLOBs in local file system in a sub directory
             * of the workspace home directory
             */
            LocalFileSystem blobFS = new LocalFileSystem();
            blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
            blobFS.init();
            this.blobFS = blobFS;
            blobStore = new FileSystemBLOBStore(blobFS);
        } else {
            /**
             * store BLOBs in db
             */
            blobStore = new DbBLOBStore();
        }

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close shared prepared statements
            for (PreparedStatement ps : preparedStatements.values()) {
                closeStatement(ps);
            }
            preparedStatements.clear();

            if (externalBLOBs) {
                // close BLOB file system
                blobFS.close();
                blobFS = null;
            }
            blobStore = null;

            // close jdbc connection
            closeConnection(con);

        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(ChangeLog changeLog)
            throws ItemStateException {
        // temporarily disable automatic reconnect feature
        // since the changes need to be persisted atomically
        autoReconnect = false;
        try {
            ItemStateException ise = null;
            // number of attempts to store the changes
            int trials = 2;
            while (trials > 0) {
                try {
                    super.store(changeLog);
                    break;
                } catch (ItemStateException e) {
                    // catch exception and fall through...
                    ise = e;
                }

                if (ise != null && ise.getCause() instanceof SQLException) {
                    // a SQLException has been thrown
                    if (--trials > 0) {
                        // try to reconnect
                        log.warn("storing changes failed, about to reconnect...", ise.getCause());

                        // try to reconnect
                        if (reestablishConnection()) {
                            // now let's give it another try
                            ise = null;
                            continue;
                        } else {
                            // reconnect failed, proceed with error processing
                            break;
                        }
                    }
                } else {
                    // a non-SQLException has been thrown,
                    // proceed with error processing
                    break;
                }
            }

            if (ise == null) {
                // storing the changes succeeded, now commit the changes
                try {
                    con.commit();
                } catch (SQLException e) {
                    String msg = "committing change log failed";
                    log.error(msg, e);
                    throw new ItemStateException(msg, e);
                }
            } else {
                // storing the changes failed, rollback changes
                try {
                    con.rollback();
                } catch (SQLException e) {
                    String msg = "rollback of change log failed";
                    log.error(msg, e);
                }
                // re-throw original exception
                throw ise;
            }
        } finally {
            // re-enable automatic reconnect feature
            autoReconnect = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (nodeStateSelectSQL) {
            ResultSet rs = null;
            InputStream in = null;
            try {
                Statement stmt = executeStmt(nodeStateSelectSQL, new Object[]{id.toString()});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new NoSuchItemStateException(id.toString());
                }

                in = rs.getBinaryStream(1);
                NodeState state = createNew(id);
                Serializer.deserialize(state, in);

                return state;
            } catch (Exception e) {
                if (e instanceof NoSuchItemStateException) {
                    throw (NoSuchItemStateException) e;
                }
                String msg = "failed to read node state: " + id;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                IOUtils.closeQuietly(in);
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (propertyStateSelectSQL) {
            ResultSet rs = null;
            InputStream in = null;
            try {
                Statement stmt = executeStmt(propertyStateSelectSQL, new Object[]{id.toString()});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new NoSuchItemStateException(id.toString());
                }

                in = rs.getBinaryStream(1);

                if (!externalBLOBs) {
                    // JCR-1532: pre-fetch/buffer stream data
                    ByteArrayInputStream bain = new ByteArrayInputStream(
                            IOUtils.toByteArray(in));
                    IOUtils.closeQuietly(in);
                    in = bain;
                }

                PropertyState state = createNew(id);
                Serializer.deserialize(state, in, blobStore);

                return state;
            } catch (Exception e) {
                if (e instanceof NoSuchItemStateException) {
                    throw (NoSuchItemStateException) e;
                }
                String msg = "failed to read property state: " + id;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                IOUtils.closeQuietly(in);
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be synchronized the shared
     * statements would have to be synchronized.
     */
    @Override
    public synchronized void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        //boolean update = exists(state.getId());
        String sql = (update) ? nodeStateUpdateSQL : nodeStateInsertSQL;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize node state
            Serializer.serialize(state, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(sql, new Object[]{out.toByteArray(), state.getNodeId().toString()});

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write node state: " + state.getNodeId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be synchronized the shared
     * statements would have to be synchronized.
     */
    @Override
    public synchronized void store(PropertyState state)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        //boolean update = exists(state.getId());
        String sql = (update) ? propertyStateUpdateSQL : propertyStateInsertSQL;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize property state
            Serializer.serialize(state, out, blobStore);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(sql, new Object[]{out.toByteArray(), state.getPropertyId().toString()});

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write property state: " + state.getPropertyId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy(NodeState state)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(nodeStateDeleteSQL, new Object[]{state.getNodeId().toString()});
        } catch (Exception e) {
            String msg = "failed to delete node state: " + state.getNodeId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy(PropertyState state)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // make sure binary values (BLOBs) are properly removed
        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    if (val.getType() == PropertyType.BINARY) {
                        val.deleteBinaryResource();
                        // also remove from BLOBStore
                        String blobId = blobStore.createId(state.getPropertyId(), i);
                        try {
                            blobStore.remove(blobId);
                        } catch (Exception e) {
                            log.warn("failed to remove from BLOBStore: " + blobId, e);
                        }
                    }
                }
            }
        }

        try {
            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(propertyStateDeleteSQL, new Object[]{state.getPropertyId().toString()});
        } catch (Exception e) {
            String msg = "failed to delete property state: " + state.getPropertyId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences loadReferencesTo(NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (nodeReferenceSelectSQL) {
            ResultSet rs = null;
            InputStream in = null;
            try {
                Statement stmt = executeStmt(
                        nodeReferenceSelectSQL, new Object[]{targetId.toString()});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new NoSuchItemStateException(targetId.toString());
                }

                in = rs.getBinaryStream(1);
                NodeReferences refs = new NodeReferences(targetId);
                Serializer.deserialize(refs, in);

                return refs;
            } catch (Exception e) {
                if (e instanceof NoSuchItemStateException) {
                    throw (NoSuchItemStateException) e;
                }
                String msg = "failed to read node references: " + targetId;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                IOUtils.closeQuietly(in);
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be synchronized the shared
     * statements would have to be synchronized.
     */
    @Override
    public synchronized void store(NodeReferences refs)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = existsReferencesTo(refs.getTargetId());
        String sql = (update) ? nodeReferenceUpdateSQL : nodeReferenceInsertSQL;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            Serializer.serialize(refs, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(sql, new Object[]{out.toByteArray(), refs.getTargetId().toString()});

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write " + refs;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy(NodeReferences refs)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the sql statement
            executeStmt(nodeReferenceDeleteSQL, new Object[]{refs.getTargetId().toString()});
        } catch (Exception e) {
            String msg = "failed to delete " + refs;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(NodeId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (nodeStateSelectExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(nodeStateSelectExistSQL, new Object[]{id.toString()});
                rs = stmt.getResultSet();

                // a node state exists if the result has at least one entry
                return rs.next();
            } catch (Exception e) {
                String msg = "failed to check existence of node state: " + id;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(PropertyId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (propertyStateSelectExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        propertyStateSelectExistSQL, new Object[]{id.toString()});
                rs = stmt.getResultSet();

                // a property state exists if the result has at least one entry
                return rs.next();
            } catch (Exception e) {
                String msg = "failed to check existence of property state: " + id;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean existsReferencesTo(NodeId targetId) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        synchronized (nodeReferenceSelectExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        nodeReferenceSelectExistSQL, new Object[]{targetId.toString()});
                rs = stmt.getResultSet();

                // a reference exists if the result has at least one entry
                return rs.next();
            } catch (Exception e) {
                String msg = "failed to check existence of node references: "
                        + targetId;
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    //----------------------------------< misc. helper methods & overridables >

    /**
     * Initializes the database connection used by this persistence manager.
     * <p>
     * Subclasses should normally override the {@link #getConnection()}
     * method instead of this one. The default implementation calls
     * {@link #getConnection()} to get the database connection and disables
     * the autocommit feature.
     *
     * @throws Exception if an error occurs
     */
    protected void initConnection() throws Exception {
        con = getConnection();
        // JCR-1013: Setter may fail unnecessarily on a managed connection
        if (con.getAutoCommit()) {
            con.setAutoCommit(false);
        }
    }

    /**
     * Abstract factory method for creating a new database connection. This
     * method is called by {@link #init(PMContext)} when the persistence
     * manager is started. The returned connection should come with the default
     * JDBC settings, as the {@link #init(PMContext)} method will explicitly
     * set the <code>autoCommit</code> and other properties as needed.
     * <p>
     * Note that the returned database connection is kept during the entire
     * lifetime of the persistence manager, after which it is closed by
     * {@link #close()} using the {@link #closeConnection(Connection)} method.
     *
     * @return new connection
     * @throws Exception if an error occurs
     */
    protected Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("Override in a subclass!");
    }

    /**
     * Closes the given database connection. This method is called by
     * {@link #close()} to close the connection acquired using
     * {@link #getConnection()} when the persistence manager was started.
     * <p>
     * The default implementation just calls the {@link Connection#close()}
     * method of the given connection, but subclasses can override this
     * method to provide more extensive database and connection cleanup.
     *
     * @param connection database connection
     * @throws Exception if an error occurs
     */
    protected void closeConnection(Connection connection) throws Exception {
        connection.close();
    }

    /**
     * Re-establishes the database connection. This method is called by
     * {@link #store(ChangeLog)} and {@link #executeStmt(String, Object[])}
     * after a <code>SQLException</code> had been encountered.
     * @return true if the connection could be successfully re-established,
     *         false otherwise.
     */
    protected synchronized boolean reestablishConnection() {
        // in any case try to shut down current connection
        // gracefully in order to avoid potential memory leaks

        // close shared prepared statements
        for (Iterator<PreparedStatement> it = preparedStatements.values().iterator(); it.hasNext();) {
            PreparedStatement stmt = it.next();
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException se) {
                    // ignored, see JCR-765
                }
            }
        }
        try {
            closeConnection(con);
        } catch (Exception ignore) {
        }

        // sleep for a while to give database a chance
        // to restart before a reconnect is attempted

        try {
            Thread.sleep(SLEEP_BEFORE_RECONNECT);
        } catch (InterruptedException ignore) {
        }

        // now try to re-establish connection

        try {
            initConnection();
            initPreparedStatements();
            return true;
        } catch (Exception e) {
            log.error("failed to re-establish connection", e);
            // reconnect failed
            return false;
        }
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     * If a <code>SQLException</code> is encountered and
     * <code>autoReconnect==true</code> <i>one</i> attempt is made to re-establish
     * the database connection and re-execute the statement.
     *
     * @param sql    statement to execute
     * @param params parameters to set
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException if an error occurs
     */
    protected Statement executeStmt(String sql, Object[] params)
            throws SQLException {
        int trials = autoReconnect ? 2 : 1;
        while (true) {
            PreparedStatement stmt = (PreparedStatement) preparedStatements.get(sql);
            try {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] instanceof SizedInputStream) {
                        SizedInputStream in = (SizedInputStream) params[i];
                        stmt.setBinaryStream(i + 1, in, (int) in.getSize());
                    } else {
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                stmt.execute();
                resetStatement(stmt);
                return stmt;
            } catch (SQLException se) {
                if (--trials == 0) {
                    // no more trials, re-throw
                    throw se;
                }
                log.warn("execute failed, about to reconnect... {}", se.getMessage());

                // try to reconnect
                if (reestablishConnection()) {
                    // reconnect succeeded; check whether it's possible to
                    // re-execute the prepared stmt with the given parameters
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] instanceof SizedInputStream) {
                            SizedInputStream in = (SizedInputStream) params[i];
                            if (in.isConsumed()) {
                                // we're unable to re-execute the prepared stmt
                                // since an InputStream paramater has already
                                // been 'consumed';
                                // re-throw previous SQLException
                                throw se;
                            }
                        }
                    }

                    // try again to execute the statement
                    continue;
                } else {
                    // reconnect failed, re-throw previous SQLException
                    throw se;
                }
            }
        }
    }

    /**
     * Resets the given <code>PreparedStatement</code> by clearing the parameters
     * and warnings contained.
     * <p>
     * NOTE: This method MUST be called in a synchronized context as neither
     * this method nor the <code>PreparedStatement</code> instance on which it
     * operates are thread safe.
     *
     * @param stmt The <code>PreparedStatement</code> to reset. If
     *             <code>null</code> this method does nothing.
     */
    protected void resetStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.clearParameters();
                stmt.clearWarnings();
            } catch (SQLException se) {
                logException("failed resetting PreparedStatement", se);
            }
        }
    }

    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException se) {
                logException("failed closing ResultSet", se);
            }
        }
    }

    protected void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException se) {
                logException("failed closing Statement", se);
            }
        }
    }

    protected void logException(String message, SQLException se) {
        if (message != null) {
            log.error(message);
        }
        log.error("    reason: " + se.getMessage());
        log.error("state/code: " + se.getSQLState() + "/" + se.getErrorCode());
        log.debug("      dump:", se);
    }

    /**
     * Makes sure that <code>schemaObjectPrefix</code> does only consist of
     * characters that are allowed in names on the target database. Illegal
     * characters will be escaped as necessary.
     *
     * @throws Exception if an error occurs
     */
    protected void prepareSchemaObjectPrefix() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String legalChars = metaData.getExtraNameCharacters();
        legalChars += "ABCDEFGHIJKLMNOPQRSTUVWXZY0123456789_";

        String prefix = schemaObjectPrefix.toUpperCase();
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (legalChars.indexOf(c) == -1) {
                escaped.append("_x");
                String hex = Integer.toHexString(c);
                escaped.append("0000".toCharArray(), 0, 4 - hex.length());
                escaped.append(hex);
                escaped.append("_");
            } else {
                escaped.append(c);
            }
        }
        schemaObjectPrefix = escaped.toString();
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    protected void checkSchema() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String tableName = schemaObjectPrefix + "NODE";
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
            InputStream in = getSchemaDDL();
            if (in == null) {
                String msg = "Configuration error: unknown schema '" + schema + "'";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = con.createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    // Skip comments and empty lines
                    if (!sql.startsWith("#") && sql.length() > 0) {
                        // replace prefix variable
                        sql = createSchemaSql(sql);
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
                // commit the changes
                con.commit();
            } finally {
                IOUtils.closeQuietly(in);
                closeStatement(stmt);
            }
        }
    }

    /**
     * Replace wildcards and return the expanded SQL statement.
     *
     * @param sql The SQL with embedded wildcards.
     * @return The SQL with no wildcards present.
     */
    protected String createSchemaSql(String sql) {
       // replace prefix variable
        return Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
    }

    /**
     * Returns an input stream to the schema DDL resource.
     * @return an input stream to the schema DDL resource.
     */
    protected InputStream getSchemaDDL() {
        // JCR-595: Use the class explicitly instead of using getClass()
        // to avoid problems when subclassed in a different package
        return DatabasePersistenceManager.class.getResourceAsStream(schema + ".ddl");
    }

    /**
     * Builds the SQL statements
     */
    protected void buildSQLStatements() {
        nodeStateInsertSQL = "insert into "
                + schemaObjectPrefix + "NODE (NODE_DATA, NODE_ID) values (?, ?)";

        nodeStateUpdateSQL = "update "
                + schemaObjectPrefix + "NODE set NODE_DATA = ? where NODE_ID = ?";
        nodeStateSelectSQL = "select NODE_DATA from "
                + schemaObjectPrefix + "NODE where NODE_ID = ?";
        nodeStateSelectExistSQL = "select 1 from "
                + schemaObjectPrefix + "NODE where NODE_ID = ?";
        nodeStateDeleteSQL = "delete from "
                + schemaObjectPrefix + "NODE where NODE_ID = ?";

        propertyStateInsertSQL = "insert into "
                + schemaObjectPrefix + "PROP (PROP_DATA, PROP_ID) values (?, ?)";
        propertyStateUpdateSQL = "update "
                + schemaObjectPrefix + "PROP set PROP_DATA = ? where PROP_ID = ?";
        propertyStateSelectSQL = "select PROP_DATA from "
                + schemaObjectPrefix + "PROP where PROP_ID = ?";
        propertyStateSelectExistSQL = "select 1 from "
                + schemaObjectPrefix + "PROP where PROP_ID = ?";
        propertyStateDeleteSQL = "delete from "
                + schemaObjectPrefix + "PROP where PROP_ID = ?";

        nodeReferenceInsertSQL = "insert into "
                + schemaObjectPrefix + "REFS (REFS_DATA, NODE_ID) values (?, ?)";
        nodeReferenceUpdateSQL = "update "
                + schemaObjectPrefix + "REFS set REFS_DATA = ? where NODE_ID = ?";
        nodeReferenceSelectSQL = "select REFS_DATA from "
                + schemaObjectPrefix + "REFS where NODE_ID = ?";
        nodeReferenceSelectExistSQL = "select 1 from "
                + schemaObjectPrefix + "REFS where NODE_ID = ?";
        nodeReferenceDeleteSQL = "delete from "
                + schemaObjectPrefix + "REFS where NODE_ID = ?";

        if (!externalBLOBs) {
            blobInsertSQL = "insert into "
                    + schemaObjectPrefix + "BINVAL (BINVAL_DATA, BINVAL_ID) values (?, ?)";
            blobUpdateSQL = "update "
                    + schemaObjectPrefix + "BINVAL set BINVAL_DATA = ? where BINVAL_ID = ?";
            blobSelectSQL =
                    "select BINVAL_DATA from "
                    + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
            blobSelectExistSQL =
                    "select 1 from "
                    + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
            blobDeleteSQL = "delete from "
                    + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
        }
    }

    /**
     * Initializes the map of prepared statements.
     *
     * @throws SQLException if an error occurs
     */
    protected void initPreparedStatements() throws SQLException {
        preparedStatements.put(
                nodeStateInsertSQL, con.prepareStatement(nodeStateInsertSQL));
        preparedStatements.put(
                nodeStateUpdateSQL, con.prepareStatement(nodeStateUpdateSQL));
        preparedStatements.put(
                nodeStateSelectSQL, con.prepareStatement(nodeStateSelectSQL));
        preparedStatements.put(
                nodeStateSelectExistSQL, con.prepareStatement(nodeStateSelectExistSQL));
        preparedStatements.put(
                nodeStateDeleteSQL, con.prepareStatement(nodeStateDeleteSQL));

        preparedStatements.put(
                propertyStateInsertSQL, con.prepareStatement(propertyStateInsertSQL));
        preparedStatements.put(
                propertyStateUpdateSQL, con.prepareStatement(propertyStateUpdateSQL));
        preparedStatements.put(
                propertyStateSelectSQL, con.prepareStatement(propertyStateSelectSQL));
        preparedStatements.put(
                propertyStateSelectExistSQL, con.prepareStatement(propertyStateSelectExistSQL));
        preparedStatements.put(
                propertyStateDeleteSQL, con.prepareStatement(propertyStateDeleteSQL));

        preparedStatements.put(
                nodeReferenceInsertSQL, con.prepareStatement(nodeReferenceInsertSQL));
        preparedStatements.put(
                nodeReferenceUpdateSQL, con.prepareStatement(nodeReferenceUpdateSQL));
        preparedStatements.put(
                nodeReferenceSelectSQL, con.prepareStatement(nodeReferenceSelectSQL));
        preparedStatements.put(
                nodeReferenceSelectExistSQL, con.prepareStatement(nodeReferenceSelectExistSQL));
        preparedStatements.put(
                nodeReferenceDeleteSQL, con.prepareStatement(nodeReferenceDeleteSQL));

        if (!externalBLOBs) {
            preparedStatements.put(blobInsertSQL, con.prepareStatement(blobInsertSQL));
            preparedStatements.put(blobUpdateSQL, con.prepareStatement(blobUpdateSQL));
            preparedStatements.put(blobSelectSQL, con.prepareStatement(blobSelectSQL));
            preparedStatements.put(blobSelectExistSQL, con.prepareStatement(blobSelectExistSQL));
            preparedStatements.put(blobDeleteSQL, con.prepareStatement(blobDeleteSQL));
        }
    }

    //--------------------------------------------------------< inner classes >

    static class SizedInputStream extends FilterInputStream {
        private final long size;
        private boolean consumed = false;

        SizedInputStream(InputStream in, long size) {
            super(in);
            this.size = size;
        }

        long getSize() {
            return size;
        }

        boolean isConsumed() {
            return consumed;
        }

        public int read() throws IOException {
            consumed = true;
            return super.read();
        }

        public long skip(long n) throws IOException {
            consumed = true;
            return super.skip(n);
        }

        public int read(byte[] b) throws IOException {
            consumed = true;
            return super.read(b);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            consumed = true;
            return super.read(b, off, len);
        }
    }

    class DbBLOBStore implements BLOBStore {
        /**
         * {@inheritDoc}
         */
        public String createId(PropertyId id, int index) {
            // the blobId is a simple string concatenation of id plus index
            StringBuilder sb = new StringBuilder();
            sb.append(id.toString());
            sb.append('[');
            sb.append(index);
            sb.append(']');
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         */
        public InputStream get(String blobId) throws Exception {
            synchronized (blobSelectSQL) {
                Statement stmt = executeStmt(blobSelectSQL, new Object[]{blobId});
                final ResultSet rs = stmt.getResultSet();
                if (!rs.next()) {
                    closeResultSet(rs);
                    throw new Exception("no such BLOB: " + blobId);
                }
                InputStream in = rs.getBinaryStream(1);
                if (in == null) {
                    // some databases treat zero-length values as NULL;
                    // return empty InputStream in such a case
                    closeResultSet(rs);
                    return new ByteArrayInputStream(new byte[0]);
                }

                /**
                 * return an InputStream wrapper in order to
                 * close the ResultSet when the stream is closed
                 */
                return new FilterInputStream(in) {
                    public void close() throws IOException {
                        in.close();
                        // now it's safe to close ResultSet
                        closeResultSet(rs);
                    }
                };
            }
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void put(String blobId, InputStream in, long size)
                throws Exception {
            Statement stmt = executeStmt(blobSelectExistSQL, new Object[]{blobId});
            ResultSet rs = stmt.getResultSet();
            // a BLOB exists if the result has at least one entry
            boolean exists = rs.next();
            closeResultSet(rs);

            String sql = (exists) ? blobUpdateSQL : blobInsertSQL;
            executeStmt(sql, new Object[]{new SizedInputStream(in, size), blobId});
        }

        /**
         * {@inheritDoc}
         */
        public synchronized boolean remove(String blobId) throws Exception {
            Statement stmt = executeStmt(blobDeleteSQL, new Object[]{blobId});
            return stmt.getUpdateCount() == 1;
        }
    }
}
