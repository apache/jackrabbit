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
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.bundle.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.apache.jackrabbit.core.persistence.bundle.util.DbNameIndex;
import org.apache.jackrabbit.core.persistence.bundle.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.bundle.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.bundle.util.StringIndex;
import org.apache.jackrabbit.core.persistence.bundle.util.NodePropBundle.ChildNodeEntry;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** indicates if uses (filesystem) blob store */
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
    protected String bundleSelectAllIdsFromSQL;
    protected String bundleSelectAllIdsSQL;

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
     * Sets the JDBC connection URL.
     * The connection can be created using a JNDI Data Source as well. 
     * To do that, the driver class name must reference a javax.naming.Context class 
     * (for example javax.naming.InitialContext), and the URL must be the JNDI URL 
     * (for example java:comp/env/jdbc/Test).
     * 
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
     * If no driver is specified, the default driver for the database is used.
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
     * objects, like tables and indexes. this is useful, if several persistence
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
     * @return <code>true</code> if consistency check is enabled.
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
     * Returns the minimum blob size in bytes.
     * @return the minimum blob size in bytes.
     */
    public String getMinBlobSize() {
        return String.valueOf(minBlobSize);
    }

    /**
     * Sets the minimum blob size. This size defines the threshold of which
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
                String msg = "Configuration error: The resource '" + schema + ".ddl' could not be found";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = connectionManager.getConnection().createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    sql = createSchemaSQL(sql);
                    if (!sql.startsWith("#") && sql.length() > 0
                            && (sql.indexOf("BINVAL") < 0 || useDbBlobStore())) {
                        // only create blob related tables of db blob store configured
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
            } catch (IOException e) {
                String msg = "Configuration error: unable to read the resource '" + schema + ".ddl': " + e;
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
     * @throws SQLException if a database error occurs.
     * @throws RepositoryException if a repository exception occurs.
     */
    protected boolean checkTablesExist() throws SQLException, RepositoryException {
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
     * Basically wraps a JDBC transaction around super.store().
     */
    public synchronized void store(ChangeLog changeLog) throws ItemStateException {
        int trials = 2;
        Throwable lastException  = null;
        do {
            trials--;
            Connection con = null;
            try {
                con = connectionManager.getConnection();
                connectionManager.setAutoReconnect(false);
                con.setAutoCommit(false);
                super.store(changeLog);
                con.commit();
                con.setAutoCommit(true);
                return;
            } catch (Throwable th) {
                lastException = th;
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
            } finally {
                connectionManager.setAutoReconnect(true);
            }
        } while(blockOnConnectionLoss || trials > 0);
        throw new ItemStateException(lastException.getMessage());
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
            // check all bundles
            checkConsistency(null, true, consistencyFix);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected BundleBinding getBinding() {
        return binding;
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
            IllegalStateException exception =
                new IllegalStateException("Unable to create nsIndex");
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Returns a new instance of a DbNameIndex.
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
     * Helper method that returns the jcr path for a node bundle.
     * 
     * <p>
     * Resolves the special root nodes (eg. <code>/jcr:system</code>). If a
     * node cannot be found in the list of child nodes of its parent (ie. has no
     * name), <code>{{missing}}</code> will be used instead.
     * 
     * @param id
     *            NodeId for which the path should be displayed
     * @param bundle
     *            bundle for id, optional (if <code>null</code>, will be
     *            loaded via given id)
     * @return the path of the node identified by id, with namespaces not
     *         resolved (eg. <code>{http://www.jcp.org/jcr/1.0}</code> instead
     *         of <code>jcr:</code>)
     * @throws ItemStateException
     *             if loading one of the parent nodes failed
     */
    protected String getBundlePath(NodeId id, NodePropBundle bundle) throws ItemStateException {
        // load bundle on demand
        if (bundle == null) {
            bundle = loadBundle(id);
            if (bundle == null) {
                return "{{missing}}";
            }
        }
        
        // check for the various special root nodes
        if (id.equals(RepositoryImpl.VERSION_STORAGE_NODE_ID)) {
            return "/jcr:system/jcr:versionStorage";
        } else if (id.equals(RepositoryImpl.NODETYPES_NODE_ID)) {
            return "/jcr:system/jcr:nodeTypes";
        } else if (id.equals(RepositoryImpl.SYSTEM_ROOT_NODE_ID)) {
            return "/jcr:system";
        } else if (id.equals(RepositoryImpl.ROOT_NODE_ID)) {
            return "";
        } else if (bundle.getParentId() == null) {
            return "";
        } else if (bundle.getParentId().equals(id)) {
            return "";
        }
        
        // get the name of this bundle by looking at the parent
        NodePropBundle parentBundle = loadBundle(bundle.getParentId());
        if (parentBundle == null) {
            return "{{missing}}";
        }
        
        String name = "{{missing}}";
        Iterator iter = parentBundle.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (entry.getId().equals(id)) {
                String uri = entry.getName().getNamespaceURI();
                // hide the empty {}Ênamespace if none is present
                if (uri == null || uri.equals("")) {
                    name = entry.getName().getLocalName();
                } else {
                    // pattern: {uri}localName
                    name = entry.getName().toString();
                }
                break;
            }
        }
        
        // recursive call (building the path from right-to-left)
        return getBundlePath(parentBundle.getId(), parentBundle) + "/" + name;
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes
     * and inexistent parents.
     *
     * @param id
     *            node id for the bundle to check
     * @param bundle
     *            the bundle to check
     * @param fix
     *            if true, repair things that can be repaired
     * @param modifications
     *            if <code>fix == true</code>, collect the repaired
     *            {@linkplain NodePropBundle bundles} here
     */
    protected void checkBundleConsistency(NodeId id, NodePropBundle bundle, boolean fix, Collection modifications) {
        //log.info(name + ": checking bundle '" + id + "'");

        // look at the node's children
        Collection missingChildren = new ArrayList();
        Iterator iter = bundle.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            NodePropBundle.ChildNodeEntry entry = (NodePropBundle.ChildNodeEntry) iter.next();

            // skip check for system nodes (root, system root, version storage, nodetypes)
            if (entry.getId().toString().endsWith("babecafebabe")) {
                continue;
            }
            if (id.toString().endsWith("babecafebabe")) {
                continue;
            }

            try {
                // analyze child node bundles
                NodePropBundle child = loadBundle(entry.getId(), true);
                if (child == null) {
                    log.error("NodeState '" + getBundlePath(id, bundle) + "' ('" + id + "') references inexistent child '" + entry.getName() + "' with id '" + entry.getId() + "'");
                    missingChildren.add(entry);
                } else {
                    NodeId cp = child.getParentId();
                    if (cp == null) {
                        log.error("ChildNode '" + entry.getName() + "' has invalid parent uuid: <null>");
                    } else if (!cp.equals(id)) {
                        log.error("ChildNode '" + entry.getName() + "' has invalid parent uuid: '" + cp + "' (instead of '" + id + "')");
                    }
                }
            } catch (ItemStateException e) {
                // problem already logged (loadBundle called with logDetailedErrors=true)
            }
        }
        // remove child node entry (if fixing is enabled)
        if (fix && !missingChildren.isEmpty()) {
            Iterator iterator = missingChildren.iterator();
            while (iterator.hasNext()) {
                bundle.getChildNodeEntries().remove(iterator.next());
            }
            modifications.add(bundle);
        }

        // check parent reference
        NodeId parentId = bundle.getParentId();
        try {
            // skip root nodes (that point to itself)
            if (parentId != null && !id.toString().endsWith("babecafebabe")) {
                if (!existsBundle(parentId)) {
                    log.error("NodeState '" + id + "' references inexistent parent uuid '" + parentId + "'");
                }
            }
        } catch (ItemStateException e) {
            log.error("Error reading node '" + parentId + "' (parent of '" + id + "'): " + e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void checkConsistency(String[] uuids, boolean recursive, boolean fix) {
        log.info("{}: checking workspace consistency...", name);

        int count = 0;
        int total = 0;
        Collection modifications = new ArrayList();

        if (uuids == null) {
            // get all node bundles in the database with a single sql statement,
            // which is (probably) faster than loading each bundle and traversing the tree
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

                // iterate over all nodebundles in the db
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
                        // parse and check bundle
                        // check bundle will log any problems itself
                        if (binding.checkBundle(din)) {
                            // reset stream for readBundle()
                            din = new DataInputStream(blob.getBinaryStream());
                            NodePropBundle bundle = binding.readBundle(din, id);
                            checkBundleConsistency(id, bundle, fix, modifications);
                        } else {
                            log.error("invalid bundle '" + id + "', see previous BundleBinding error log entry");
                        }
                    } catch (Exception e) {
                        log.error("Error in bundle " + id + ": " + e);
                    }
                    count++;
                    if (count % 1000 == 0) {
                        log.info(name + ": checked " + count + " bundles...");
                    }
                }
            } catch (Exception e) {
                log.error("Error loading bundle", e);
            } finally {
                closeStream(din);
                closeResultSet(rs);
                total = count;
            }
        } else {
            // check only given uuids, handle recursive flag

            // 1) convert uuid array to modifiable list
            // 2) for each uuid do
            //     a) load node bundle
            //     b) check bundle, store any bundle-to-be-modified in collection
            //     c) if recursive, add child uuids to list of uuids

            List uuidList = new ArrayList(uuids.length);
            // convert uuid string array to list of UUID objects
            for (int i = 0; i < uuids.length; i++) {
                try {
                    uuidList.add(new UUID(uuids[i]));
                } catch (IllegalArgumentException e) {
                    log.error("Invalid uuid for consistency check, skipping: '" + uuids[i] + "': " + e);
                }
            }

            // iterate over UUIDs (including ones that are newly added inside the loop!)
            for (int i = 0; i < uuidList.size(); i++) {
                final UUID uuid = (UUID) uuidList.get(i);
                try {
                    // load the node from the database
                    NodeId id = new NodeId(uuid);
                    NodePropBundle bundle = loadBundle(id, true);

                    if (bundle == null) {
                        log.error("No bundle found for uuid '" + uuid + "'");
                        continue;
                    }

                    checkBundleConsistency(id, bundle, fix, modifications);

                    if (recursive) {
                        Iterator iter = bundle.getChildNodeEntries().iterator();
                        while (iter.hasNext()) {
                            NodePropBundle.ChildNodeEntry entry = (NodePropBundle.ChildNodeEntry) iter.next();
                            uuidList.add(entry.getId().getUUID());
                        }
                    }

                    count++;
                    if (count % 1000 == 0) {
                        log.info(name + ": checked " + count + "/" + uuidList.size() + " bundles...");
                    }
                } catch (ItemStateException e) {
                    // problem already logged (loadBundle called with logDetailedErrors=true)
                }
            }
            total = uuidList.size();
        }

        // repair collected broken bundles
        if (fix && !modifications.isEmpty()) {
            log.info(name + ": Fixing " + modifications.size() + " inconsistent bundle(s)...");
            Iterator iterator = modifications.iterator();
            while (iterator.hasNext()) {
                NodePropBundle bundle = (NodePropBundle) iterator.next();
                try {
                    log.info(name + ": Fixing bundle '" + bundle.getId() + "'");
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
    public synchronized NodeIdIterator getAllNodeIds(NodeId bigger, int maxCount)
            throws ItemStateException, RepositoryException {
        ResultSet rs = null;
        try {
            UUID lowUuid;
            Object[] keys;
            String sql;
            if (bigger == null) {
                sql = bundleSelectAllIdsSQL;
                lowUuid = null;
                keys = new Object[0];
            } else {
                sql = bundleSelectAllIdsFromSQL;
                lowUuid = bigger.getUUID();
                keys = getKey(lowUuid);
            }
            if (maxCount > 0) {
                // get some more rows, in case the first row is smaller
                // only required for SM_LONGLONG_KEYS
                // probability is very low to get get the wrong first key, < 1 : 2^64
                // see also bundleSelectAllIdsFrom SQL statement
                maxCount += 10;
            }
            Statement stmt = connectionManager.executeStmt(sql, keys, false, maxCount);
            rs = stmt.getResultSet();
            ArrayList result = new ArrayList();
            while ((maxCount == 0 || result.size() < maxCount) && rs.next()) {
                UUID current;
                if (getStorageModel() == SM_BINARY_KEYS) {
                    current = new UUID(rs.getBytes(1));
                } else {
                    long high = rs.getLong(1);
                    long low = rs.getLong(2);
                    current = new UUID(high, low);
                }
                if (lowUuid != null) {
                    // skip the keys that are smaller or equal (see above, maxCount += 10)
                    if (current.compareTo(lowUuid) <= 0) {
                        continue;
                    }
                }
                result.add(current);
            }        
            ListNodeIdIterator it = new ListNodeIdIterator(result);
            return it;
        } catch (SQLException e) {
            String msg = "getAllNodeIds failed.";
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeResultSet(rs);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected synchronized NodePropBundle loadBundle(NodeId id)
            throws ItemStateException {
        return loadBundle(id, false);
    }
    
    /**
     * Loads a bundle from the underlying system and optionally performs
     * a check on the bundle first.
     *
     * @param id the node id of the bundle
     * @param checkBeforeLoading check the bundle before loading it and log
     *                           detailed informations about it (slower)
     * @return the loaded bundle or <code>null</code> if the bundle does not
     *         exist.
     * @throws ItemStateException if an error while loading occurs.
     */
    protected synchronized NodePropBundle loadBundle(NodeId id, boolean checkBeforeLoading)
            throws ItemStateException {
        ResultSet rs = null;
        InputStream in = null;
        byte[] bytes = null;
        try {
            Statement stmt = connectionManager.executeStmt(bundleSelectSQL, getKey(id.getUUID()));
            rs = stmt.getResultSet();
            if (!rs.next()) {
                return null;
            }
            Blob b = rs.getBlob(1);
            // JCR-1039: pre-fetch/buffer blob data
            long length = b.length();
            bytes = new byte[(int) length];
            in = b.getBinaryStream();
            int read, pos = 0;
            while ((read = in.read(bytes, pos, bytes.length - pos)) > 0) {
                pos += read;
            }
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(bytes));
            
            if (checkBeforeLoading) {
                if (binding.checkBundle(din)) {
                    // reset stream for readBundle()
                    din = new DataInputStream(new ByteArrayInputStream(bytes));
                } else {
                    // gets wrapped as proper ItemStateException below
                    throw new Exception("invalid bundle, see previous BundleBinding error log entry");
                }
            }
            
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
     * {@inheritDoc}
     *
     * This method uses shared <code>PreparedStatements</code>, which must
     * be used strictly sequentially. Because this method synchronizes on the
     * persistence manager instance, there is no need to synchronize on the
     * shared statement. If the method would not be synchronized, the shared
     * statement must be synchronized.
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
     * @param ins the input stream
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
     * @param stmt the statement
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
            
            bundleSelectAllIdsSQL = "select NODE_ID from " + schemaObjectPrefix + "BUNDLE";
            bundleSelectAllIdsFromSQL = "select NODE_ID from " + schemaObjectPrefix + "BUNDLE WHERE NODE_ID > ? ORDER BY NODE_ID";
        } else {
            bundleInsertSQL = "insert into " + schemaObjectPrefix + "BUNDLE (BUNDLE_DATA, NODE_ID_HI, NODE_ID_LO) values (?, ?, ?)";
            bundleUpdateSQL = "update " + schemaObjectPrefix + "BUNDLE set BUNDLE_DATA = ? where NODE_ID_HI = ? and NODE_ID_LO = ?";
            bundleSelectSQL = "select BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?";
            bundleDeleteSQL = "delete from " + schemaObjectPrefix + "BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?";

            nodeReferenceInsertSQL = "insert into " + schemaObjectPrefix + "REFS (REFS_DATA, NODE_ID_HI, NODE_ID_LO) values (?, ?, ?)";
            nodeReferenceUpdateSQL = "update " + schemaObjectPrefix + "REFS set REFS_DATA = ? where NODE_ID_HI = ? and NODE_ID_LO = ?";
            nodeReferenceSelectSQL = "select REFS_DATA from " + schemaObjectPrefix + "REFS where NODE_ID_HI = ? and NODE_ID_LO = ?";
            nodeReferenceDeleteSQL = "delete from " + schemaObjectPrefix + "REFS where NODE_ID_HI = ? and NODE_ID_LO = ?";
            
            bundleSelectAllIdsSQL = "select NODE_ID_HI, NODE_ID_LO from " + schemaObjectPrefix + "BUNDLE";
            // need to use HI and LO parameters
            // this is not the exact statement, but not all databases support WHERE (NODE_ID_HI, NODE_ID_LOW) >= (?, ?)
            bundleSelectAllIdsFromSQL = "select NODE_ID_HI, NODE_ID_LO from " + schemaObjectPrefix + "BUNDLE WHERE (NODE_ID_HI >= ?) AND (? IS NOT NULL) ORDER BY NODE_ID_HI, NODE_ID_LO";
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

    private class ListNodeIdIterator implements NodeIdIterator {
        
        private final ArrayList list;
        private int pos;
        
        ListNodeIdIterator(ArrayList list) {
            this.list = list;
        }
        
        public NodeId nextNodeId() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return new NodeId((UUID) list.get(pos++));
        }        

        public boolean hasNext() {
            return pos < list.size();
        }

        public Object next() {
            return nextNodeId();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }

}
