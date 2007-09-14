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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.apache.jackrabbit.core.persistence.bundle.util.DbNameIndex;
import org.apache.jackrabbit.core.persistence.bundle.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.bundle.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.bundle.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.bundle.util.StringIndex;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.uuid.UUID;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * This is a generic persistence manager that stores the {@link NodePropBundle}s
 * in a database.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setConsistencyFix(String) consistencyFix}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="4096"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value=""/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/>
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value=""/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * <li>&lt;param name="{@link #setBlockOnConnectionLoss(String) blockOnConnectionLoss}" value="false"/>
 * </ul>
 */
public class BundleDbPersistenceManager extends AbstractBundlePersistenceManager {

    /** the cvs/svn id */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(BundleDbPersistenceManager.class);

    /** the variable for the schema prefix */
    public static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    /** storage model modifier: binary keys */
    public static final int SM_BINARY_KEYS = 1;

    /** storage model modifier: longlong keys */
    public static final int SM_LONGLONG_KEYS = 2;

    /** flag indicating if this manager was initialized */
    protected boolean initialized = false;

    /** the jdbc driver name */
    protected String driver;

    /** the jdbc url string */
    protected String url;

    /** the jdbc user */
    protected String user;

    /** the jdbc password */
    protected String password;

    /** the schema identifier */
    protected String schema;

    /** the prefix for the database objects */
    protected String schemaObjectPrefix;

    /** flag indicating if a consistency check should be issued during startup */
    protected boolean consistencyCheck = false;

    /** flag indicating if the consistency check should attempt to fix issues */
    protected boolean consistencyFix = false;

    /** initial size of buffer used to serialize objects */
    protected static final int INITIAL_BUFFER_SIZE = 1024;

    /** inidicates if uses (filesystem) blob store */
    protected boolean externalBLOBs;

    /** indicates whether to block if the database connection is lost */
    protected boolean blockOnConnectionLoss = false;

    /**
     * The class that manages statement execution and recovery from connection loss.
     */
    protected ConnectionRecoveryManager connectionManager;

    // SQL statements for bundle management
    protected String bundleInsertSQL;
    protected String bundleUpdateSQL;
    protected String bundleSelectSQL;
    protected String bundleDeleteSQL;

    // SQL statements for NodeReference management
    protected String nodeReferenceInsertSQL;
    protected String nodeReferenceUpdateSQL;
    protected String nodeReferenceSelectSQL;
    protected String nodeReferenceDeleteSQL;

    /** file system where BLOB data is stored */
    protected CloseableBLOBStore blobStore;

    /** the index for local names */
    private StringIndex nameIndex;

    /**
     * the minimum size of a property until it gets written to the blob store
     * @see #setMinBlobSize(String)
     */
    private int minBlobSize = 0x1000;

    /**
     * flag for error handling
     */
    protected ErrorHandling errorHandling = new ErrorHandling();

    /**
     * the bundle binding
     */
    protected BundleBinding binding;

    /**
     * the name of this persistence manager
     */
    private String name = super.toString();


    /**
     * Returns the configured JDBC connection url.
     * @return the configured JDBC connection url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the JDBC connection url.
     * @param url the url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the configured user that is used to establish JDBC connections.
     * @return the JDBC user.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user name that will be used to establish JDBC connections.
     * @param user the user name.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the configured password that is used to establish JDBC connections.
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password that will be used to establish JDBC connections.
     * @param password the password for the connection
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the class name of the JDBC driver.
     * @return the class name of the JDBC driver.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the class name of the JDBC driver. The driver class will be loaded
     * during {@link #init(PMContext) init} in order to assure the existence.
     *
     * @param driver the class name of the driver
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Returns the configured schema object prefix.
     * @return the configured schema object prefix.
     */
    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    /**
     * Sets the schema object prefix. This string is used to prefix all schema
     * objects, like tables and indexes. this is usefull, if several persistence
     * managers use the same database.
     *
     * @param schemaObjectPrefix the prefix for schema objects.
     */
    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        // make sure prefix is all uppercase
        this.schemaObjectPrefix = schemaObjectPrefix.toUpperCase();
    }

    /**
     * Returns the configured schema identifier.
     * @return the schema identifier.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema identifier. This identifier is used to load and execute
     * the respective .ddl resource in order to create the required schema
     * objects.
     *
     * @param schema the schema identifier.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns if uses external (filesystem) blob store.
     * @return if uses external (filesystem) blob store.
     */
    public boolean isExternalBLOBs() {
        return externalBLOBs;
    }

    /**
     * Sets the flag for external (filesystem) blob store usage.
     * @param externalBLOBs a value of "true" indicates that an external blob
     *        store is to be used.
     */
    public void setExternalBLOBs(boolean externalBLOBs) {
        this.externalBLOBs = externalBLOBs;
    }

    /**
     * Checks if consistency check is enabled.
     * @return <code>true</code> if consistenct check is enabled.
     */
    public String getConsistencyCheck() {
        return Boolean.toString(consistencyCheck);
    }

    /**
     * Defines if a consistency check is to be performed on initialization.
     * @param consistencyCheck the consistency check flag.
     */
    public void setConsistencyCheck(String consistencyCheck) {
        this.consistencyCheck = Boolean.valueOf(consistencyCheck).booleanValue();
    }

    /**
     * Checks if consistency fix is enabled.
     * @return <code>true</code> if consistency fix is enabled.
     */
    public String getConsistencyFix() {
        return Boolean.toString(consistencyFix);
    }

    /**
     * Defines if the consistency check should attempt to fix issues that
     * it finds.
     *
     * @param consistencyFix the consistency fix flag.
     */
    public void setConsistencyFix(String consistencyFix) {
        this.consistencyFix = Boolean.valueOf(consistencyFix).booleanValue();
    }

    /**
     * Returns the miminum blob size in bytes.
     * @return the miminum blob size in bytes.
     */
    public String getMinBlobSize() {
        return String.valueOf(minBlobSize);
    }

    /**
     * Sets the minumum blob size. This size defines the threshhold of which
     * size a property is included in the bundle or is stored in the blob store.
     *
     * @param minBlobSize the minimum blobsize in bytes.
     */
    public void setMinBlobSize(String minBlobSize) {
        this.minBlobSize = Integer.decode(minBlobSize).intValue();
    }

    /**
     * Sets the error handling behaviour of this manager. See {@link ErrorHandling}
     * for details about the flags.
     *
     * @param errorHandling the error handling flags
     */
    public void setErrorHandling(String errorHandling) {
        this.errorHandling = new ErrorHandling(errorHandling);
    }

    /**
     * Returns the error handling configuration of this manager
     * @return the error handling configuration of this manager
     */
    public String getErrorHandling() {
        return errorHandling.toString();
    }

    public void setBlockOnConnectionLoss(String block) {
        this.blockOnConnectionLoss = Boolean.valueOf(block).booleanValue();
    }

    public String getBlockOnConnectionLoss() {
        return Boolean.toString(blockOnConnectionLoss);
    }

    /**
     * Returns <code>true</code> if the blobs are stored in the DB.
     * @return <code>true</code> if the blobs are stored in the DB.
     */
    public boolean useDbBlobStore() {
        return !externalBLOBs;
    }

    /**
     * Returns <code>true</code> if the blobs are stored in the local fs.
     * @return <code>true</code> if the blobs are stored in the local fs.
     */
    public boolean useLocalFsBlobStore() {
        return externalBLOBs;
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws SQLException if an SQL error occurs.
     * @throws RepositoryException if an error occurs.
     */
    protected void checkSchema() throws SQLException, RepositoryException {
        if (!checkTablesExist()) {
            // read ddl from resources
            InputStream in = BundleDbPersistenceManager.class.getResourceAsStream(schema + ".ddl");
            if (in == null) {
                String msg = "Configuration error: unknown schema '" + schema + "'";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = connectionManager.getConnection().createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    sql = createSchemaSQL(sql);
                    if (sql.length() > 0 && (sql.indexOf("BINVAL") < 0 || useDbBlobStore())) {
                        // only create blob related tables of db blob store configured
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
            } catch (IOException e) {
                String msg = "Configuration error: unable to read schema '" + schema + "': " + e;
                log.debug(msg);
                throw new RepositoryException(msg, e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
                stmt.close();
            }
        }
    }

    /**
     * Creates an SQL statement for schema creation by variable substitution.
     *
     * @param sql a SQL string which may contain variables to substitute
     * @return a valid SQL string
     */
    protected String createSchemaSQL(String sql) {
        // replace prefix variable
        return Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix).trim();
    }

    /**
     * Checks if the database table exist.
     *
     * @return <code>true</code> if the tables exist;
     *         <code>false</code> otherwise.
     *
     * @throws SQLException if an SQL erro occurs.
     */
    protected boolean checkTablesExist() throws SQLException {
        DatabaseMetaData metaData = connectionManager.getConnection().getMetaData();
        String tableName = schemaObjectPrefix + "BUNDLE";
        if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase();
        }
        String userName = checkTablesWithUser() ? metaData.getUserName() : null;
        ResultSet rs = metaData.getTables(null, userName, tableName, null);
        try {
            return rs.next();
        } finally {
            rs.close();
        }
    }

    /**
     * Indicates if the username should be included when retrieving the tables
     * during {@link #checkTablesExist()}.
     * <p/>
     * Please note that this currently only needs to be changed for oracle based
     * persistence managers.
     *
     * @return <code>false</code>
     */
    protected boolean checkTablesWithUser() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Basically wrapps a JDBC transaction around super.store().
     */
    public synchronized void store(ChangeLog changeLog) throws ItemStateException {
        Connection con = null;
        try {
            boolean tryAgain = true;
            do {
                try {
                    con = connectionManager.getConnection();
                    connectionManager.setAutoReconnect(false);
                    con.setAutoCommit(false);
                    super.store(changeLog);
                    con.commit();
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    if (tryAgain) {
                        tryAgain = false;
                        continue;
                    }
                    throw e;
                }
            } while(false);
        } catch (Throwable th) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException e) {
                logException("rollback failed", e);
            }
            if (th instanceof SQLException || th.getCause() instanceof SQLException) {
                connectionManager.close();
            }
            throw new ItemStateException(th.getMessage());
        } finally {
            connectionManager.setAutoReconnect(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        super.init(context);

        this.name = context.getHomeDir().getName();

        connectionManager = new ConnectionRecoveryManager(blockOnConnectionLoss,
                getDriver(), getUrl(), getUser(), getPassword());

        // make sure schemaObjectPrefix consists of legal name characters only
        prepareSchemaObjectPrefix();

        // check if schema objects exist and create them if necessary
        checkSchema();

        // create correct blob store
        blobStore = createBlobStore();

        buildSQLStatements();

        // load namespaces
        binding = new BundleBinding(errorHandling, blobStore, getNsIndex(), getNameIndex(), context.getDataStore());
        binding.setMinBlobSize(minBlobSize);

        initialized = true;

        if (consistencyCheck) {
            checkConsistency();
        }
    }

    /**
     * Creates a suitable blobstore
     * @return a blobstore
     * @throws Exception if an unspecified error occurs
     */
    protected CloseableBLOBStore createBlobStore() throws Exception {
        if (useLocalFsBlobStore()) {
            return createLocalFSBlobStore(context);
        } else {
            return createDBBlobStore(context);
        }
    }

    /**
     * Returns the local name index
     * @return the local name index
     * @throws IllegalStateException if an error occurs.
     */
    public StringIndex getNameIndex() {
        try {
            if (nameIndex == null) {
                FileSystemResource res = new FileSystemResource(context.getFileSystem(), RES_NAME_INDEX);
                if (res.exists()) {
                    nameIndex = super.getNameIndex();
                } else {
                    // create db nameindex
                    nameIndex = createDbNameIndex();
                }
            }
            return nameIndex;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create nsIndex: " + e);
        }
    }

    /**
     * Retruns a new instance of a DbNameIndex.
     * @return a new instance of a DbNameIndex.
     * @throws SQLException if an SQL error occurs.
     */
    protected DbNameIndex createDbNameIndex() throws SQLException {
        return new DbNameIndex(connectionManager, schemaObjectPrefix);
    }

    /**
     * returns the storage model
     * @return the storage model
     */
    public int getStorageModel() {
        return SM_BINARY_KEYS;
    }

    /**
     * Creates a blob store that is based on a local fs. This is called by
     * init if {@link #useLocalFsBlobStore()} returns <code>true</code>.
     *
     * @param context the persistence manager context
     * @return a blob store
     * @throws Exception if an error occurs.
     */
    protected CloseableBLOBStore createLocalFSBlobStore(PMContext context)
            throws Exception {
        /**
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        return new FSBlobStore(blobFS);
    }

    /**
     * Creates a blob store that uses the database. This is called by
     * init if {@link #useDbBlobStore()} returns <code>true</code>.
     *
     * @param context the persistence manager context
     *
     * @return a blob store
     * @throws Exception if an error occurs.
     */
    protected CloseableBLOBStore createDBBlobStore(PMContext context)
            throws Exception {
        return new DbBlobStore();
    }

    /**
     * Performs a consistency check.
     */
    private void checkConsistency() {
        int count = 0;
        int total = 0;
        log.info("{}: checking workspace consistency...", name);

        Collection modifications = new ArrayList();
        ResultSet rs = null;
        DataInputStream din = null;
        try {
            String sql;
            if (getStorageModel() == SM_BINARY_KEYS) {
                sql = "select NODE_ID, BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE";
            } else {
                sql = "select NODE_ID_HI, NODE_ID_LO, BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE";
            }
            Statement stmt = connectionManager.executeStmt(sql, new Object[0]);
            rs = stmt.getResultSet();
            while (rs.next()) {
                NodeId id;
                Blob blob;
                if (getStorageModel() == SM_BINARY_KEYS) {
                    id = new NodeId(new UUID(rs.getBytes(1)));
                    blob = rs.getBlob(2);
                } else {
                    id = new NodeId(new UUID(rs.getLong(1), rs.getLong(2)));
                    blob = rs.getBlob(3);
                }
                din = new DataInputStream(blob.getBinaryStream());
                try {
                    NodePropBundle bundle = binding.readBundle(din, id);
                    Collection missingChildren = new ArrayList();
                    Iterator iter = bundle.getChildNodeEntries().iterator();
                    while (iter.hasNext()) {
                        NodePropBundle.ChildNodeEntry entry = (NodePropBundle.ChildNodeEntry) iter.next();
                        if (entry.getId().toString().endsWith("babecafebabe")) {
                            continue;
                        }
                        if (id.toString().endsWith("babecafebabe")) {
                            continue;
                        }
                        try {
                            NodePropBundle child = loadBundle(entry.getId());
                            if (child == null) {
                                log.error("NodeState " + id.getUUID() + " references inexistent child " + entry.getName() + " with id " + entry.getId().getUUID());
                                missingChildren.add(entry);
                            } else {
                                NodeId cp = child.getParentId();
                                if (cp == null) {
                                    log.error("ChildNode has invalid parent uuid: null");
                                } else if (!cp.equals(id)) {
                                    log.error("ChildNode has invalid parent uuid: " + cp + " (instead of " + id.getUUID() + ")");
                                }
                            }
                        } catch (ItemStateException e) {
                            log.error("Error while loading child node: " + e);
                        }
                    }
                    if (consistencyFix && !missingChildren.isEmpty()) {
                        Iterator iterator = missingChildren.iterator();
                        while (iterator.hasNext()) {
                            bundle.getChildNodeEntries().remove(iterator.next());
                        }
                        modifications.add(bundle);
                    }

                    NodeId parentId = bundle.getParentId();
                    if (parentId != null) {
                        if (!exists(parentId)) {
                            log.error("NodeState " + id + " references inexistent parent id " + parentId);
                        }
                    }
                } catch (IOException e) {
                    log.error("Error in bundle " + id + ": " + e);
                    din = new DataInputStream(blob.getBinaryStream());
                    binding.checkBundle(din);
                }
                count++;
                if (count % 1000 == 0) {
                    log.info(name + ": checked " + count + "/" + total + " bundles...");
                }
            }
        } catch (Exception e) {
            log.error("Error in bundle", e);
        } finally {
            closeStream(din);
            closeResultSet(rs);
        }

        if (consistencyFix && !modifications.isEmpty()) {
            log.info(name + ": Fixing " + modifications.size() + " inconsistent bundle(s)...");
            Iterator iterator = modifications.iterator();
            while (iterator.hasNext()) {
                NodePropBundle bundle = (NodePropBundle) iterator.next();
                try {
                    log.info(name + ": Fixing bundle " + bundle.getId());
                    bundle.markOld(); // use UPDATE instead of INSERT
                    storeBundle(bundle);
                } catch (ItemStateException e) {
                    log.error(name + ": Error storing fixed bundle: " + e);
                }
            }
        }

        log.info(name + ": checked " + count + "/" + total + " bundles.");
    }


    /**
     * Makes sure that <code>schemaObjectPrefix</code> does only consist of
     * characters that are allowed in names on the target database. Illegal
     * characters will be escaped as necessary.
     *
     * @throws Exception if an error occurs
     */
    protected void prepareSchemaObjectPrefix() throws Exception {
        DatabaseMetaData metaData = connectionManager.getConnection().getMetaData();
        String legalChars = metaData.getExtraNameCharacters();
        legalChars += "ABCDEFGHIJKLMNOPQRSTUVWXZY0123456789_";

        String prefix = schemaObjectPrefix.toUpperCase();
        StringBuffer escaped = new StringBuffer();
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
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            if (nameIndex instanceof DbNameIndex) {
                ((DbNameIndex) nameIndex).close();
            }
            connectionManager.close();
            // close blob store
            blobStore.close();
            blobStore = null;
        } finally {
            initialized = false;
        }
    }

    /**
     * Sets the key parameters to the prepared statement, starting at
     * <code>pos</code> and returns the number of key parameters + pos.
     *
     * @param stmt the statement
     * @param uuid the uuid of the key
     * @param pos the position of the key parameter
     * @return the number of key parameters + <code>pos</code>
     * @throws SQLException if an SQL error occurs.
     */
    protected int setKey(PreparedStatement stmt, UUID uuid, int pos)
            throws SQLException {
        if (getStorageModel() == SM_BINARY_KEYS) {
            stmt.setBytes(pos++, uuid.getRawBytes());
        } else {
            stmt.setLong(pos++, uuid.getMostSignificantBits());
            stmt.setLong(pos++, uuid.getLeastSignificantBits());
        }
        return pos;
    }

    /**
     * Constructs a parameter list for a PreparedStatement
     * for the given UUID.
     *
     * @param uuid the uuid
     * @return a list of Objects
     */
    protected Object[] getKey(UUID uuid) {
        if (getStorageModel() == SM_BINARY_KEYS) {
            return new Object[]{uuid.getRawBytes()};
        } else {
            return new Object[]{new Long(uuid.getMostSignificantBits()),
                    new Long(uuid.getLeastSignificantBits())};
        }
    }

    /**
     * Creates a parameter array for an SQL statement that needs
     * (i) a UUID, and (2) another parameter.
     *
     * @param uuid the UUID
     * @param p the other parameter
     * @param before whether the other parameter should be before the uuid parameter
     * @return an Object array that represents the parameters
     */
    protected Object[] createParams(UUID uuid, Object p, boolean before) {

        // Create the key
        List key = new ArrayList();
        if (getStorageModel() == SM_BINARY_KEYS) {
            key.add(uuid.getRawBytes());
        } else {
            key.add(new Long(uuid.getMostSignificantBits()));
            key.add(new Long(uuid.getLeastSignificantBits()));
        }

        // Create the parameters
        List params = new ArrayList();
        if (before) {
            params.add(p);
            params.addAll(key);
        } else {
            params.addAll(key);
            params.add(p);
        }

        return params.toArray();
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized NodePropBundle loadBundle(NodeId id)
            throws ItemStateException {
        ResultSet rs = null;
        InputStream in = null;
        try {
            Statement stmt = connectionManager.executeStmt(bundleSelectSQL, getKey(id.getUUID()));
            rs = stmt.getResultSet();
            if (!rs.next()) {
                return null;
            }
            Blob b = rs.getBlob(1);
            // JCR-1039: pre-fetch/buffer blob data
            long length = b.length();
            byte[] bytes = new byte[(int) length];
            in = b.getBinaryStream();
            int read, pos = 0;
            while ((read = in.read(bytes, pos, bytes.length - pos)) > 0) {
                pos += read;
            }
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(bytes));
            NodePropBundle bundle = binding.readBundle(din, id);
            bundle.setSize(length);
            return bundle;
        } catch (Exception e) {
            String msg = "failed to read bundle: " + id + ": " + e;
            log.error(msg);
            throw new ItemStateException(msg, e);
        } finally {
            closeStream(in);
            closeResultSet(rs);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized boolean existsBundle(NodeId id) throws ItemStateException {
        ResultSet rs = null;
        try {
            Statement stmt = connectionManager.executeStmt(bundleSelectSQL, getKey(id.getUUID()));
            rs = stmt.getResultSet();
            // a bundle exists, if the result has at least one entry
            return rs.next();
        } catch (Exception e) {
            String msg = "failed to check existence of bundle: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeResultSet(rs);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void storeBundle(NodePropBundle bundle) throws ItemStateException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            DataOutputStream dout = new DataOutputStream(out);
            binding.writeBundle(dout, bundle);
            dout.close();

            String sql = bundle.isNew() ? bundleInsertSQL : bundleUpdateSQL;
            Object[] params = createParams(bundle.getId().getUUID(), out.toByteArray(), true);
            connectionManager.executeStmt(sql, params);
        } catch (Exception e) {
            String msg = "failed to write bundle: " + bundle.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void destroyBundle(NodePropBundle bundle) throws ItemStateException {
        try {
            connectionManager.executeStmt(bundleDeleteSQL, getKey(bundle.getId().getUUID()));
            // also delete all
            bundle.removeAllProperties();
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to delete bundle: " + bundle.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences load(NodeReferencesId targetId)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        ResultSet rs = null;
        InputStream in = null;
        try {
            Statement stmt = connectionManager.executeStmt(
                    nodeReferenceSelectSQL, getKey(targetId.getTargetId().getUUID()));
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
            String msg = "failed to read references: " + targetId;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeStream(in);
            closeResultSet(rs);
        }
    }

    /**
     * This method uses shared <code>PreparedStatements</code>, which must
     * be used strictly sequentially. Because this method synchronizes on the
     * persistence manager instance, there is no need to synchronize on the
     * shared statement. If the method would not be sychronized, the shared
     * statement must be synchronized.
     *
     * @see AbstractPersistenceManager#store(NodeReferences)
     */
    public synchronized void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = exists(refs.getId());
        String sql = (update) ? nodeReferenceUpdateSQL : nodeReferenceInsertSQL;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            Serializer.serialize(refs, out);

            Object[] params = createParams(refs.getTargetId().getUUID(), out.toByteArray(), true);
            connectionManager.executeStmt(sql, params);

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write node references: " + refs.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            connectionManager.executeStmt(nodeReferenceDeleteSQL,
                    getKey(refs.getTargetId().getUUID()));
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to delete references: " + refs.getTargetId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(NodeReferencesId targetId) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        ResultSet rs = null;
        try {
            Statement stmt = connectionManager.executeStmt(nodeReferenceSelectSQL,
                    getKey(targetId.getTargetId().getUUID()));
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

    /**
     * Resets the given <code>PreparedStatement</code> by clearing the
     * parameters and warnings contained.
     *
     * @param stmt The <code>PreparedStatement</code> to reset. If
     *             <code>null</code> this method does nothing.
     */
    protected synchronized void resetStatement(PreparedStatement stmt) {
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
     * Closes the result set
     * @param rs the result set
     */
    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException se) {
                logException("Failed closing ResultSet", se);
            }
        }
    }

    /**
     * closes the input stream
     * @param ins the inputs stream
     */
    protected void closeStream(InputStream ins) {
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    /**
     * closes the statement
     * @param stmt the statemenet
     */
    protected void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException se) {
                logException("Failed closing PreparedStatement", se);
            }
        }
    }

    /**
     * logs an sql exception
     * @param message the message
     * @param se the exception
     */
    protected void logException(String message, SQLException se) {
        if (message != null) {
            log.error(message);
        }
        log.error("       Reason: " + se.getMessage());
        log.error("   State/Code: " + se.getSQLState() + "/" +
                se.getErrorCode());
        log.debug("   dump:", se);
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        return name;
    }

    /**
     * Initializes the SQL strings.
     */
    protected void buildSQLStatements() {
        // prepare statements
        if (getStorageModel() == SM_BINARY_KEYS) {
            bundleInsertSQL = "insert into " + schemaObjectPrefix + "BUNDLE (BUNDLE_DATA, NODE_ID) values (?, ?)";
            bundleUpdateSQL = "update " + schemaObjectPrefix + "BUNDLE set BUNDLE_DATA = ? where NODE_ID = ?";
            bundleSelectSQL = "select BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE where NODE_ID = ?";
            bundleDeleteSQL = "delete from " + schemaObjectPrefix + "BUNDLE where NODE_ID = ?";

            nodeReferenceInsertSQL = "insert into " + schemaObjectPrefix + "REFS (REFS_DATA, NODE_ID) values (?, ?)";
            nodeReferenceUpdateSQL = "update " + schemaObjectPrefix + "REFS set REFS_DATA = ? where NODE_ID = ?";
            nodeReferenceSelectSQL = "select REFS_DATA from " + schemaObjectPrefix + "REFS where NODE_ID = ?";
            nodeReferenceDeleteSQL = "delete from " + schemaObjectPrefix + "REFS where NODE_ID = ?";
        } else {
            bundleInsertSQL = "insert into " + schemaObjectPrefix + "BUNDLE (BUNDLE_DATA, NODE_ID_HI, NODE_ID_LO) values (?, ?, ?)";
            bundleUpdateSQL = "update " + schemaObjectPrefix + "BUNDLE set BUNDLE_DATA = ? where NODE_ID_HI = ? and NODE_ID_LO = ?";
            bundleSelectSQL = "select BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?";
            bundleDeleteSQL = "delete from " + schemaObjectPrefix + "BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?";

            nodeReferenceInsertSQL = "insert into " + schemaObjectPrefix + "REFS (REFS_DATA, NODE_ID_HI, NODE_ID_LO) values (?, ?, ?)";
            nodeReferenceUpdateSQL = "update " + schemaObjectPrefix + "REFS set REFS_DATA = ? where NODE_ID_HI = ? and NODE_ID_LO = ?";
            nodeReferenceSelectSQL = "select REFS_DATA from " + schemaObjectPrefix + "REFS where NODE_ID_HI = ? and NODE_ID_LO = ?";
            nodeReferenceDeleteSQL = "delete from " + schemaObjectPrefix + "REFS where NODE_ID_HI = ? and NODE_ID_LO = ?";
        }
    }

    /**
     * Helper interface for closeable stores
     */
    protected static interface CloseableBLOBStore extends BLOBStore {
        void close();
    }

    /**
     * own implementation of the filesystem blob store that uses a different
     * blob-id scheme.
     */
    protected class FSBlobStore extends FileSystemBLOBStore implements CloseableBLOBStore {

        private FileSystem fs;

        public FSBlobStore(FileSystem fs) {
            super(fs);
            this.fs = fs;
        }

        public String createId(PropertyId id, int index) {
            return buildBlobFilePath(null, id, index).toString();
        }

        public void close() {
            try {
                fs.close();
                fs = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Implementation of a blob store that stores the data inside the database
     */
    protected class DbBlobStore implements CloseableBLOBStore {

        protected String blobInsertSQL;
        protected String blobUpdateSQL;
        protected String blobSelectSQL;
        protected String blobSelectExistSQL;
        protected String blobDeleteSQL;

        public DbBlobStore() throws SQLException {
            blobInsertSQL = "insert into " + schemaObjectPrefix + "BINVAL (BINVAL_DATA, BINVAL_ID) values (?, ?)";
            blobUpdateSQL = "update " + schemaObjectPrefix + "BINVAL set BINVAL_DATA = ? where BINVAL_ID = ?";
            blobSelectSQL = "select BINVAL_DATA from " + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
            blobSelectExistSQL = "select 1 from " + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
            blobDeleteSQL = "delete from " + schemaObjectPrefix + "BINVAL where BINVAL_ID = ?";
        }

        /**
         * {@inheritDoc}
         */
        public String createId(PropertyId id, int index) {
            StringBuffer buf = new StringBuffer();
            buf.append(id.getParentId().toString());
            buf.append('.');
            buf.append(getNsIndex().stringToIndex(id.getName().getNamespaceURI()));
            buf.append('.');
            buf.append(getNameIndex().stringToIndex(id.getName().getLocalName()));
            buf.append('.');
            buf.append(index);
            return buf.toString();
        }

        /**
         * {@inheritDoc}
         */
        public InputStream get(String blobId) throws Exception {
            Statement stmt = connectionManager.executeStmt(blobSelectSQL, new Object[]{blobId});
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

        /**
         * {@inheritDoc}
         */
        public synchronized void put(String blobId, InputStream in, long size)
                throws Exception {
            Statement stmt = connectionManager.executeStmt(blobSelectExistSQL, new Object[]{blobId});
            ResultSet rs = stmt.getResultSet();
            // a BLOB exists if the result has at least one entry
            boolean exists = rs.next();
            closeResultSet(rs);

            String sql = (exists) ? blobUpdateSQL : blobInsertSQL;
            Object[] params = new Object[]{new ConnectionRecoveryManager.StreamWrapper(in, size), blobId};
            connectionManager.executeStmt(sql, params);
        }

        /**
         * {@inheritDoc}
         */
        public synchronized boolean remove(String blobId) throws Exception {
            Statement stmt = connectionManager.executeStmt(blobDeleteSQL, new Object[]{blobId});
            return stmt.getUpdateCount() == 1;
        }

        public void close() {
            // closing the database resources of this blobstore is left to the
            // owning BundleDbPersistenceManager
        }
    }
}
