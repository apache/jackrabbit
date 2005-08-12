/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.db;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.state.AbstractPersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.obj.BLOBStore;
import org.apache.jackrabbit.core.state.obj.ObjectPersistenceManager;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.util.Text;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <code>SimpleDbPersistenceManager</code> is a generic JDBC-based
 * <code>PersistenceManager</code> for Jackrabbit that persists
 * <code>ItemState</code> and <code>NodeReferences</code> objects using a
 * simple custom serialization format and a very basic non-normalized database
 * schema (in essence tables with one 'key' and one 'data' column).
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class</li>
 * <li><code>url</code>: the database url of the form <code>jdbc:subprotocol:subname</code></li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schema</code>: type of schema to be used
 * (e.g. <code>mysql</code>, <code>mssql</code>, etc.); </li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * </ul>
 * The required schema objects are automatically created by executing the DDL
 * statements read from the [schema].ddl file. The .ddl file is read from the
 * resources by calling <code>getClass().getResourceAsStream(schema + ".ddl")</code>.
 * Every line in the specified .ddl file is executed separatly by calling
 * <code>java.sql.Statement.execute(String)</code> where every occurence of the
 * the string <code>"${schemaObjectPrefix}"</code> has been replaced with the
 * value of the property <code>schemaObjectPrefix</code>.
 * <p/>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.state.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="com.mysql.jdbc.Driver"/&gt;
 *       &lt;param name="url" value="jdbc:mysql:///test"/&gt;
 *       &lt;param name="schema" value="mysql"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *  &lt;/PersistenceManager&gt;
 * </pre>
 */
public class SimpleDbPersistenceManager extends AbstractPersistenceManager
        implements BLOBStore {

    /** Logger instance */
    private static Logger log = Logger.getLogger(SimpleDbPersistenceManager.class);

    protected static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    protected boolean initialized;

    protected String driver;
    protected String url;
    protected String user;
    protected String password;
    protected String schema;
    protected String schemaObjectPrefix;

    // initial size of buffer used to serialize objects
    protected static final int INITIAL_BUFFER_SIZE = 1024;

    // jdbc connection
    protected Connection con;

    // shared prepared statements for NodeState management
    protected PreparedStatement nodeStateInsert;
    protected PreparedStatement nodeStateUpdate;
    protected PreparedStatement nodeStateSelect;
    protected PreparedStatement nodeStateDelete;

    // shared prepared statements for PropertyState management
    protected PreparedStatement propertyStateInsert;
    protected PreparedStatement propertyStateUpdate;
    protected PreparedStatement propertyStateSelect;
    protected PreparedStatement propertyStateDelete;

    // shared prepared statements for NodeReference management
    protected PreparedStatement nodeReferenceInsert;
    protected PreparedStatement nodeReferenceUpdate;
    protected PreparedStatement nodeReferenceSelect;
    protected PreparedStatement nodeReferenceDelete;

    /** file system where BLOB data is stored */
    protected FileSystem blobFS;

    /**
     * Creates a new <code>SimpleDbPersistenceManager</code> instance.
     */
    public SimpleDbPersistenceManager() {
        schema = "default";
        schemaObjectPrefix = "";
        initialized = false;
    }

    //----------------------------------------------------< setters & getters >
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

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

    //------------------------------------------------------------< BLOBStore >
    /**
     * {@inheritDoc}
     */
    public FileSystemResource get(String blobId) throws Exception {
        return new FileSystemResource(blobFS, blobId);
    }

    /**
     * {@inheritDoc}
     */
    public String put(PropertyId id, int index, InputStream in, long size)
            throws Exception {
        String path = buildBlobFilePath(id.getParentUUID(), id.getName(), index);
        OutputStream out = null;
        FileSystemResource internalBlobFile = new FileSystemResource(blobFS, path);
        internalBlobFile.makeParentDirs();
        try {
            out = new BufferedOutputStream(internalBlobFile.getOutputStream());
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(String blobId) throws Exception {
        FileSystemResource res = new FileSystemResource(blobFS, blobId);
        if (!res.exists()) {
            return false;
        }
        // delete resource and prune empty parent folders
        res.delete(true);
        return true;
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
        Class.forName(driver);
        con = DriverManager.getConnection(url, user, password);
        con.setAutoCommit(false);

        // check if schema objects exist and create them if necessary
        checkSchema();

        /**
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        this.blobFS = blobFS;

        // prepare statements
        nodeStateInsert =
                con.prepareStatement("insert into "
                + schemaObjectPrefix + "NODE (NODE_DATA, NODE_ID) values (?, ?)");
        nodeStateUpdate =
                con.prepareStatement("update "
                + schemaObjectPrefix + "NODE set NODE_DATA = ? where NODE_ID = ?");
        nodeStateSelect =
                con.prepareStatement("select NODE_DATA from "
                + schemaObjectPrefix + "NODE where NODE_ID = ?");
        nodeStateDelete =
                con.prepareStatement("delete from "
                + schemaObjectPrefix + "NODE where NODE_ID = ?");

        propertyStateInsert =
                con.prepareStatement("insert into "
                + schemaObjectPrefix + "PROP (PROP_DATA, PROP_ID) values (?, ?)");
        propertyStateUpdate =
                con.prepareStatement("update "
                + schemaObjectPrefix + "PROP set PROP_DATA = ? where PROP_ID = ?");
        propertyStateSelect =
                con.prepareStatement("select PROP_DATA from "
                + schemaObjectPrefix + "PROP where PROP_ID = ?");
        propertyStateDelete =
                con.prepareStatement("delete from "
                + schemaObjectPrefix + "PROP where PROP_ID = ?");

        nodeReferenceInsert =
                con.prepareStatement("insert into "
                + schemaObjectPrefix + "REFS (REFS_DATA, NODE_ID) values (?, ?)");
        nodeReferenceUpdate =
                con.prepareStatement("update "
                + schemaObjectPrefix + "REFS set REFS_DATA = ? where NODE_ID = ?");
        nodeReferenceSelect =
                con.prepareStatement("select REFS_DATA from "
                + schemaObjectPrefix + "REFS where NODE_ID = ?");
        nodeReferenceDelete =
                con.prepareStatement("delete from "
                + schemaObjectPrefix + "REFS where NODE_ID = ?");

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
            closeStatement(nodeStateInsert);
            closeStatement(nodeStateUpdate);
            closeStatement(nodeStateSelect);
            closeStatement(nodeStateDelete);

            closeStatement(propertyStateInsert);
            closeStatement(propertyStateUpdate);
            closeStatement(propertyStateSelect);
            closeStatement(propertyStateDelete);

            closeStatement(nodeReferenceInsert);
            closeStatement(nodeReferenceUpdate);
            closeStatement(nodeReferenceSelect);
            closeStatement(nodeReferenceDelete);

            // close jdbc connection
            con.close();
            // close blob store
            blobFS.close();
            blobFS = null;
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(ChangeLog changeLog)
            throws ItemStateException {
        ItemStateException ise = null;
        try {
            super.store(changeLog);
        } catch (ItemStateException e) {
            ise = e;
        } finally {
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
                    // re-throw original exception
                    throw ise;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        PreparedStatement stmt = nodeStateSelect;
        ResultSet rs = null;
        InputStream in = null;
        try {
            stmt.setString(1, id.toString());
            stmt.execute();
            rs = stmt.getResultSet();
            if (!rs.next()) {
                throw new NoSuchItemStateException(id.toString());
            }

            in = rs.getBinaryStream(1);
            NodeState state = createNew(id);
            ObjectPersistenceManager.deserialize(state, in);

            return state;
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to read node state: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeStream(in);
            closeResultSet(rs);
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        PreparedStatement stmt = propertyStateSelect;
        ResultSet rs = null;
        InputStream in = null;
        try {
            stmt.setString(1, id.toString());
            stmt.execute();
            rs = stmt.getResultSet();
            if (!rs.next()) {
                throw new NoSuchItemStateException(id.toString());
            }

            in = rs.getBinaryStream(1);
            PropertyState state = createNew(id);
            ObjectPersistenceManager.deserialize(state, in, this);

            return state;
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to read property state: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeStream(in);
            closeResultSet(rs);
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be sychronized the shared
     * statements would have to be synchronized.
     */
    public synchronized void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        //boolean update = exists((NodeId) state.getId());
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        PreparedStatement stmt = (update) ? nodeStateUpdate : nodeStateInsert;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize node state
            ObjectPersistenceManager.serialize(state, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the preparedStatement

            stmt.setBytes(1, out.toByteArray());
            stmt.setString(2, state.getId().toString());
            stmt.execute();

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write node state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be sychronized the shared
     * statements would have to be synchronized.
     */
    public synchronized void store(PropertyState state)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        //boolean update = exists((PropertyId) state.getId());
        boolean update = state.getStatus() != ItemState.STATUS_NEW;
        PreparedStatement stmt = (update) ? propertyStateUpdate : propertyStateInsert;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize property state
            ObjectPersistenceManager.serialize(state, out, this);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the preparedStatement

            stmt.setBytes(1, out.toByteArray());
            stmt.setString(2, state.getId().toString());
            stmt.execute();

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write property state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
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

        PreparedStatement stmt = nodeStateDelete;
        try {
            stmt.setString(1, state.getId().toString());
            stmt.execute();
        } catch (Exception e) {
            String msg = "failed to delete node state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
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

        // delete binary values (stored as files)
        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    if (val.getType() == PropertyType.BINARY) {
                        BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                        // delete blob file and prune empty parent folders
                        blobVal.delete(true);
                    }
                }
            }
        }

        PreparedStatement stmt = propertyStateDelete;
        try {
            stmt.setString(1, state.getId().toString());
            stmt.execute();
        } catch (Exception e) {
            String msg = "failed to delete property state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
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

        PreparedStatement stmt = nodeReferenceSelect;
        ResultSet rs = null;
        InputStream in = null;
        try {
            stmt.setString(1, targetId.toString());
            stmt.execute();
            rs = stmt.getResultSet();
            if (!rs.next()) {
                throw new NoSuchItemStateException(targetId.toString());
            }

            in = rs.getBinaryStream(1);
            NodeReferences refs = new NodeReferences(targetId);
            ObjectPersistenceManager.deserialize(refs, in);

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
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method uses shared <code>PreparedStatement</code>s which must
     * be executed strictly sequentially. Because this method synchronizes on
     * the persistence manager instance there is no need to synchronize on the
     * shared statement. If the method would not be sychronized the shared
     * statements would have to be synchronized.
     */
    public synchronized void store(NodeReferences refs)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // check if insert or update
        boolean update = exists(refs.getTargetId());
        PreparedStatement stmt = (update) ? nodeReferenceUpdate : nodeReferenceInsert;

        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            ObjectPersistenceManager.serialize(refs, out);

            // we are synchronized on this instance, therefore we do not
            // not have to additionally synchronize on the preparedStatement

            stmt.setBytes(1, out.toByteArray());
            stmt.setString(2, refs.getTargetId().toString());
            stmt.execute();

            // there's no need to close a ByteArrayOutputStream
            //out.close();
        } catch (Exception e) {
            String msg = "failed to write property state: " + refs.getTargetId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
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

        PreparedStatement stmt = nodeReferenceDelete;
        try {
            stmt.setString(1, refs.getTargetId().toString());
            stmt.execute();
        } catch (Exception e) {
            String msg = "failed to delete references: " + refs.getTargetId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(NodeId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        PreparedStatement stmt = nodeStateSelect;
        ResultSet rs = null;
        try {
            stmt.setString(1, id.toString());
            stmt.execute();
            rs = stmt.getResultSet();

            // a node state exists if the result has at least one entry
            return rs.next();
        } catch (Exception e) {
            String msg = "failed to check existence of node state: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeResultSet(rs);
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(PropertyId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        PreparedStatement stmt = propertyStateSelect;
        ResultSet rs = null;
        try {
            stmt.setString(1, id.toString());
            stmt.execute();
            rs = stmt.getResultSet();

            // a property state exists if the result has at least one entry
            return rs.next();
        } catch (Exception e) {
            String msg = "failed to check existence of property state: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            closeResultSet(rs);
            resetStatement(stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(NodeReferencesId targetId)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        PreparedStatement stmt = nodeReferenceSelect;
        ResultSet rs = null;
        try {
            stmt.setString(1, targetId.toString());
            stmt.execute();
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
            resetStatement(stmt);
        }
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Resets the given <code>PreparedStatement</code> by clearing the parameters
     * and warnings contained.
     * <p/>
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

    protected void closeStream(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignore) {
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

    protected static String buildBlobFilePath(String parentUUID,
                                              QName propName, int index) {
        StringBuffer sb = new StringBuffer();
        char[] chars = parentUUID.toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            //if (cnt > 0 && cnt % 4 == 0) {
            if (cnt == 2 || cnt == 4) {
                sb.append(FileSystem.SEPARATOR_CHAR);
            }
            sb.append(chars[i]);
            cnt++;
        }
        sb.append(FileSystem.SEPARATOR_CHAR);
        sb.append(FileSystemPathUtil.escapeName(propName.toString()));
        sb.append('.');
        sb.append(index);
        sb.append(".bin");
        return sb.toString();
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     * @throws Exception if an error occurs
     */
    protected void checkSchema() throws Exception {
        ResultSet rs = con.getMetaData().getTables(null, null,
                schemaObjectPrefix + "NODE", null);
        boolean schemaExists;
        try {
            schemaExists = rs.next();
        } finally {
            rs.close();
        }

        if (!schemaExists) {
            // read ddl from resources
            InputStream in = getClass().getResourceAsStream(schema + ".ddl");
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
                    // replace prefix variable
                    sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
                    // execute sql stmt
                    stmt.execute(sql);
                    // read next sql stmt
                    sql = reader.readLine();
                }
                // commit the changes
                con.commit();
            } finally {
                closeStream(in);
                closeStatement(stmt);
            }
        }
    }
}
